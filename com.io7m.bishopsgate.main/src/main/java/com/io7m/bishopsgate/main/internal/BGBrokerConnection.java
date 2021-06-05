/*
 * Copyright © 2018 Mark Raynsford <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.bishopsgate.main.internal;

import com.io7m.bishopsgate.main.BGQueueConfiguration;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.junreachable.UnreachableCodeException;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.TextMessage;
import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static org.apache.activemq.artemis.api.jms.JMSFactoryType.CF;

/**
 * A connection to a message broker.
 */

public final class BGBrokerConnection implements Closeable
{
  private static final Logger LOG = LoggerFactory.getLogger(BGBrokerConnection.class);

  private final BGQueueConfiguration configuration;
  private final CloseableCollectionType<? extends Exception> resources;
  private final MessageConsumer messageConsumer;
  private final AtomicBoolean closed;

  private BGBrokerConnection(
    final BGQueueConfiguration inConfiguration,
    final CloseableCollectionType<? extends Exception> inResources,
    final MessageConsumer inMessageConsumer)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.resources =
      Objects.requireNonNull(inResources, "resources");
    this.messageConsumer =
      Objects.requireNonNull(inMessageConsumer, "messageConsumer");
    this.closed =
      new AtomicBoolean(false);
  }

  /**
   * Create a new connection.
   *
   * @param configuration The configuration
   *
   * @return A new connection
   *
   * @throws Exception On errors
   */

  public static BGBrokerConnection create(
    final BGQueueConfiguration configuration)
    throws Exception
  {
    final var resources =
      CloseableCollection.create();

    final var transport =
      new TransportConfiguration(NettyConnectorFactory.class.getName());
    final var connections =
      resources.add(
        ActiveMQJMSClient.createConnectionFactoryWithoutHA(CF, transport)
      );

    final var address =
      new StringBuilder(64)
        .append("tcp://")
        .append(configuration.brokerAddress())
        .append(":")
        .append(configuration.brokerPort())
        .append("?sslEnabled=true")
        .toString();

    connections.setBrokerURL(address);
    connections.setClientID(UUID.randomUUID().toString());
    connections.setUser(configuration.brokerUser());
    connections.setPassword(configuration.brokerPassword());

    switch (configuration.queueKind()) {
      case TOPIC: {
        final var topicConnection =
          resources.add(
            connections.createTopicConnection()
          );

        final var session =
          resources.add(
            topicConnection.createTopicSession(false, AUTO_ACKNOWLEDGE)
          );

        final var topic =
          session.createTopic(configuration.queueAddress());
        final var subscriber =
          resources.add(session.createSubscriber(topic));

        topicConnection.start();
        return new BGBrokerConnection(configuration, resources, subscriber);
      }
    }

    throw new UnreachableCodeException();
  }

  /**
   * @return {@code true} if the connection is still open
   */

  public boolean isOpen()
  {
    return !this.closed.get();
  }

  /**
   * Recieve a message.
   *
   * @param receiver The message receiver
   *
   * @throws IOException On I/O errors
   */

  public void receive(
    final Consumer<BGMessage> receiver)
    throws IOException
  {
    try {
      Objects.requireNonNull(receiver, "receiver");

      final var message =
        this.messageConsumer.receive(500L);

      if (message instanceof TextMessage) {
        final var textMessage =
          (TextMessage) message;
        final var text =
          textMessage.getText();
        final var time =
          Instant.ofEpochMilli(message.getJMSDeliveryTime());

        receiver.accept(
          BGMessage.builder()
            .setQueue(this.configuration.queueAddress())
            .setTimestamp(time)
            .setMessage(text)
            .build()
        );

        message.acknowledge();
      }
    } catch (final JMSException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void close()
    throws IOException
  {
    if (this.closed.compareAndSet(false, true)) {
      try {
        this.resources.close();
      } catch (final Exception e) {
        throw new IOException(e);
      }
    }
  }
}
