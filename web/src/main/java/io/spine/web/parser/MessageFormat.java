/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.web.parser;

import com.google.common.flogger.FluentLogger;
import com.google.common.net.MediaType;
import com.google.protobuf.Message;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static java.util.Optional.empty;

/**
 * Message formats supported by the Spine web API.
 */
@SuppressWarnings({"NonSerializableFieldInSerializableClass", "UnstableApiUsage"})
public enum MessageFormat {

    /**
     * The JSON message stringification format.
     */
    JSON(JSON_UTF_8) {
        @Override
        <M extends Message> MessageParser<M> parserFor(Class<M> type) {
            return new JsonMessageParser<>(type);
        }
    },

    /**
     * The Base64 bytes message stringification format.
     */
    BASE64(MediaType.create("application", "x-protobuf")) {
        @Override
        <M extends Message> MessageParser<M> parserFor(Class<M> type) {
            return new Base64MessageParser<>(type);
        }
    };

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private static final String CONTENT_TYPE = "Content-Type";

    private final MediaType contentType;

    MessageFormat(MediaType contentType) {
        this.contentType = contentType;
    }

    /**
     * Finds the required format for the given {@linkplain HttpServletRequest request}.
     *
     * <p>The format is determined by the value of the {@code Content-Type} header.
     * If the value is equal to {@code application/json} (case insensitive), returns {@link #JSON}.
     * If the value is equal to {@code application/x-protobuf} (case insensitive), returns
     * {@link #BASE64}. If the header is not set, returns {@link #JSON}.
     *
     * @param request
     *         the request to get the format for
     * @return the format of the message in the given request or {@code Optional.empty()} if
     *         the request does not justify the described format
     */
    public static Optional<MessageFormat> formatOf(HttpServletRequest request) {
        String contentTypeHeader = request.getHeader(CONTENT_TYPE);
        if (isNullOrEmpty(contentTypeHeader)) {
            return Optional.of(JSON);
        }
        try {
            MediaType type = MediaType.parse(contentTypeHeader);
            Optional<MessageFormat> format = formatOf(type);
            if (!format.isPresent()) {
                logger.atWarning()
                      .log("Cannot determine message format for request `%s %s`.%n" +
                                   "Content-Type: `%s`.",
                           request.getMethod(),
                           request.getServletPath(),
                           contentTypeHeader);
            }
            return format;
        } catch (IllegalArgumentException e) {
            logger.atSevere()
                  .withCause(e)
                  .log();
            return empty();
        }
    }

    /**
     * Parses a given string representation into a message of the given {@code type}.
     *
     * @param rawMessage
     *         string representation of the message
     * @param type
     *         expected class of the message
     * @param <T>
     *         expected class of the message
     * @return the parsed message or {@code Optional.empty()} if parsing fails
     */
    public <T extends Message> Optional<T> parse(String rawMessage, Class<T> type) {
        return parserFor(type).parse(rawMessage);
    }

    private static Optional<MessageFormat> formatOf(MediaType type) {
        return Stream.of(values())
                     .filter(format -> format.matches(type))
                     .findFirst();
    }

    private boolean matches(MediaType otherContentType) {
        return contentType.withoutParameters()
                          .is(otherContentType.withoutParameters());
    }

    /**
     * Creates a {@link MessageParser} for the given {@code type}.
     *
     * <p>The parser works with {@code this} message format.
     *
     * @param type
     *         the class of the message to parse
     * @param <M>
     *         the type of the message to parse
     * @return a message parses instance
     */
    abstract <M extends Message> MessageParser<M> parserFor(Class<M> type);
}
