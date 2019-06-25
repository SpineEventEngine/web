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

package io.spine.web.firebase;

import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.common.annotations.VisibleForTesting;
import io.spine.server.DeploymentType;
import io.spine.server.ServerEnvironment;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.DeploymentType.APPENGINE_CLOUD;
import static io.spine.server.DeploymentType.APPENGINE_EMULATOR;
import static io.spine.web.firebase.rest.RestClient.create;

/**
 * A tool for {@link FirebaseClient} instances creation.
 */
public final class FirebaseClientFactory {

    /** Prevents instantiation of this static factory. */
    private FirebaseClientFactory() {
    }

    /**
     * Creates a {@linkplain io.spine.web.firebase.rest.RestClient firebase client} which
     * operates via the Firebase REST API.
     *
     * <p>The client created with this method is suitable only for databases whose read/write side
     * is public, to access the databases requiring authentication, use
     * {@link #restClient(DatabaseUrl, FirebaseCredentials)}.
     *
     * @param url
     *         the URL of the database on which the client operates
     * @return the new instance of {@code RestClient}
     */
    public static FirebaseClient restClient(DatabaseUrl url) {
        checkNotNull(url);
        return forCurrentEnv(url, FirebaseCredentials.empty());
    }

    /**
     * Creates a {@link io.spine.web.firebase.rest.RestClient} which uses given credentials to
     * authorize its requests to the Firebase database.
     *
     * @param url
     *         the URL of the database on which the client operates
     * @param credentials
     *         the Firebase Database credentials to use
     * @return the new instance of {@code RestClient}
     */
    public static FirebaseClient restClient(DatabaseUrl url, FirebaseCredentials credentials) {
        checkNotNull(url);
        checkNotNull(credentials);
        return forCurrentEnv(url, credentials);
    }

    /**
     * Creates a {@link io.spine.web.firebase.rest.RestClient} for the current environment.
     *
     * <p>Different environments require different {@linkplain HttpTransport HTTP transport}
     * to operate.
     *
     * <p>For more information, see
     * <a href="https://developers.google.com/api-client-library/java/google-http-java-client/reference/1.20.0/com/google/api/client/http/HttpTransport">
     * HttpTransport docs</a>.
     */
    private static FirebaseClient forCurrentEnv(DatabaseUrl url, FirebaseCredentials credentials) {
        DeploymentType deploymentType = ServerEnvironment.deploymentType();
        if (deploymentType == APPENGINE_CLOUD || deploymentType == APPENGINE_EMULATOR) {
            return gae(url, credentials);
        } else {
            return nonGae(url, credentials);
        }
    }

    /**
     * Creates a {@code FirebaseClient} for usage in the Google AppEngine environment.
     */
    @VisibleForTesting
    static FirebaseClient gae(DatabaseUrl url, FirebaseCredentials credentials) {
        UrlFetchTransport urlFetchTransport = UrlFetchTransport.getDefaultInstance();
        return createWithTransport(urlFetchTransport, url, credentials);
    }

    /**
     * Creates a {@code FirebaseClient} for usage in the non-GAE environment.
     */
    @VisibleForTesting
    static FirebaseClient nonGae(DatabaseUrl url, FirebaseCredentials credentials) {
        ApacheHttpTransport apacheHttpTransport = new ApacheHttpTransport();
        return createWithTransport(apacheHttpTransport, url, credentials);
    }

    private static FirebaseClient createWithTransport(HttpTransport httpTransport,
                                                      DatabaseUrl url,
                                                      FirebaseCredentials credentials) {
        if (credentials.isEmpty()) {
            return createUnauthorized(httpTransport, url);
        }
        return createAuthorized(httpTransport, url, credentials);
    }

    /**
     * Creates a {@code FirebaseClient} authorized with the given credentials.
     */
    private static FirebaseClient createAuthorized(HttpTransport httpTransport,
                                                   DatabaseUrl url,
                                                   FirebaseCredentials credentials) {
        GoogleCredential googleCredentials = credentials.credentials();
        HttpRequestFactory requestFactory = httpTransport.createRequestFactory(googleCredentials);
        return create(url, requestFactory);
    }

    /**
     * Creates an unauthorized {@code FirebaseClient} to access the database with public rules.
     */
    private static FirebaseClient createUnauthorized(HttpTransport httpTransport,
                                                     DatabaseUrl url) {
        HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
        return create(url, requestFactory);
    }
}
