/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.web.firebase;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.google.api.client.http.ByteArrayContent.fromString;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({"ResultOfMethodCallIgnored", "CheckReturnValue"}) // Calling methods to throw.
@DisplayName("HttpRequestExecutor should")
class HttpRequestExecutorTest {

    private static final GenericUrl URL = new GenericUrl("https://localhost:8080");
    private static final String RESPONSE = "{\"error\":\"not found\"}";
    private static final ByteArrayContent CONTENT = fromString(JSON_UTF_8.toString(), "content");

    @Test
    @DisplayName("execute GET request")
    void executeGetRequest() {
        MockHttpTransport transport = mockHttpTransport(RESPONSE);
        HttpRequestExecutor requestExecutor = HttpRequestExecutor.using(transport);
        String content = requestExecutor.get(URL);
        assertEquals(RESPONSE, content);
    }

    @Test
    @DisplayName("throw RequestToFirebaseFailedException if an error occurs on GET request")
    void throwIfErrorOnGet() {
        MockHttpTransport transport = throwingHttpTransport();
        HttpRequestExecutor requestExecutor = HttpRequestExecutor.using(transport);
        assertThrows(RequestToFirebaseFailedException.class, () -> requestExecutor.get(URL));
    }

    @Test
    @DisplayName("execute PUT request")
    void executePutRequest() {
        MockHttpTransport transport = mockHttpTransport(RESPONSE);
        HttpRequestExecutor requestExecutor = HttpRequestExecutor.using(transport);
        String content = requestExecutor.put(URL, CONTENT);
        assertEquals(RESPONSE, content);
    }

    @Test
    @DisplayName("throw RequestToFirebaseFailedException if an error occurs on PUT request")
    void throwIfErrorOnPut() {
        MockHttpTransport transport = throwingHttpTransport();
        HttpRequestExecutor requestExecutor = HttpRequestExecutor.using(transport);
        assertThrows(RequestToFirebaseFailedException.class,
                     () -> requestExecutor.put(URL, CONTENT));
    }

    @Test
    @DisplayName("execute PATCH request")
    void executePatchRequest() {
        MockHttpTransport transport = mockHttpTransport(RESPONSE);
        HttpRequestExecutor requestExecutor = HttpRequestExecutor.using(transport);
        String content = requestExecutor.patch(URL, CONTENT);
        assertEquals(RESPONSE, content);
    }

    @Test
    @DisplayName("throw RequestToFirebaseFailedException if an error occurs on PATCH request")
    void throwIfErrorOnPatch() {
        MockHttpTransport transport = throwingHttpTransport();
        HttpRequestExecutor requestExecutor = HttpRequestExecutor.using(transport);
        assertThrows(RequestToFirebaseFailedException.class,
                     () -> requestExecutor.patch(URL, CONTENT));
    }

    /**
     * Returns an {@code HttpTransport} mock which returns the specified content on every request.
     */
    private static MockHttpTransport mockHttpTransport(String content) {
        return new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest
            buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.setContent(content);
                        return response;
                    }
                };
            }
        };
    }

    /**
     * Returns an {@code HttpTransport} mock which throws {@link java.io.IOException} on every
     * request.
     */
    private static MockHttpTransport throwingHttpTransport() {
        return new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest
            buildRequest(String method, String url) throws IOException {
                throw new IOException("Intended exception");
            }
        };
    }
}
