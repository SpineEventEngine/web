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

import com.google.protobuf.Empty;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UnknownFieldSet;
import io.spine.base.Time;
import io.spine.core.Ack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

import static io.spine.json.Json.toCompactJson;
import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.web.parser.HttpMessages.parse;
import static io.spine.web.parser.given.HttpMessagesTestEnv.JSON_TYPE;
import static io.spine.web.parser.given.HttpMessagesTestEnv.JSON_TYPE_CRAZY_CASE;
import static io.spine.web.parser.given.HttpMessagesTestEnv.JSON_TYPE_UTF_8;
import static io.spine.web.parser.given.HttpMessagesTestEnv.PROTOBUF_TYPE;
import static io.spine.web.parser.given.HttpMessagesTestEnv.base64;
import static io.spine.web.parser.given.HttpMessagesTestEnv.newAck;
import static io.spine.web.parser.given.HttpMessagesTestEnv.request;
import static io.spine.web.parser.given.HttpMessagesTestEnv.requestWithoutContentType;
import static io.spine.web.parser.given.HttpMessagesTestEnv.testJsonWithContentType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link io.spine.web.parser.HttpMessages HttpMessages}.
 */
@DisplayName("HttpMessages should")
class HttpMessagesTest {

    @Test
    @DisplayName("have private utility ctor")
    void testUtilCtor() {
        assertHasPrivateParameterlessCtor(HttpMessages.class);
    }

    @Test
    @DisplayName("parse message from JSON in HTTP request")
    void testParseEscaped() throws IOException {
        Ack expectedAck = newAck(5);
        String content = toCompactJson(expectedAck);
        Optional<Ack> actual = parse(requestWithoutContentType(content), Ack.class);
        assertTrue(actual.isPresent());
        assertEquals(expectedAck, actual.get());
    }

    @Test
    @DisplayName("parse message from JSON with application/json UTF8 Content-Type in HTTP request")
    void testParseJsonWithCharset() throws IOException {
        testJsonWithContentType(JSON_TYPE_UTF_8);
    }

    @Test
    @DisplayName("parse message from JSON with application/json Content-Type in HTTP request")
    void testParseJsonWithoutCharset() throws IOException {
        testJsonWithContentType(JSON_TYPE);
    }

    @Test
    @DisplayName("message parsing is accepting case-insensitive Content-Type")
    void testParseCaseInsensitive() throws IOException {
        testJsonWithContentType(JSON_TYPE_CRAZY_CASE);
    }

    @Test
    @DisplayName("parse message from Base64 string in HTTP request")
    void testBase64() throws IOException {
        Message expectedMessage = FieldMask.newBuilder()
                                           .addPaths("Dummy.field")
                                           .build();
        String content = base64(expectedMessage);
        Optional<FieldMask> actual =
                parse(request(content, PROTOBUF_TYPE), FieldMask.class);
        assertTrue(actual.isPresent());
        assertEquals(expectedMessage, actual.get());
    }

    @Test
    @DisplayName("not parse message of an unknown format")
    void testNotSupportUnknownFormat() throws IOException {
        String content = "whatever";
        Optional<?> result = parse(
                request(content, "invalid-format"),
                Message.class);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("parse message of JSON format specified explicitly")
    void testParseExplicitJson() throws IOException {
        String content = "{}";
        Optional<Empty> parsed = parse(
                request(content, JSON_TYPE),
                Empty.class);
        assertTrue(parsed.isPresent());
        assertEquals(Empty.getDefaultInstance(), parsed.get());
    }

    @Test
    @DisplayName("fail to parse a malformed byte string")
    void testFailToParseBytes() throws IOException {
        String content = Base64.getEncoder()
                               .encodeToString(new byte[]{(byte) 1, (byte) 42, (byte) 127});
        Optional<?> parsed = parse(request(content, PROTOBUF_TYPE), Empty.class);
        assertFalse(parsed.isPresent());
    }

    @Test
    @DisplayName("fail to parse wrong type of message from JSON")
    void testJsonWrongType() throws IOException {
        String content = "{ \"foo\": \"bar\" }";
        Optional<?> parsed = parse(requestWithoutContentType(content), Empty.class);
        assertFalse(parsed.isPresent());
    }

    @Test
    @DisplayName("parse to parse wrong type of message from bytes into unknown fields")
    void testBase64WrongType() throws IOException {
        Timestamp message = Time.currentTime();
        String content = base64(message);
        Optional<Empty> parsed = parse(request(content, PROTOBUF_TYPE), Empty.class);
        assertTrue(parsed.isPresent());
        Empty parsingResult = parsed.get();
        UnknownFieldSet unknownFields = parsingResult.getUnknownFields();
        assertEquals(message.getSeconds(),
                     (long) unknownFields.getField(1)
                                         .getVarintList()
                                         .get(0));
        assertEquals(message.getNanos(), (long) unknownFields.getField(2)
                                                             .getVarintList()
                                                             .get(0));
    }
}
