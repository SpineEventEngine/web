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

package io.spine.web.firebase;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.common.testing.NullPointerTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.web.firebase.FirebaseRestClient.NULL_ENTRY;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("FirebaseRestClient should")
class FirebaseRestClientTest {

    private static final String NODE_ACCESS_FORMAT = "https://database.com/%s.json";

    private static final String PATH = "node/path";
    private static final String DATA = "{\"a\":\"b\"}";

    private HttpRequestExecutor requestExecutor;
    private FirebaseRestClient client;
    private FirebaseDatabasePath path;
    private FirebaseNodeValue value;

    @BeforeEach
    void setUp() {
        requestExecutor = mock(HttpRequestExecutor.class);
        client = new FirebaseRestClient(NODE_ACCESS_FORMAT, requestExecutor);
        path = FirebaseDatabasePath.fromString(PATH);
        value = FirebaseNodeValue.from(DATA);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(FirebaseDatabasePath.class, path)
                .setDefault(FirebaseNodeValue.class, value)
                .testAllPublicInstanceMethods(client);
    }

    @Test
    @DisplayName("retrieve data from given database path")
    void getData() {
        when(requestExecutor.get(any())).thenReturn(DATA);

        Optional<FirebaseNodeValue> result = client.get(path);
        assertTrue(result.isPresent());
        FirebaseNodeValue value = result.get();
        String contentString = value.underlyingJson()
                                    .toString();
        assertEquals(DATA, contentString);
    }

    @Test
    @DisplayName("return empty Optional in case of null data")
    void getNullData() {
        when(requestExecutor.get(any())).thenReturn(NULL_ENTRY);

        Optional<FirebaseNodeValue> result = client.get(path);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("store data via PUT method when node is not present")
    void storeNewViaPut() {
        when(requestExecutor.get(any())).thenReturn(NULL_ENTRY);

        client.merge(path, value);
        verify(requestExecutor).put(eq(expectedUrl()), any(ByteArrayContent.class));
    }

    @Test
    @DisplayName("store data via PATCH method when node already exists")
    void updateExistingViaPatch() {
        when(requestExecutor.get(any())).thenReturn(DATA);

        client.merge(path, value);
        verify(requestExecutor).patch(eq(expectedUrl()), any(ByteArrayContent.class));
    }

    private static GenericUrl expectedUrl() {
        String fullPath = format(NODE_ACCESS_FORMAT, PATH);
        GenericUrl result = new GenericUrl(fullPath);
        return result;
    }
}
