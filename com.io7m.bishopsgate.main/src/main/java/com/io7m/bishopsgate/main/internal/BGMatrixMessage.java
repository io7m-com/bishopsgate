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

import java.util.Objects;
import java.util.Optional;

/**
 * The type of Matrix messages.
 */

public final class BGMatrixMessage
{
  private final Optional<String> icon;
  private final String text;
  private final BGMatrixMessageKind kind;

  private BGMatrixMessage(
    final Optional<String> inIcon,
    final String inText,
    final BGMatrixMessageKind inKind)
  {
    this.icon = Objects.requireNonNull(inIcon, "icon");
    this.text = Objects.requireNonNull(inText, "text");
    this.kind = Objects.requireNonNull(inKind, "kind");
  }

  /**
   * Create a message.
   *
   * @param icon The icon
   * @param text The text
   * @param kind The message kind
   *
   * @return A message
   */

  public static BGMatrixMessage create(
    final String icon,
    final String text,
    final BGMatrixMessageKind kind)
  {
    return new BGMatrixMessage(Optional.ofNullable(icon), text, kind);
  }

  /**
   * @return The message kind
   */

  public BGMatrixMessageKind kind()
  {
    return this.kind;
  }

  /**
   * @return The message icon
   */

  public Optional<String> icon()
  {
    return this.icon;
  }

  /**
   * @return The message text
   */

  public String text()
  {
    return this.text;
  }
}
