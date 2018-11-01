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

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import io.spine.web.http.RequestExecutor;

import java.util.Optional;

/**
 * A {@code FirebaseClient} which operates via Firebase REST API.
 *
 * See Firebase REST API <a href="https://firebase.google.com/docs/reference/rest/database/">docs
 * </a>.
 */
class FirebaseRestClient implements FirebaseClient {

    /**
     * The format by which Firebase database nodes are accessible via REST API.
     *
     * <p>The first placeholder is the database URL and the second one is a node path.
     */
    private static final String NODE_URL_FORMAT = "%s/%s.json";

    private final String databaseUrl;
    private final RequestExecutor requestExecutor;

    private FirebaseRestClient(String databaseUrl, RequestExecutor requestExecutor) {
        this.databaseUrl = databaseUrl;
        this.requestExecutor = requestExecutor;
    }

    /**
     * Create a {@code FirebaseRestClient} which operates on the database located at given
     * {@code databaseUrl} and uses given {@code httpTransport}.
     */
    static FirebaseRestClient create(String databaseUrl, HttpTransport httpTransport) {
        RequestExecutor requestExecutor = RequestExecutor.using(httpTransport);
        return new FirebaseRestClient(databaseUrl, requestExecutor);
    }

    @Override
    public Optional<FirebaseNodeContent> get(FirebaseDatabasePath nodePath) {
        GenericUrl url = toNodeUrl(nodePath);
        String data = requestExecutor.get(url);
        if (isNullData(data)) {
            return Optional.empty();
        }
        FirebaseNodeContent content = FirebaseNodeContent.from(data);
        Optional<FirebaseNodeContent> result = Optional.of(content);
        return result;
    }

    @Override
    public void addContent(FirebaseDatabasePath nodePath, FirebaseNodeContent content) {
        GenericUrl url = toNodeUrl(nodePath);
        ByteArrayContent byteArrayContent = content.toByteArray();
        Optional<FirebaseNodeContent> existingContent = get(nodePath);
        if (!existingContent.isPresent()) {
            create(url, byteArrayContent);
        } else {
            update(url, byteArrayContent);
        }
    }

    /**
     * Creates the database node or overwrites the node content by the given content.
     */
    private void create(GenericUrl nodeUrl, ByteArrayContent content) {
        requestExecutor.put(nodeUrl, content);
    }

    /**
     * Updates the database node with the given content.
     *
     * <p>Common entries are overwritten.
     */
    private void update(GenericUrl nodeUrl, ByteArrayContent content) {
        requestExecutor.patch(nodeUrl, content);
    }

    private GenericUrl toNodeUrl(FirebaseDatabasePath nodePath) {
        String url = String.format(NODE_URL_FORMAT, databaseUrl, nodePath);
        return new GenericUrl(url);
    }

    private static boolean isNullData(String data) {
        return "null".equals(data);
    }
}
