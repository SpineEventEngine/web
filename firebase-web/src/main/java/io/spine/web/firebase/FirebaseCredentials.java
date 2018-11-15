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

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.util.Arrays.asList;

/**
 * Class is read-only outside of the package.
 */
public class FirebaseCredentials {

    private static final String AUTH_DATABASE = "https://www.googleapis.com/auth/firebase.database";
    private static final String AUTH_USER_EMAIL = "https://www.googleapis.com/auth/userinfo.email";
    private static final Collection<String> AUTH_SCOPES = asList(AUTH_DATABASE, AUTH_USER_EMAIL);

    private final GoogleCredential credentials;

    private FirebaseCredentials(GoogleCredential credentials) {
        this.credentials = credentials;
    }

    @VisibleForTesting
    static FirebaseCredentials fromGoogleCredentials(GoogleCredential credentials) {
        checkNotNull(credentials);
        return new FirebaseCredentials(credentials);
    }

    public static FirebaseCredentials fromStream(InputStream resource) {
        checkNotNull(resource);
        GoogleCredential credentials = parseCredentials(resource);
        return new FirebaseCredentials(credentials);
    }

    GoogleCredential credentials() {
        return credentials;
    }

    private static GoogleCredential parseCredentials(InputStream resource) {
        try {
            GoogleCredential credentials = GoogleCredential.fromStream(resource)
                                                           .createScoped(AUTH_SCOPES);
            return credentials;
        } catch (IOException e) {
            throw newIllegalStateException(
                    e, "Exception when acquiring Firebase credentials: %s", e.getMessage());
        }
    }
}
