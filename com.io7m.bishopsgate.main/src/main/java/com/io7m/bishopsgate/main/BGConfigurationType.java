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

import com.io7m.immutables.styles.ImmutablesStyleType;
import org.immutables.value.Value;

import java.net.URI;
import java.util.List;

/**
 * Configuration data.
 */

@ImmutablesStyleType
@Value.Immutable
public interface BGConfigurationType
{
  /**
   * @return The Matrix server base URI
   */

  @Value.Parameter
  URI matrixServer();

  /**
   * @return The Matrix server user name
   */

  @Value.Parameter
  String matrixUserName();

  /**
   * @return The Matrix server password
   */

  @Value.Parameter
  String matrixPassword();

  /**
   * @return The Matrix channel to which to relay messages
   */

  @Value.Parameter
  String matrixChannel();

  /**
   * @return The message broker queues
   */

  @Value.Parameter
  List<BGQueueConfiguration> queues();

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    if (!this.matrixServer().toString().endsWith("/")) {
      throw new IllegalArgumentException("Matrix server URL must end with /");
    }
  }
}
