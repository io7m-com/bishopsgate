/*
 * Copyright © 2021 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

import com.io7m.bishopsgate.main.BGConfiguration;
import com.io7m.bishopsgate.main.BGQueueConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.io7m.bishopsgate.main.internal.BGMatrixJSON.BGError;
import static com.io7m.bishopsgate.main.internal.BGMatrixJSON.BGLoginResponse;
import static com.io7m.bishopsgate.main.internal.BGMatrixJSON.BGRoomResolveAliasResponse;
import static com.io7m.bishopsgate.main.internal.BGMatrixMessageKind.NOTICE;
import static com.io7m.bishopsgate.main.internal.BGMatrixMessageKind.PLAIN;

/**
 * The basic JMS → Matrix relay client.
 */

public final class BGClient implements AutoCloseable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(BGClient.class);

  private static final String ICON_CHECKMARK = "\u2705";
  private static final String ICON_ERROR = "\u274c";
  private static final String ICON_MESSAGE = "\u2709";

  private final BGConfiguration configuration;
  private final ExecutorService executor;
  private final BGMatrixClient client;
  private final AtomicBoolean done = new AtomicBoolean(false);
  private final ConcurrentLinkedQueue<BGMatrixMessage> outbox;

  private BGClient(
    final BGConfiguration inConfiguration,
    final ExecutorService inExecutor,
    final BGMatrixClient inClient)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.executor =
      Objects.requireNonNull(inExecutor, "executor");
    this.client =
      Objects.requireNonNull(inClient, "client");
    this.outbox =
      new ConcurrentLinkedQueue<>();
  }

  /**
   * Create a new client.
   *
   * @param configuration The client configuration
   * @param client        The HTTP client
   *
   * @return A new client
   */

  public static BGClient create(
    final BGConfiguration configuration,
    final HttpClient client)
  {
    Objects.requireNonNull(configuration, "configuration");
    Objects.requireNonNull(client, "client");

    final var matrixClient =
      BGMatrixClient.create(client, configuration.matrixServer());
    final var threadCount =
      1 + configuration.queues().size();
    final var executor =
      Executors.newFixedThreadPool(
        threadCount,
        runnable -> {
          final Thread thread = new Thread(runnable);
          thread.setName(String.format(
            "com.io7m.bishopsgate.%d",
            Long.valueOf(thread.getId()))
          );
          return thread;
        }
      );

    return new BGClient(configuration, executor, matrixClient);
  }

  private static void pauseFor(
    final long seconds)
  {
    try {
      Thread.sleep(seconds * 1_000L);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void close()
    throws Exception
  {
    if (this.done.compareAndSet(false, true)) {
      this.executor.shutdown();
    }
  }

  /**
   * Start the client executing.
   */

  public void start()
  {
    for (final var queueConfig : this.configuration.queues()) {
      this.executor.execute(
        new QueueTask(queueConfig, this.done, this::enqueueMessageSend)
      );
    }

    this.executor.execute(() -> {
      while (!this.done.get()) {
        try {
          final var token =
            this.fetchAccessToken();
          final var room =
            this.fetchRoom(token);

          this.joinRoom(token, room);

          while (!this.done.get()) {
            while (!this.outbox.isEmpty()) {
              this.sendMessage(token, room, this.outbox.poll());
            }
            pauseFor(1L);
          }
        } catch (final Exception e) {
          LOG.error("error: ", e);
          pauseFor(5L);
        }
      }
    });
  }

  private void enqueueMessageSend(
    final String icon,
    final String text,
    final BGMatrixMessageKind kind)
  {
    this.outbox.add(BGMatrixMessage.create(icon, text, kind));
  }

  private void joinRoom(
    final String token,
    final String room)
    throws IOException, InterruptedException
  {
    this.client.roomJoin(token, room);
  }

  private String fetchRoom(
    final String token)
    throws IOException, InterruptedException
  {
    final var roomId =
      this.client.roomResolveAlias(token, this.configuration.matrixChannel());

    if (roomId instanceof BGError) {
      final var error = (BGError) roomId;
      throw new IOException(String.format(
        "Matrix server said: %s %s",
        error.errorCode,
        error.errorMessage)
      );
    }

    return ((BGRoomResolveAliasResponse) roomId).roomId;
  }

  private void sendMessage(
    final String token,
    final String room,
    final BGMatrixMessage message)
    throws IOException, InterruptedException
  {
    this.client.roomSendMessage(token, room, message);
  }

  private String fetchAccessToken()
    throws IOException, InterruptedException
  {
    final var response =
      this.client.login(
        this.configuration.matrixUserName(),
        this.configuration.matrixPassword()
      );

    if (response instanceof BGError) {
      final var error = (BGError) response;
      throw new IOException(String.format(
        "Matrix server said: %s %s",
        error.errorCode,
        error.errorMessage)
      );
    }

    final var token =
      ((BGLoginResponse) response).accessToken;

    LOG.debug("retrieved access token {}", token);
    LOG.info("logged in to {}", this.configuration.matrixServer());
    return token;
  }

  private interface MessageSenderType
  {
    void send(
      String icon,
      String text,
      BGMatrixMessageKind notice);
  }

  private static final class QueueTask implements Runnable
  {
    private final BGQueueConfiguration queueConfig;
    private final AtomicBoolean done;
    private final MessageSenderType sender;
    private BGBrokerConnection connection;

    QueueTask(
      final BGQueueConfiguration inQueueConfig,
      final AtomicBoolean inDone,
      final MessageSenderType inSender)
    {
      this.queueConfig =
        Objects.requireNonNull(inQueueConfig, "queueConfig");
      this.done =
        Objects.requireNonNull(inDone, "done");
      this.sender =
        Objects.requireNonNull(inSender, "message");
    }

    @Override
    public void run()
    {
      final String queueAddress = this.queueConfig.queueAddress();
      LOG.debug("starting task for queue: {}", queueAddress);

      boolean sentError = false;

      while (!this.done.get()) {
        try {
          if (this.connection == null || !this.connection.isOpen()) {
            LOG.debug(
              "(re)opening connection to broker for queue: {}",
              queueAddress);
            this.connection = BGBrokerConnection.create(this.queueConfig);
            this.sender.send(
              ICON_CHECKMARK,
              "Established connection to message broker",
              NOTICE);
            sentError = false;
          }

          this.connection.receive(this::onMessageReceived);
        } catch (final Exception e) {
          if (!sentError) {
            this.sender.send(
              ICON_ERROR,
              new StringBuilder(64)
                .append("Lost connection to message broker: ")
                .append(e.getClass().getSimpleName())
                .append(": ")
                .append(e.getMessage())
                .toString(),
              NOTICE);
            sentError = true;
          }

          LOG.error("i/o error: ", e);
          try {
            if (this.connection != null) {
              this.connection.close();
            }
          } catch (final IOException ex) {
            LOG.error("error closing connection: ", ex);
          } finally {
            this.connection = null;
          }

          LOG.debug("pausing for one second before retrying");
          pauseFor(1L);
        }
      }
    }

    private void onMessageReceived(
      final BGMessage message)
    {
      LOG.debug("received: {}: {}", message.queue(), message.message());
      this.sender.send("", message.message(), PLAIN);
    }
  }
}
