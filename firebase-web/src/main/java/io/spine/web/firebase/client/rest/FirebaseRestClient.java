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

package io.spine.web.firebase.client.rest;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequestFactory;
import com.google.common.annotations.VisibleForTesting;
import io.spine.web.firebase.client.DatabasePath;
import io.spine.web.firebase.client.DatabaseUrl;
import io.spine.web.firebase.client.FirebaseClient;
import io.spine.web.firebase.client.NodeValue;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * A {@code FirebaseClient} which operates via the Firebase REST API.
 *
 * See Firebase REST API <a href="https://firebase.google.com/docs/reference/rest/database/">docs
 * </a>.
 */
public class FirebaseRestClient implements FirebaseClient {

    /**
     * The format by which Firebase database nodes are accessible via REST API.
     *
     * <p>The first placeholder is the database URL and the second one is a node path.
     */
    private static final String NODE_URL_FORMAT = "%s/%s.json";

    /**
     * The representation of the database {@code null} entry.
     *
     * <p>In Firebase the {@code null} node is deemed nonexistent.
     */
    @SuppressWarnings("DuplicateStringLiteralInspection")
    @VisibleForTesting
    static final String NULL_ENTRY = "null";

    private final String nodeAccessFormat;
    private final HttpRequestExecutor requestExecutor;

    @VisibleForTesting
    public FirebaseRestClient(String nodeAccessFormat, HttpRequestExecutor requestExecutor) {
        this.nodeAccessFormat = nodeAccessFormat;
        this.requestExecutor = requestExecutor;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    /**
     * Creates a {@code FirebaseRestClient} which operates on the database located at the given
     * {@code url} and uses the given {@code requestFactory} to prepare HTTP requests.
     */
    public static FirebaseRestClient create(DatabaseUrl url, HttpRequestFactory requestFactory) {
        String nodeAccessFormat = nodeAccessFormat(url);
        HttpRequestExecutor requestExecutor = HttpRequestExecutor.using(requestFactory);
        return new FirebaseRestClient(nodeAccessFormat, requestExecutor);
    }

    /**
     * Returns format in which the individual nodes are accessed for the given database.
     *
     * <p>The format is a string with one placeholder which should be substituted by the node path.
     */
    private static String nodeAccessFormat(DatabaseUrl databaseUrl) {
        String nodePathPlaceholder = "%s";
        String result = format(NODE_URL_FORMAT, databaseUrl, nodePathPlaceholder);
        return result;
    }

    @Override
    public Optional<NodeValue> get(DatabasePath nodePath) {
        checkNotNull(nodePath);

        GenericUrl url = toNodeUrl(nodePath);
        String data = requestExecutor.get(url);
        if (isNullData(data)) {
            return Optional.empty();
        }
        NodeValue value = NodeValue.from(data);
        Optional<NodeValue> result = Optional.of(value);
        return result;
    }

    @Override
    public void merge(DatabasePath nodePath, NodeValue value) {
        checkNotNull(nodePath);
        checkNotNull(value);

        GenericUrl url = toNodeUrl(nodePath);
        ByteArrayContent byteArrayContent = value.toByteArray();
        Optional<NodeValue> existingValue = get(nodePath);
        if (!existingValue.isPresent()) {
            create(url, byteArrayContent);
        } else {
            update(url, byteArrayContent);
        }
    }

    /**
     * Creates the database node with the given value or overwrites the existing one.
     */
    private void create(GenericUrl nodeUrl, HttpContent value) {
        requestExecutor.put(nodeUrl, value);
    }

    /**
     * Updates the database node with the given value.
     *
     * <p>Common entries are overwritten.
     */
    private void update(GenericUrl nodeUrl, HttpContent value) {
        requestExecutor.patch(nodeUrl, value);
    }

    private GenericUrl toNodeUrl(DatabasePath nodePath) {
        String url = format(nodeAccessFormat, nodePath);
        return new GenericUrl(url);
    }

    private static boolean isNullData(String data) {
        return NULL_ENTRY.equals(data);
    }
}
