/*
 * Copyright 2023, TeamDev. All rights reserved.
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

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.annotations.VisibleForTesting;
import io.spine.annotation.Internal;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static java.util.Arrays.asList;

/**
 * A Firebase Database credentials.
 *
 * <p>This type can be used as an authentication facility for the requests to Firebase REST API.
 *
 * <p>See <a href="https://firebase.google.com/docs/database/rest/auth">Firebase REST docs</a>.
 */
@SuppressWarnings("deprecation" /*`GoogleCredential` is used to retain backward compatibility.*/)
public final class FirebaseCredentials implements HttpRequestInitializer {

    private static final String AUTH_DATABASE = "https://www.googleapis.com/auth/firebase.database";
    private static final String AUTH_USER_EMAIL = "https://www.googleapis.com/auth/userinfo.email";

    /**
     * The authentication scopes which are required to access the Firebase Database.
     */
    private static final Collection<String> AUTH_SCOPES = asList(AUTH_DATABASE, AUTH_USER_EMAIL);

    /**
     * The credential used for authentication.
     *
     * <p>Either this field or {@link #oldStyleCredential} will always be {@code null}.
     */
    private final @Nullable GoogleCredentials credentials;

    /**
     * The credential used for authentication.
     *
     * <p>Either this field or {@link #credentials} will always be {@code null}.
     */
    private final com.google.api.client.googleapis.auth.oauth2.@Nullable GoogleCredential
            oldStyleCredential;

    private FirebaseCredentials() {
        this.credentials = null;
        this.oldStyleCredential = null;
    }

    private FirebaseCredentials(GoogleCredentials credentials) {
        this.credentials = credentials;
        this.oldStyleCredential = null;
    }

    private FirebaseCredentials(com.google.api.client.googleapis.auth.oauth2.GoogleCredential credential) {
        this.credentials = null;
        this.oldStyleCredential = credential;
    }

    /**
     * Creates an empty instance of {@code FirebaseCredentials}.
     *
     * @return an empty {@code FirebaseCredentials}
     */
    public static FirebaseCredentials empty() {
        return new FirebaseCredentials();
    }

    /**
     * Creates a new {@code FirebaseCredentials} from the given {@link GoogleCredentials}.
     *
     * <p>This method sets scopes required to use the Firebase database.
     *
     * <p>This method is useful to create credentials from the
     * {@linkplain GoogleCredentials#getApplicationDefault() application default credentials}.
     *
     * @param credentials
     *         the credentials to create from
     * @return a new instance of {@code FirebaseCredentials}
     */
    public static FirebaseCredentials fromGoogleCredentials(GoogleCredentials credentials) {
        checkNotNull(credentials);
        GoogleCredentials scopedCredential = credentials.createScoped(AUTH_SCOPES);
        return new FirebaseCredentials(scopedCredential);
    }

    /**
     * Creates a new {@code FirebaseCredentials} from the given {@link GoogleCredentials}.
     *
     * <p>This method sets scopes required to use the Firebase database.
     *
     * @deprecated please use {@link #fromGoogleCredentials(GoogleCredentials)} or any other
     *             alternative instead
     */
    @Deprecated
    public static FirebaseCredentials fromGoogleCredentials(com.google.api.client.googleapis.auth.oauth2.GoogleCredential credentials) {
        checkNotNull(credentials);
        com.google.api.client.googleapis.auth.oauth2.GoogleCredential scopedCredential
                = credentials.createScoped(AUTH_SCOPES);
        return new FirebaseCredentials(scopedCredential);
    }

    /**
     * Creates a new instance of the {@code FirebaseCredentials} from the given credential stream.
     *
     * <p>Typically the stream will contain a service account JSON file data.
     *
     * <p>See <a href="https://firebase.google.com/docs/admin/setup#add_firebase_to_your_app">docs
     * </a> for details on how to obtain the file.
     *
     * @param credentialStream
     *         the stream containing credentials data
     * @return a new instance of {@code FirebaseCredentials}
     * @throws java.lang.IllegalArgumentException
     *         in case there are problems with parsing the given stream into
     *         {@link GoogleCredentials}
     */
    public static FirebaseCredentials fromStream(InputStream credentialStream) {
        checkNotNull(credentialStream);
        GoogleCredentials credentials = parseCredentials(credentialStream);
        return fromGoogleCredentials(credentials);
    }

    /**
     * Authenticates a given {@link HttpRequest} with the wrapped Google credentials instance.
     *
     * @throws IllegalStateException
     *         if this instance of {@code FirebaseCredentials} is {@linkplain #isEmpty() empty}
     */
    @Internal
    @Override
    public void initialize(HttpRequest request) throws IOException {
        checkState(!isEmpty(),
                   "An empty credentials instance cannot serve as HTTP request initializer.");
        if (isOldStyle()) {
            oldStyleCredential.initialize(request);
        } else {
            HttpRequestInitializer adapter = new HttpCredentialsAdapter(credentials);
            adapter.initialize(request);
        }
    }

    /**
     * Checks if the instance of the {@code FirebaseCredentials} is empty.
     *
     * @return {@code true} if {@code FirebaseCredentials} are empty and {@code false} otherwise
     */
    public boolean isEmpty() {
        return credentials == null && oldStyleCredential == null;
    }

    @VisibleForTesting
    boolean isOldStyle() {
        return oldStyleCredential != null;
    }

    private static GoogleCredentials parseCredentials(InputStream credentialStream) {
        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialStream);
            return credentials;
        } catch (IOException e) {
            throw newIllegalArgumentException(
                    e, "Exception when acquiring Firebase credentials: %s", e.getMessage());
        }
    }
}
