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

package io.spine.web.parser.given;

import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import io.spine.core.Ack;
import io.spine.core.AckVBuilder;
import io.spine.web.parser.HttpMessages;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Base64;
import java.util.Optional;

import static io.spine.core.Responses.statusOk;
import static io.spine.json.Json.toCompactJson;
import static io.spine.protobuf.AnyPacker.pack;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test environment for {@link io.spine.web.parser.HttpMessagesTest HttpMessages Tests}.
 */
public final class HttpMessagesTestEnv {

    @SuppressWarnings("DuplicateStringLiteralInspection")
    private static final String CONTENT_TYPE = "Content-Type";
    public static final String JSON_TYPE = "application/json";
    public static final String JSON_TYPE_UTF_8 = "application/json; charset=utf-8";
    public static final String JSON_TYPE_CRAZY_CASE = "aPPliCatIon/JSon";
    public static final String PROTOBUF_TYPE = "application/x-protobuf";

    /** Prevents the test environment class instantiation. */
    private HttpMessagesTestEnv() {
    }

    public static void testJsonWithContentType(String type) throws IOException {
        Ack expectedAck = newAck(5);
        String content = toCompactJson(expectedAck);
        Optional<Ack> actual = HttpMessages.parse(request(content, type), Ack.class);
        assertTrue(actual.isPresent());
        assertEquals(expectedAck, actual.get());
    }

    public static Ack newAck(int id) {
        return Ack
                .vBuilder()
                .setMessageId(pack(Int32Value.of(id)))
                .setStatus(statusOk())
                .build();
    }

    public static String base64(Message message) {
        byte[] messageBytes = message.toByteArray();
        String result = Base64.getEncoder()
                              .encodeToString(messageBytes);
        return result;
    }

    public static HttpServletRequest requestWithoutContentType(String content) throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(content)));
        return request;
    }

    public static HttpServletRequest request(String content, String format)
            throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(content)));
        when(request.getHeader(eq(CONTENT_TYPE))).thenReturn(format);
        return request;
    }
}
