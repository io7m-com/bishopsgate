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

package com.io7m.bishopsgate.tests;

import com.io7m.bishopsgate.main.BGConfiguration;
import com.io7m.bishopsgate.main.BGConfigurations;
import com.io7m.bishopsgate.main.internal.BGMatrixClient;
import com.io7m.bishopsgate.main.internal.BGMatrixMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import static com.io7m.bishopsgate.main.internal.BGMatrixJSON.BGLoginResponse;
import static com.io7m.bishopsgate.main.internal.BGMatrixJSON.BGRoomResolveAliasResponse;
import static com.io7m.bishopsgate.main.internal.BGMatrixMessageKind.PLAIN;

public final class BGClientDemo
{
  private static final Logger LOG =
    LoggerFactory.getLogger(BGClientDemo.class);

  private BGClientDemo()
  {

  }

  public static void main(
    final String[] args)
    throws Exception
  {
    if (args.length != 1) {
      LOG.error("usage: bishopsgate.conf");
      System.exit(1);
    }

    final var path = Paths.get(args[0]);
    final BGConfiguration configuration;
    try (var stream = Files.newInputStream(path)) {
      final var properties = new Properties();
      properties.load(stream);
      configuration = BGConfigurations.ofProperties(properties);
    }

    final var client =
      BGMatrixClient.create(
        HttpClient.newHttpClient(),
        configuration.matrixServer()
      );

    final var token =
      (BGLoginResponse) client.login(
        configuration.matrixUserName(),
        configuration.matrixPassword()
      );

    final var roomId =
      (BGRoomResolveAliasResponse)
        client.roomResolveAlias(
          token.accessToken,
          configuration.matrixChannel()
        );

    LOG.info("room ID: {}", roomId);
    client.roomJoin(token.accessToken, roomId.roomId);
    client.roomSendMessage(token.accessToken, roomId.roomId, BGMatrixMessage.create("\u1000", "Hello", PLAIN));
  }
}
