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


import com.io7m.dixmont.core.DmJsonRestrictedDeserializers;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.math.BigInteger;
import java.net.URI;

/**
 * JSON object mappers.
 */

public final class BGMatrixObjectMappers
{
  private BGMatrixObjectMappers()
  {

  }

  /**
   * Create a JSON object mappers.
   *
   * @return A new object mapper
   */

  public static JsonMapper createObjectMapper()
  {
    final var builder =
      JsonMapper.builder()
        .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    final var dixB = DmJsonRestrictedDeserializers.builder();
    dixB.allowClass(BGMatrixJSON.BGError.class);
    dixB.allowClass(BGMatrixJSON.BGLoginRequest.class);
    dixB.allowClass(BGMatrixJSON.BGLoginResponse.class);
    dixB.allowClass(BGMatrixJSON.BGRoomResolveAliasResponse.class);
    dixB.allowClass(BGMatrixJSON.BGRoomMessage.class);
    dixB.allowClass(String.class);
    dixB.allowClass(BigInteger.class);
    dixB.allowClass(URI.class);
    dixB.allowListsOfClass(String.class);
    dixB.allowListsOfClass(BigInteger.class);

    final var deserializers = dixB.build();
    final var simpleModule = new SimpleModule();
    simpleModule.setDeserializers(deserializers);
    builder.addModule(simpleModule);
    return builder.build();
  }
}
