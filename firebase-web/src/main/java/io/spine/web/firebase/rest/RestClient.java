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
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequestFactory;
import com.google.common.annotations.VisibleForTesting;
import io.spine.web.firebase.DatabaseUrl;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.NodeValue;
import io.spine.web.firebase.StoredJson;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.web.firebase.rest.RestNodeUrls.asGenericUrl;

/**
 * A {@code FirebaseClient} which operates via the Firebase REST API.
 *
 * See Firebase REST API <a href="https://firebase.google.com/docs/reference/rest/database/">docs
 * </a>.
 */
public final class RestClient implements FirebaseClient {


    @VisibleForTesting
    static final String NULL_ENTRY = "null";

    private final RestNodeUrls factory;
    private final HttpClient httpClient;

    @VisibleForTesting
    RestClient(RestNodeUrls factory, HttpClient httpClient) {
        this.factory = factory;
        this.httpClient = httpClient;
    }

    /**
     * Creates a {@code RestClient} which operates on the database located at the given
     * {@code url} and uses the given {@code requestFactory} to prepare HTTP requests.
     */
    public static RestClient create(DatabaseUrl url, HttpRequestFactory requestFactory) {
        RestNodeUrls nodeUrlTemplate = new RestNodeUrls(url);
        HttpClient requestExecutor = HttpClient.using(requestFactory);
        return new RestClient(nodeUrlTemplate, requestExecutor);
    }

    @Override
    public Optional<NodeValue> get(NodePath nodePath) {
        checkNotNull(nodePath);

        GenericUrl nodeUrl = asGenericUrl(factory.with(nodePath));
        String data = httpClient.get(nodeUrl);
        StoredJson json = StoredJson.from(data);
        Optional<NodeValue> nodeValue = Optional.of(json)
                                                .filter(value -> !value.isNull())
                                                .map(StoredJson::asNodeValue);
        return nodeValue;
    }

    @Override
    public void create(NodePath nodePath, NodeValue value) {
        checkNotNull(nodePath);
        checkNotNull(value);

        GenericUrl nodeUrl = asGenericUrl(factory.with(nodePath));
        ByteArrayContent byteArrayContent = value.toByteArray();
        create(nodeUrl, byteArrayContent);
    }

    @Override
    public void update(NodePath nodePath, NodeValue value) {
        checkNotNull(nodePath);
        checkNotNull(value);

        GenericUrl nodeUrl = asGenericUrl(factory.with(nodePath));
        ByteArrayContent byteArrayContent = value.toByteArray();
        update(nodeUrl, byteArrayContent);
    }

    /**
     * Creates the database node with the given value or overwrites the existing one.
     */
    private void create(GenericUrl nodeUrl, HttpContent value) {
        httpClient.put(nodeUrl, value);
    }

    /**
     * Updates the database node with the given value.
     *
     * <p>Common entries are overwritten.
     */
    private void update(GenericUrl nodeUrl, HttpContent value) {
        httpClient.patch(nodeUrl, value);
    }
}
