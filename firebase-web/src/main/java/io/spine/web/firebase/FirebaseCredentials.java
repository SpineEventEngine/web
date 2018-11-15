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
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static java.util.Arrays.asList;

/**
 * A Firebase Database credentials.
 *
 * <p>The underlying Google OAuth 2.0 service can be used as an authentication facility for the
 * requests to Firebase REST API.
 *
 * <p>See <a href="https://firebase.google.com/docs/database/rest/auth">Firebase REST docs</a>.
 */
public final class FirebaseCredentials {

    private static final String AUTH_DATABASE =
            "https://www.googleapis.com/auth/firebase.database";
    private static final String AUTH_USER_EMAIL = "https://www.googleapis.com/auth/userinfo.email";

    /**
     * The authentication scopes which are required to access the Firebase Database.
     */
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

    /**
     * Creates a new instance of the {@code FirebaseCredentials} from the given credential stream.
     *
     * <p>Typically the stream will contain a service account JSON file data.
     *
     * <p>See <a href="https://firebase.google.com/docs/admin/setup#add_firebase_to_your_app">docs
     * </a> for how to obtain the file.
     *
     * @param credentialStream
     *         the stream containing credentials data
     * @return a new instance of {@code FirebaseCredentials}
     * @throws java.lang.IllegalArgumentException
     *         in case there are problems with parsing the given stream into
     *         {@link GoogleCredential}
     */
    public static FirebaseCredentials fromStream(InputStream credentialStream) {
        checkNotNull(credentialStream);
        GoogleCredential credentials = parseCredentials(credentialStream);
        return new FirebaseCredentials(credentials);
    }

    GoogleCredential credentials() {
        return credentials;
    }

    private static GoogleCredential parseCredentials(InputStream credentialStream) {
        try {
            GoogleCredential credentials = GoogleCredential.fromStream(credentialStream)
                                                           .createScoped(AUTH_SCOPES);
            return credentials;
        } catch (IOException e) {
            throw newIllegalArgumentException(
                    e, "Exception when acquiring Firebase credentials: %s", e.getMessage());
        }
    }
}
