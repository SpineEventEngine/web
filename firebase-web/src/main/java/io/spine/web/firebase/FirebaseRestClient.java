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

class FirebaseRestClient implements FirebaseClient {

    private final RequestExecutor requestExecutor;

    private FirebaseRestClient(RequestExecutor requestExecutor) {
        this.requestExecutor = requestExecutor;
    }

    static FirebaseRestClient create(HttpTransport httpTransport) {
        RequestExecutor requestExecutor = RequestExecutor.using(httpTransport);
        return new FirebaseRestClient(requestExecutor);
    }

    @Override
    public Optional<NodeContent> get(NodeUrl nodeUrl) {
        GenericUrl url = nodeUrl.toGenericUrl();
        String data = requestExecutor.get(url);
        if (isNullData(data)) {
            return Optional.empty();
        }
        NodeContent content = NodeContent.from(data);
        Optional<NodeContent> result = Optional.of(content);
        return result;
    }

    @Override
    public void addContent(NodeUrl nodeUrl, NodeContent content) {
        Optional<NodeContent> existingContent = get(nodeUrl);
        if (!existingContent.isPresent()) {
            add(nodeUrl, content);
        } else {
            update(nodeUrl, content);
        }
    }

    private void add(NodeUrl nodeUrl, NodeContent content) {
        GenericUrl genericUrl = nodeUrl.toGenericUrl();
        ByteArrayContent byteArrayContent = content.toByteArray();
        requestExecutor.put(genericUrl, byteArrayContent);
    }

    private void update(NodeUrl nodeUrl, NodeContent content) {
        GenericUrl genericUrl = nodeUrl.toGenericUrl();
        ByteArrayContent byteArrayContent = content.toByteArray();
        requestExecutor.patch(genericUrl, byteArrayContent);
    }

    private static boolean isNullData(String value) {
        return "null".equals(value);
    }
}
