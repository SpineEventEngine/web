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

package io.spine.web.firebase;

import com.google.api.client.googleapis.testing.auth.oauth2.MockGoogleCredential;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.testing.NullPointerTester;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`FirebaseCredentials` should")
class FirebaseCredentialsTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester().testAllPublicStaticMethods(FirebaseCredentials.class);
    }

    @Nested
    @DisplayName("be created")
    class BeCreated {

        @Test
        @DisplayName("without credentials")
        void empty() {
            FirebaseCredentials credentials = FirebaseCredentials.empty();
            assertThat(credentials.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("from `GoogleCredentials` instance")
        void fromGoogleCredentials() {
            GoogleCredentials googleCredentials = GoogleCredentials.newBuilder()
                                                                   .build();
            FirebaseCredentials credentials =
                    FirebaseCredentials.fromGoogleCredentials(googleCredentials);
            assertThat(credentials.isEmpty()).isFalse();
            assertThat(credentials.isOldStyle()).isFalse();
        }

        @SuppressWarnings("deprecation") // This test checks the deprecated method.
        @Test
        @DisplayName("from `GoogleCredential` instance")
        void fromOldStyleCredentials() {
            MockGoogleCredential googleCredential = new MockGoogleCredential.Builder().build();
            FirebaseCredentials credentials =
                    FirebaseCredentials.fromGoogleCredentials(googleCredential);
            assertThat(credentials.isEmpty()).isFalse();
            assertThat(credentials.isOldStyle()).isTrue();
        }
    }

    @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
    @Test
    @DisplayName("throw `IAE` when created from invalid data")
    void throwOnInvalidData() {
        String invalidCredentials = "invalid_credentials";
        InputStream stream = toInputStream(invalidCredentials);
        assertThrows(IllegalArgumentException.class, () -> FirebaseCredentials.fromStream(stream));
    }

    private static InputStream toInputStream(String theString) {
        byte[] bytes = theString.getBytes(UTF_8);
        InputStream result = new ByteArrayInputStream(bytes);
        return result;
    }
}
