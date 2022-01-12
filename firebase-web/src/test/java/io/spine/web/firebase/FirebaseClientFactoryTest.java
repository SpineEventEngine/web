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

package io.spine.web.firebase;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.testing.NullPointerTester;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;
import io.spine.server.DeploymentType;
import io.spine.server.ServerEnvironment;
import io.spine.testing.UtilityClassTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static com.google.common.truth.Truth.assertThat;

@DisplayName("`FirebaseClientFactory` should")
class FirebaseClientFactoryTest extends UtilityClassTest<FirebaseClientFactory> {

    private static final FirebaseCredentials CREDENTIALS = FirebaseCredentials.empty();
    private static final String FIREBASE_APP_NAME = FirebaseClientFactoryTest.class.getSimpleName();
    private final FirebaseApp app = FirebaseApp.getInstance(FIREBASE_APP_NAME);
    private FirebaseDatabase database;

    FirebaseClientFactoryTest() {
        super(FirebaseClientFactory.class);
    }

    @BeforeAll
    @SuppressWarnings({"JdkObsolete", "JavaUtilDate" })  /* Comply with Google API. */
    static void initApp() {
        var fakeCredentials =
                GoogleCredentials.create(new AccessToken("apparently fake", new Date()));
        FirebaseApp.initializeApp(FirebaseOptions
                                          .builder()
                                          .setCredentials(fakeCredentials)
                                          .setDatabaseUrl("https://apparently.fake")
                                          .build(),
                                  FIREBASE_APP_NAME);
    }

    @Override
    protected void configure(NullPointerTester tester) {
        tester.setDefault(FirebaseDatabase.class, FirebaseDatabase.getInstance(app))
              .setDefault(FirebaseCredentials.class, CREDENTIALS);
    }

    @BeforeEach
    void configureDatabase() {
        database = FirebaseDatabase.getInstance(app);
    }

    @AfterEach
    void resetEnvironment() {
        ServerEnvironment
                .instance()
                .reset();
    }

    @Test
    @DisplayName("create a client with AppEngine-specific HTTP transport")
    void createForGae() {
        ServerEnvironment
                .instance()
                .configureDeployment(() -> DeploymentType.APPENGINE_CLOUD);
        var client = FirebaseClientFactory.remoteClient(database);
        assertThat(client)
                .isNotNull();
    }

    @Test
    @DisplayName("create a client with non-AppEngine HTTP transport")
    void createNotForGae() {
        ServerEnvironment
                .instance()
                .configureDeployment(() -> DeploymentType.STANDALONE);
        var client = FirebaseClientFactory.remoteClient(database);
        assertThat(client)
                .isNotNull();
    }
}
