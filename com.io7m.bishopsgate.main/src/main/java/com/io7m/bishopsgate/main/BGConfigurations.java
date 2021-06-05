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

package com.io7m.bishopsgate.main;

import com.io7m.jdeferthrow.core.ExceptionTracker;
import com.io7m.jproperties.JProperties;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Functions to parse configurations.
 */

public final class BGConfigurations
{
  private static final Pattern WHITESPACE = Pattern.compile("\\s+");

  private BGConfigurations()
  {

  }

  /**
   * Parse a configuration from the given properties.
   *
   * @param properties The input properties
   *
   * @return A parsed message
   *
   * @throws IOException On errors
   */

  public static BGConfiguration ofProperties(
    final Properties properties)
    throws IOException
  {
    Objects.requireNonNull(properties, "properties");

    final var tracker = new ExceptionTracker<Exception>();

    try {
      final var builder =
        BGConfiguration.builder();

      tracker.catching(() -> {
        JProperties.getString(properties, "bishopsgate.queues");
      });

      final var queuesValue =
        properties.getProperty("bishopsgate.queues")
          .trim();

      tracker.catching(() -> {
        builder.setMatrixUserName(
          JProperties.getString(properties, "bishopsgate.matrix.user")
        );
      });
      tracker.catching(() -> {
        builder.setMatrixPassword(
          JProperties.getString(properties, "bishopsgate.matrix.password")
        );
      });
      tracker.catching(() -> {
        builder.setMatrixServer(
          JProperties.getURI(properties, "bishopsgate.matrix.url")
        );
      });
      tracker.catching(() -> {
        builder.setMatrixChannel(
          JProperties.getString(properties, "bishopsgate.matrix.channel")
        );
      });

      final var queueNames =
        List.of(WHITESPACE.split(queuesValue))
          .stream()
          .filter(name -> !name.isBlank())
          .collect(Collectors.toList());

      for (final var queue : queueNames) {
        parseQueue(tracker, properties, queue).ifPresent(builder::addQueues);
      }

      tracker.throwIfNecessary();
      return builder.build();
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  private static Optional<BGQueueConfiguration> parseQueue(
    final ExceptionTracker<Exception> tracker,
    final Properties properties,
    final String directory)
  {
    final var builder =
      BGQueueConfiguration.builder();

    tracker.catching(() -> {
      builder.setQueueAddress(
        JProperties.getString(
          properties,
          String.format("bishopsgate.queues.%s.queue_address", directory))
      );
    });
    tracker.catching(() -> {
      builder.setQueueKind(
        BGQueueKind.valueOf(
          JProperties.getString(
            properties,
            String.format("bishopsgate.queues.%s.queue_kind", directory)))
      );
    });

    tracker.catching(() -> {
      builder.setBrokerUser(
        JProperties.getString(
          properties,
          String.format("bishopsgate.queues.%s.broker_user", directory))
      );
    });
    tracker.catching(() -> {
      builder.setBrokerPassword(
        JProperties.getString(
          properties,
          String.format("bishopsgate.queues.%s.broker_password", directory))
      );
    });
    tracker.catching(() -> {
      builder.setBrokerAddress(
        JProperties.getString(
          properties,
          String.format("bishopsgate.queues.%s.broker_address", directory))
      );
    });
    tracker.catching(() -> {
      builder.setBrokerPort(
        JProperties.getInteger(
          properties,
          String.format("bishopsgate.queues.%s.broker_port", directory))
      );
    });

    try {
      tracker.throwIfNecessary();
    } catch (final Exception e) {
      return Optional.empty();
    }

    return Optional.of(builder.build());
  }
}
