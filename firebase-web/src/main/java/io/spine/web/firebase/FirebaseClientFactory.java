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

import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.util.BackOff;
import com.google.api.client.util.ExponentialBackOff;
import com.google.firebase.database.FirebaseDatabase;
import io.spine.server.ServerEnvironment;
import io.spine.web.firebase.rest.RemoteDatabaseClient;

import java.time.Duration;
import java.util.function.Supplier;

import static com.google.api.client.util.BackOff.STOP_BACKOFF;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.DeploymentType.APPENGINE_CLOUD;
import static io.spine.server.DeploymentType.APPENGINE_EMULATOR;

/**
 * A tool for {@link FirebaseClient} instances creation.
 */
public final class FirebaseClientFactory {

    private static final Duration BACK_OFF_MAX_DURATION = Duration.ofSeconds(10);

    /** Prevents instantiation of this static factory. */
    private FirebaseClientFactory() {
    }

    /**
     * Creates a {@linkplain RemoteDatabaseClient firebase client} which
     * operates via the Firebase REST API.
     *
     * <p>The client created with this method is suitable only for databases whose read/write side
     * is public, to access the databases requiring authentication, use
     * {@link #remoteClient(FirebaseDatabase, FirebaseCredentials)}.
     *
     * @param database
     *         the Firebase database to connect to
     * @return the new instance of {@code RestClient}
     */
    public static FirebaseClient remoteClient(FirebaseDatabase database) {
        checkNotNull(database);
        return remoteClient(database, FirebaseCredentials.empty());
    }

    /**
     * Creates a {@link RemoteDatabaseClient} which uses given
     * credentials to authorize its requests to the Firebase database.
     *
     * @param database
     *         the Firebase database to connect to
     * @param credentials
     *         the Firebase Database credentials to use
     * @return the new instance of {@code RestClient}
     */
    public static FirebaseClient
    remoteClient(FirebaseDatabase database, FirebaseCredentials credentials) {
        checkNotNull(database);
        checkNotNull(credentials);
        return forCurrentEnv(database, credentials);
    }

    /**
     * Creates a {@link RemoteDatabaseClient} for the current
     * environment.
     *
     * <p>Different environments require different {@linkplain HttpTransport HTTP transport}
     * to operate.
     *
     * <p>For more information, see
     * <a href="https://developers.google.com/api-client-library/java/google-http-java-client/reference/1.20.0/com/google/api/client/http/HttpTransport">
     * HttpTransport docs</a>.
     */
    private static FirebaseClient
    forCurrentEnv(FirebaseDatabase database, FirebaseCredentials credentials) {
        var deploymentType = ServerEnvironment.instance().deploymentType();
        return deploymentType == APPENGINE_CLOUD || deploymentType == APPENGINE_EMULATOR
               ? gae(database, credentials)
               : nonGae(database, credentials);
    }

    /**
     * Creates a {@code FirebaseClient} for usage in the Google AppEngine environment.
     */
    private static FirebaseClient gae(FirebaseDatabase database,
                                      FirebaseCredentials credentials) {
        var urlFetchTransport = UrlFetchTransport.getDefaultInstance();
        return createWithTransport(urlFetchTransport, database, credentials, () -> STOP_BACKOFF);
    }

    /**
     * Creates a {@code FirebaseClient} for usage in the non-GAE environment.
     */
    private static FirebaseClient
    nonGae(FirebaseDatabase database, FirebaseCredentials credentials) {
        var apacheHttpTransport = new ApacheHttpTransport();
        return createWithTransport(apacheHttpTransport,
                                   database,
                                   credentials,
                                   FirebaseClientFactory::apacheTransportBackOff);
    }

    private static BackOff apacheTransportBackOff() {
        @SuppressWarnings("NumericCastThatLosesPrecision") // Should not be a large number.
        var maxElapsedTime = (int) BACK_OFF_MAX_DURATION.toMillis();
        return new ExponentialBackOff.Builder()
                .setMaxElapsedTimeMillis(maxElapsedTime)
                .build();
    }

    private static FirebaseClient createWithTransport(HttpTransport httpTransport,
                                                      FirebaseDatabase database,
                                                      FirebaseCredentials credentials,
                                                      Supplier<BackOff> backOff) {
        return credentials.isEmpty()
               ? createUnauthorized(httpTransport, database, backOff)
               : createAuthorized(httpTransport, database, credentials, backOff);
    }

    /**
     * Creates a {@code FirebaseClient} authorized with the given credentials.
     */
    private static FirebaseClient createAuthorized(HttpTransport httpTransport,
                                                   FirebaseDatabase database,
                                                   FirebaseCredentials credentials,
                                                   Supplier<BackOff> backOff) {
        var requestFactory = httpTransport.createRequestFactory(credentials);
        return RemoteDatabaseClient.newBuilder()
                .setDatabase(database)
                .setRequestFactory(requestFactory)
                .setBackOff(backOff)
                .build();
    }

    /**
     * Creates an unauthorized {@code FirebaseClient} to access the database with public rules.
     */
    private static FirebaseClient createUnauthorized(HttpTransport httpTransport,
                                                     FirebaseDatabase database,
                                                     Supplier<BackOff> backOff) {
        var requestFactory = httpTransport.createRequestFactory();
        return RemoteDatabaseClient.newBuilder()
                .setDatabase(database)
                .setRequestFactory(requestFactory)
                .setBackOff(backOff)
                .build();
    }
}
