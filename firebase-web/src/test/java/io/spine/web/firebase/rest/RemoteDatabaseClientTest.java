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

package io.spine.web.firebase.rest;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpMethods;
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
import io.spine.web.firebase.FirebaseClient;
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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.web.firebase.rest.HttpClientMockFactory.mockHttpClient;
import static io.spine.web.firebase.rest.HttpClientMockFactory.noOpClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    private FirebaseDatabase database;
    private NodePath path;
    private NodeValue value;

    @BeforeAll
    @SuppressWarnings({"JdkObsolete", "JavaUtilDate"})  /* Comply with Google API. */
    static void initFirebase() {
        var fakeCredentials =
                GoogleCredentials.create(new AccessToken("obviously fake", new Date()));
        FirebaseApp.initializeApp(
                FirebaseOptions.builder()
                        .setDatabaseUrl(DATABASE_URL_STRING)
                        .setCredentials(fakeCredentials)
                        .build(),
                FIREBASE_APP_NAME);
    }

    @BeforeEach
    void setUp() {
        database = FirebaseDatabase.getInstance(
                FirebaseApp.getInstance(FIREBASE_APP_NAME)
        );
        path = NodePaths.of(PATH);
        value = DATA.asNodeValue();
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        FirebaseClient client = new RemoteDatabaseClient(database, NODE_FACTORY, noOpClient());
        new NullPointerTester()
                .setDefault(NodePath.class, path)
                .setDefault(NodeValue.class, value)
                .testAllPublicInstanceMethods(client);
    }

    @Test
    @DisplayName("retrieve data from given database path")
    void getData() {
        var httpClient = mockHttpClient(DATA.value());
        FirebaseClient client = new RemoteDatabaseClient(database, NODE_FACTORY, httpClient);

        var result = client.fetchNode(path);
        assertThat(result)
                .isPresent();
        var value = result.get();
        var contentString = value.underlyingJson()
                                 .toString();
        assertEquals(DATA.value(), contentString);
    }

    @Test
    @DisplayName("return empty Optional in case of null data")
    void getNullData() {
        var httpClient = mockHttpClient(NULL_ENTRY);
        FirebaseClient client = new RemoteDatabaseClient(database, NODE_FACTORY, httpClient);

        var result = client.fetchNode(path);
        assertThat(result)
                .isEmpty();
    }

    @Test
    @DisplayName("store data via PUT method when node is not present")
    void storeNewViaPut() {
        var httpClient = mockHttpClient(NULL_ENTRY, (method, url) -> {
            assertThat(method)
                    .isEqualTo(HttpMethods.PUT);
            assertThat(url)
                    .isEqualTo(EXPECTED_NODE_URL.build());
        });
        FirebaseClient client = new RemoteDatabaseClient(database, NODE_FACTORY, httpClient);

        client.create(path, value);
    }

    @Test
    @DisplayName("store data via PATCH method when node already exists")
    void updateExistingViaPatch() {
        var httpClient = mockHttpClient(NULL_ENTRY, (method, url) -> {
            assertThat(method)
                    .isEqualTo(HttpMethods.PATCH);
            assertThat(url)
                    .isEqualTo(EXPECTED_NODE_URL.build());
        });
        FirebaseClient client = new RemoteDatabaseClient(database, NODE_FACTORY, httpClient);

        client.update(path, value);
    }

    @Nested
    @DisplayName("have a builder which should")
    class Builder {

        private final HttpRequestFactory requestFactory =
                new MockHttpTransport().createRequestFactory();
        private FirebaseDatabase database;

        @BeforeEach
        void configureFirebase() {
            var app = FirebaseApp.getInstance(FIREBASE_APP_NAME);
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
            var builder = RemoteDatabaseClient
                    .newBuilder()
                    .setDatabase(database);
            assertThrows(IllegalStateException.class, builder::build);
        }

        @Test
        @DisplayName("require a Firebase database to build")
        void requireDatabase() {
            var builder = RemoteDatabaseClient
                    .newBuilder()
                    .setRequestFactory(requestFactory);
            assertThrows(IllegalStateException.class, builder::build);
        }

        @Test
        @DisplayName("build with all required parameters")
        void build() {
            var client = RemoteDatabaseClient
                    .newBuilder()
                    .setDatabase(database)
                    .setRequestFactory(requestFactory)
                    .build();
            assertNotNull(client);
        }
    }
}
