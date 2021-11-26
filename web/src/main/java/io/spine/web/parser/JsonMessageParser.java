/*
 * Copyright 2021, TeamDev. All rights reserved.
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

import com.google.protobuf.Message;
import io.spine.json.Json;
import io.spine.logging.Logging;

import java.util.Optional;
import java.util.regex.Pattern;

import static io.spine.json.Json.fromJson;
import static java.util.regex.Pattern.LITERAL;
import static java.util.regex.Pattern.compile;

/**
 * An implementation of {@link MessageParser} which parses messages from their JSON representations.
 *
 * <p>See {@link Json} and the
 * <a href="https://developers.google.com/protocol-buffers/docs/proto3#json">Protobuf documentation
 * </a> for the detailed description of the message format.
 *
 * @param <M>
 *         the type of messages to parse
 */
final class JsonMessageParser<M extends Message> implements MessageParser<M>, Logging {

    private final Class<M> type;

    JsonMessageParser(Class<M> type) {
        this.type = type;
    }

    @Override
    public Optional<M> parse(String raw) {
        var json = cleanUp(raw);
        try {
            var message = fromJson(json, type);
            return Optional.of(message);
        } catch (IllegalArgumentException e) {
            _error().withCause(e)
                    .log("Unable to parse message of type `%s` from JSON: `%s`.",
                         type.getName(), json);
            return Optional.empty();
        }
    }

    private static String cleanUp(String jsonFromRequest) {
        var json = EscapeSymbol.unEscapeAll(jsonFromRequest);
        var unQuoted = unQuote(json);
        return unQuoted;
    }

    private static String unQuote(String json) {
        var beginIndex = 0;
        var endIndex = json.length();
        if (json.startsWith("\"")) {
            beginIndex = 1;
        }
        if (json.endsWith("\"")) {
            endIndex = json.length() - 1;
        }
        var result = json.substring(beginIndex, endIndex);
        return result;
    }

    /**
     * An enumeration of special characters that may be escaped when sending a string over HTTP.
     */
    private enum EscapeSymbol {

        @SuppressWarnings({
                "HardcodedLineSeparator", // Work only with literal "\n"s
                "unused"                  // Used via `values()`.
        })
        LINE_FEED("\\n", "\n"),
        @SuppressWarnings("unused") // Used via `values()`.
        QUOTATION_MARK("\\\"", "\"");

        private final Pattern escapedPattern;
        private final String raw;

        EscapeSymbol(String escaped, String raw) {
            this.escapedPattern = compile(escaped, LITERAL);
            this.raw = raw;
        }

        private String unEscape(String escaped) {
            var matcher = escapedPattern.matcher(escaped);
            var unescaped = matcher.replaceAll(raw);
            return unescaped;
        }

        private static String unEscapeAll(String escaped) {
            var json = escaped;
            for (var symbol : EscapeSymbol.values()) {
                json = symbol.unEscape(json);
            }
            return json;
        }
    }
}
