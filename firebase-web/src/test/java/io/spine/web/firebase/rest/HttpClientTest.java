/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.web.firebase.rest;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.api.client.http.ByteArrayContent.fromString;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static io.spine.web.firebase.rest.HttpClientMockFactory.mockHttpClient;
import static io.spine.web.firebase.rest.HttpClientMockFactory.throwingClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`HttpClient` should")
@SuppressWarnings({"ResultOfMethodCallIgnored", "CheckReturnValue"}) // Calling methods to throw.
class HttpClientTest {

    private static final GenericUrl URL = new GenericUrl("https://localhost:8080");
    private static final String RESPONSE = "{\"error\":\"not found\"}";
    private static final ByteArrayContent CONTENT = fromString(JSON_UTF_8.toString(), "content");

    @Test
    @DisplayName("execute GET request")
    void executeGetRequest() {
        HttpClient requestExecutor = mockHttpClient(RESPONSE);
        String content = requestExecutor.get(URL);
        assertEquals(RESPONSE, content);
    }

    @Test
    @DisplayName("throw RequestToFirebaseFailedException if an error occurs on GET request")
    void throwIfErrorOnGet() {
        HttpClient requestExecutor = throwingClient();
        assertThrows(RequestToFirebaseFailedException.class, () -> requestExecutor.get(URL));
    }

    @Test
    @DisplayName("execute PUT request")
    void executePutRequest() {
        HttpClient requestExecutor = mockHttpClient(RESPONSE);
        String content = requestExecutor.put(URL, CONTENT);
        assertEquals(RESPONSE, content);
    }

    @Test
    @DisplayName("throw RequestToFirebaseFailedException if an error occurs on PUT request")
    void throwIfErrorOnPut() {
        HttpClient requestExecutor = throwingClient();
        assertThrows(RequestToFirebaseFailedException.class,
                     () -> requestExecutor.put(URL, CONTENT));
    }

    @Test
    @DisplayName("execute PATCH request")
    void executePatchRequest() {
        HttpClient requestExecutor = mockHttpClient(RESPONSE);
        String content = requestExecutor.patch(URL, CONTENT);
        assertEquals(RESPONSE, content);
    }

    @Test
    @DisplayName("throw RequestToFirebaseFailedException if an error occurs on PATCH request")
    void throwIfErrorOnPatch() {
        HttpClient requestExecutor = throwingClient();
        assertThrows(RequestToFirebaseFailedException.class,
                     () -> requestExecutor.patch(URL, CONTENT));
    }
}
