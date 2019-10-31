/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import io.spine.json.Json;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static com.google.common.net.MediaType.create;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.joining;

/**
 * Message formats supported by the {@link HttpMessages}.
 */
@SuppressWarnings("NonSerializableFieldInSerializableClass")
public enum MessageFormat {

    /**
     * The JSON message stringification format.
     */
    JSON(JSON_UTF_8) {
        @Override
        public String print(Message message) {
            return Json.toCompactJson(message);
        }

        @Override
        <M extends Message> MessageParser<M> parserFor(Class<M> type) {
            return new JsonMessageParser<>(type);
        }
    },

    /**
     * The Base64 bytes message stringification format.
     */
    BASE64(create("application", "x-protobuf")) {
        @Override
        public String print(Message message) {
            byte[] bytes = message.toByteArray();
            String raw = Base64.getEncoder()
                               .encodeToString(bytes);
            return raw;
        }

        @Override
        <M extends Message> MessageParser<M> parserFor(Class<M> type) {
            return new Base64MessageParser<>(type);
        }
    };

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    @SuppressWarnings("DuplicateStringLiteralInspection") // A duplicate is in tests.
    private static final String CONTENT_TYPE = "Content-Type";

    private final MediaType contentType;

    MessageFormat(MediaType contentType) {
        this.contentType = contentType;
    }

    public MediaType contentType() {
        return contentType;
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

    public abstract String print(Message message);

    public <T extends Message> Optional<T> parse(String rawMessage, Class<T> cls) {
        return parserFor(cls).parse(rawMessage);
    }

    private static String body(ServletRequest request) throws IOException {
        String result = request.getReader()
                               .lines()
                               .collect(joining(" "));
        return result;
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
