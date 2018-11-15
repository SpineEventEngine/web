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

import com.google.api.client.googleapis.testing.auth.oauth2.MockGoogleCredential;
import com.google.common.testing.NullPointerTester;
import io.spine.testing.UtilityClassTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.web.firebase.DatabaseUrl.from;
import static io.spine.web.firebase.FirebaseCredentials.fromGoogleCredentials;

@DisplayName("FirebaseClientFactory should")
class FirebaseClientFactoryTest extends UtilityClassTest<FirebaseClientFactory> {

    private static final DatabaseUrl SOME_URL = from("https://someUrl.com");

    private static final MockGoogleCredential GOOGLE_CREDENTIALS =
            new MockGoogleCredential.Builder().build();

    private static final FirebaseCredentials CREDENTIALS =
            fromGoogleCredentials(GOOGLE_CREDENTIALS);

    FirebaseClientFactoryTest() {
        super(FirebaseClientFactory.class);
    }

    @Override
    protected void configure(NullPointerTester tester) {
        tester.setDefault(DatabaseUrl.class, SOME_URL)
              .setDefault(FirebaseCredentials.class, CREDENTIALS);
    }

    @Test
    @DisplayName("create REST client for AppEngine environment")
    void createGaeRestClient() {
        FirebaseClient client = FirebaseClientFactory.gae(SOME_URL, CREDENTIALS);
        assertThat(client).isInstanceOf(FirebaseRestClient.class);
    }

    @Test
    @DisplayName("create REST client for non-GAE environment")
    void createNonGaeRestClient() {
        FirebaseClient client = FirebaseClientFactory.other(SOME_URL, CREDENTIALS);
        assertThat(client).isInstanceOf(FirebaseRestClient.class);
    }
}
