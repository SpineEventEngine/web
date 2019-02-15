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

package io.spine.web.firebase.rest;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.common.testing.NullPointerTester;
import io.spine.web.firebase.DatabaseUrls;
import io.spine.web.firebase.NodePaths;
import io.spine.web.firebase.NodeValue;
import io.spine.web.firebase.client.DatabaseUrl;
import io.spine.web.firebase.client.NodePath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.web.firebase.rest.RestClient.NULL_ENTRY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("RestClient should")
class RestClientTest {

    private static final String PATH = "node/path";
    private static final String DATA = "{\"a\":\"b\"}";
    private static final String DATABASE_URL_STRING = "https://database.com";
    private static final DatabaseUrl DATABASE_URL = DatabaseUrls.from(DATABASE_URL_STRING);
    private static final RestNodeUrls NODE_FACTORY = new RestNodeUrls(DATABASE_URL);

    private static final GenericUrl EXPECTED_NODE_URL =
            new GenericUrl(DATABASE_URL_STRING + '/' + PATH + ".json");

    private HttpClient httpClient;
    private RestClient client;
    private NodePath path;
    private NodeValue value;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        client = new RestClient(NODE_FACTORY, httpClient);
        path = NodePaths.of(PATH);
        value = NodeValue.from(DATA);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(NodePath.class, path)
                .setDefault(NodeValue.class, value)
                .testAllPublicInstanceMethods(client);
    }

    @Test
    @DisplayName("retrieve data from given database path")
    void getData() {
        when(httpClient.get(any())).thenReturn(DATA);

        Optional<NodeValue> result = client.get(path);
        assertTrue(result.isPresent());
        NodeValue value = result.get();
        String contentString = value.underlyingJson()
                                    .toString();
        assertEquals(DATA, contentString);
    }

    @Test
    @DisplayName("return empty Optional in case of null data")
    void getNullData() {
        when(httpClient.get(any())).thenReturn(NULL_ENTRY);

        Optional<NodeValue> result = client.get(path);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("store data via PUT method when node is not present")
    void storeNewViaPut() {
        when(httpClient.get(any())).thenReturn(NULL_ENTRY);

        client.merge(path, value);
        verify(httpClient).put(eq(EXPECTED_NODE_URL), any(ByteArrayContent.class));
    }

    @Test
    @DisplayName("store data via PATCH method when node already exists")
    void updateExistingViaPatch() {
        when(httpClient.get(any())).thenReturn(DATA);

        client.merge(path, value);
        verify(httpClient).patch(eq(EXPECTED_NODE_URL), any(ByteArrayContent.class));
    }
}
