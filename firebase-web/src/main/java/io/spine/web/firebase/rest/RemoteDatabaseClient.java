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
import com.google.api.client.util.BackOff;
import com.google.common.annotations.VisibleForTesting;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.FirebaseDatabase;
import io.spine.web.firebase.DatabaseUrls;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.NodeValue;
import io.spine.web.firebase.StoredJson;

import java.util.Optional;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.web.firebase.rest.RestNodeUrls.asGenericUrl;

/**
 * A {@code FirebaseClient} which operates via the Firebase REST API and the Java Admin SDK.
 *
 * <p>The client uses the Java Firebase Admin SDK for subscribing to events of a given database
 * node. The API exposes the {@link ChildEventListener} so that the caller may build more complex
 * "nested" subscriptions without a need to re-fetch database references.
 *
 * <p>For all the other operations, the client uses the Firebase REST API as described in the
 * <a href="https://firebase.google.com/docs/reference/rest/database/">documentation</a>.
 */
public final class RemoteDatabaseClient implements FirebaseClient {

    private final FirebaseDatabase database;
    private final RestNodeUrls factory;
    private final HttpClient httpClient;

    private RemoteDatabaseClient(Builder builder) {
        this(builder.database, builder.buildNodeUrls(), builder.buildHttpClient());
    }

    @VisibleForTesting
    RemoteDatabaseClient(FirebaseDatabase database, RestNodeUrls factory, HttpClient httpClient) {
        this.database = database;
        this.factory = factory;
        this.httpClient = httpClient;
    }

    @Override
    public Optional<NodeValue> fetchNode(NodePath nodePath) {
        checkNotNull(nodePath);

        GenericUrl nodeUrl = url(nodePath);
        String data = httpClient.get(nodeUrl);
        StoredJson json = StoredJson.from(data);
        Optional<NodeValue> nodeValue = Optional.of(json)
                                                .filter(value -> !value.isNull())
                                                .map(StoredJson::asNodeValue);
        return nodeValue;
    }

    @Override
    public void subscribeTo(NodePath nodePath, ChildEventListener listener) {
        checkNotNull(nodePath);
        checkNotNull(listener);
        database.getReference(nodePath.getValue())
                .addChildEventListener(listener);
    }

    @Override
    public void create(NodePath nodePath, NodeValue value) {
        checkNotNull(nodePath);
        checkNotNull(value);

        GenericUrl nodeUrl = url(nodePath);
        ByteArrayContent byteArrayContent = value.toByteArray();
        httpClient.put(nodeUrl, byteArrayContent);
    }

    @Override
    public void update(NodePath nodePath, NodeValue value) {
        checkNotNull(nodePath);
        checkNotNull(value);

        GenericUrl nodeUrl = url(nodePath);
        ByteArrayContent byteArrayContent = value.toByteArray();
        httpClient.patch(nodeUrl, byteArrayContent);
    }

    @Override
    public void delete(NodePath nodePath) {
        checkNotNull(nodePath);

        GenericUrl nodeUrl = url(nodePath);
        httpClient.delete(nodeUrl);
    }

    private GenericUrl url(NodePath nodePath) {
        return asGenericUrl(factory.with(nodePath));
    }

    /**
     * Creates a new instance of {@code Builder} for {@code RemoteDatabaseClient} instances.
     *
     * @return new instance of {@code Builder}
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * A builder for the {@code RemoteDatabaseClient} instances.
     */
    public static final class Builder {

        private FirebaseDatabase database;
        private HttpRequestFactory requestFactory;
        private Supplier<BackOff> backOff = () -> BackOff.STOP_BACKOFF;

        /**
         * Prevents direct instantiation.
         */
        private Builder() {
        }

        public Builder setDatabase(FirebaseDatabase database) {
            this.database = checkNotNull(database);
            return this;
        }

        public Builder setRequestFactory(HttpRequestFactory requestFactory) {
            this.requestFactory = checkNotNull(requestFactory);
            return this;
        }

        public Builder setBackOff(Supplier<BackOff> backOff) {
            this.backOff = checkNotNull(backOff);
            return this;
        }

        /**
         * Creates a new instance of {@code RemoteDatabaseClient}.
         *
         * @return new instance of {@code RemoteDatabaseClient}
         */
        public RemoteDatabaseClient build() {
            checkState(database != null);
            checkState(requestFactory != null);
            return new RemoteDatabaseClient(this);
        }

        private RestNodeUrls buildNodeUrls() {
            String databaseUrl = database
                    .getApp()
                    .getOptions()
                    .getDatabaseUrl();
            RestNodeUrls urls = new RestNodeUrls(DatabaseUrls.from(databaseUrl));
            return urls;
        }

        private HttpClient buildHttpClient() {
            return HttpClient.using(requestFactory, backOff);
        }
    }
}
