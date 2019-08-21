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
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.testing.NullPointerTester;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;
import io.spine.web.firebase.DatabaseUrl;
import io.spine.web.firebase.DatabaseUrls;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.NodePaths;
import io.spine.web.firebase.NodeValue;
import io.spine.web.firebase.StoredJson;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Optional;

import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("`RemoteDatabaseClient` should")
class RemoteDatabaseClientTest {

    private static final String PATH = "node/path";
    private static final StoredJson DATA = StoredJson.from("{\"a\":\"b\"}");
    private static final String DATABASE_URL_STRING = "https://database.com";
    private static final String FIREBASE_APP_NAME = RemoteDatabaseClient.class.getSimpleName();
    private static final DatabaseUrl DATABASE_URL = DatabaseUrls.from(DATABASE_URL_STRING);
    private static final RestNodeUrls NODE_FACTORY = new RestNodeUrls(DATABASE_URL);
    private static final String NULL_ENTRY = StoredJson.nullValue()
                                                       .toString();
    private static final GenericUrl EXPECTED_NODE_URL =
            new GenericUrl(DATABASE_URL_STRING + '/' + PATH + ".json");

    private HttpClient httpClient;
    private RemoteDatabaseClient client;
    private NodePath path;
    private NodeValue value;

    @BeforeAll
    static void initFirebase() {
        GoogleCredentials fakeCredentials =
                GoogleCredentials.create(new AccessToken("obviously fake", new Date()));
        FirebaseApp.initializeApp(FirebaseOptions
                                          .builder()
                                          .setDatabaseUrl(DATABASE_URL_STRING)
                                          .setCredentials(fakeCredentials)
                                          .build(),
                                  FIREBASE_APP_NAME);
    }

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        FirebaseDatabase mockFirebase = mock(FirebaseDatabase.class);
        client = new RemoteDatabaseClient(mockFirebase, NODE_FACTORY, httpClient);
        path = NodePaths.of(PATH);
        value = DATA.asNodeValue();
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
        when(httpClient.get(any())).thenReturn(DATA.value());

        Optional<NodeValue> result = client.fetchNode(path);
        assertTrue(result.isPresent());
        NodeValue value = result.get();
        String contentString = value.underlyingJson()
                                    .toString();
        assertEquals(DATA.value(), contentString);
    }

    @Test
    @DisplayName("return empty Optional in case of null data")
    void getNullData() {
        when(httpClient.get(any())).thenReturn(NULL_ENTRY);

        Optional<NodeValue> result = client.fetchNode(path);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("store data via PUT method when node is not present")
    void storeNewViaPut() {
        when(httpClient.get(any())).thenReturn(NULL_ENTRY);

        client.create(path, value);
        verify(httpClient).put(eq(EXPECTED_NODE_URL), any(ByteArrayContent.class));
    }

    @Test
    @DisplayName("store data via PATCH method when node already exists")
    void updateExistingViaPatch() {
        when(httpClient.get(any())).thenReturn(DATA.value());

        client.update(path, value);
        verify(httpClient).patch(eq(EXPECTED_NODE_URL), any(ByteArrayContent.class));
    }

    @Nested
    @DisplayName("have a builder which should")
    class Builder {

        private final HttpRequestFactory requestFactory =
                new MockHttpTransport().createRequestFactory();
        private FirebaseDatabase database;

        @BeforeEach
        void configureFirebase() {
            FirebaseApp app = FirebaseApp.getInstance(FIREBASE_APP_NAME);
            database = FirebaseDatabase.getInstance(app);
        }

        @Test
        @DisplayName(NOT_ACCEPT_NULLS)
        void nulls() {
            new NullPointerTester()
                    .testAllPublicInstanceMethods(RemoteDatabaseClient.newBuilder());
        }

        @Test
        @DisplayName("require a request factory to build")
        void requireFactory() {
            RemoteDatabaseClient.Builder builder = RemoteDatabaseClient
                    .newBuilder()
                    .setDatabase(database);
            assertThrows(IllegalStateException.class, builder::build);
        }

        @Test
        @DisplayName("require a Firebase database to build")
        void requireDatabase() {
            RemoteDatabaseClient.Builder builder = RemoteDatabaseClient
                    .newBuilder()
                    .setRequestFactory(requestFactory);
            assertThrows(IllegalStateException.class, builder::build);
        }

        @Test
        @DisplayName("build with all required parameters")
        void build() {
            RemoteDatabaseClient client = RemoteDatabaseClient
                    .newBuilder()
                    .setDatabase(database)
                    .setRequestFactory(requestFactory)
                    .build();
            assertNotNull(client);
        }
    }
}
