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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.StringJoiner;

// CHECKSTYLE:OFF

public final class BGMatrixJSON
{
  private BGMatrixJSON()
  {

  }

  /**
   * The base type of JSON objects.
   */

  public interface BGMatrixJSONObjectType
  {

  }

  /**
   * The base type of JSON responses.
   */

  public interface BGMatrixJSONResponseType
    extends BGMatrixJSONObjectType
  {

  }

  @JsonDeserialize
  @JsonSerialize
  public static final class BGError
    implements BGMatrixJSONResponseType
  {
    @JsonProperty(required = true, value = "errcode")
    public String errorCode;
    @JsonProperty(required = true, value = "error")
    public String errorMessage;

    public BGError()
    {

    }

    @Override
    public String toString()
    {
      return new StringJoiner(
        ", ",
        BGError.class.getSimpleName() + "[",
        "]")
        .add("errorCode='" + this.errorCode + "'")
        .add("errorMessage='" + this.errorMessage + "'")
        .toString();
    }
  }

  @JsonSerialize
  @JsonDeserialize
  public static final class BGLoginRequest
    implements BGMatrixJSONObjectType
  {
    @JsonProperty(required = true, value = "type")
    public final String type = "m.login.password";
    @JsonProperty(required = true, value = "user")
    public String userName;
    @JsonProperty(required = true, value = "password")
    public String password;

    public BGLoginRequest()
    {

    }

    @Override
    public String toString()
    {
      final var sb = new StringBuilder("BGLoginRequest{");
      sb.append("userName='").append(this.userName).append('\'');
      sb.append(", password='").append(this.password).append('\'');
      sb.append(", type='").append(this.type).append('\'');
      sb.append('}');
      return sb.toString();
    }
  }

  @JsonSerialize
  @JsonDeserialize
  public static final class BGLoginResponse
    implements BGMatrixJSONResponseType
  {
    @JsonProperty(required = true, value = "user_id")
    public String userId;
    @JsonProperty(required = true, value = "access_token")
    public String accessToken;

    public BGLoginResponse()
    {

    }

    @Override
    public String toString()
    {
      final var sb = new StringBuilder("BGLoginResponse{");
      sb.append("userId='").append(this.userId).append('\'');
      sb.append(", accessToken='").append(this.accessToken).append('\'');
      sb.append('}');
      return sb.toString();
    }
  }

  @JsonSerialize
  @JsonDeserialize
  public static final class BGRoomResolveAliasResponse
    implements BGMatrixJSONResponseType
  {
    @JsonProperty(required = true, value = "room_id")
    public String roomId;

    public BGRoomResolveAliasResponse()
    {

    }

    @Override
    public String toString()
    {
      final var sb = new StringBuilder("BGRoomResolveAliasResponse{");
      sb.append("roomId='").append(this.roomId).append('\'');
      sb.append('}');
      return sb.toString();
    }
  }

  @JsonSerialize
  @JsonDeserialize
  public static final class BGRoomMessage
    implements BGMatrixJSONObjectType
  {
    @JsonProperty(required = true, value = "format")
    public final String format = "org.matrix.custom.html";
    @JsonProperty(required = true, value = "msgtype")
    public String msgtype;
    @JsonProperty(required = true, value = "body")
    public String body;
    @JsonProperty(required = true, value = "formatted_body")
    public String formattedBody;

    public BGRoomMessage()
    {

    }

    @Override
    public String toString()
    {
      final var sb = new StringBuilder("BGRoomMessage{");
      sb.append("msgtype='").append(this.msgtype).append('\'');
      sb.append(", format='").append(this.format).append('\'');
      sb.append(", body='").append(this.body).append('\'');
      sb.append(", formattedBody='").append(this.formattedBody).append('\'');
      sb.append('}');
      return sb.toString();
    }
  }
}
