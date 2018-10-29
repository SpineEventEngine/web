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

import com.google.api.client.http.*;
import com.google.api.client.util.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ThreadFactory;

import static io.spine.util.Exceptions.newIllegalStateException;

public class FirebaseRestClient implements FirebaseClient {

    private final HttpRequestFactory requestFactory;
    private final ThreadFactory threadFactory;

    private FirebaseRestClient(HttpTransport httpTransport, ThreadFactory threadFactory) {
        this.requestFactory = requestFactory(httpTransport);
        this.threadFactory = threadFactory;
    }

    @Override
    public void set(String nodeUrl, String value) {
        Thread thread = threadFactory.newThread(() -> {
            addOrUpdate(nodeUrl, value);
        });
        thread.start();
    }

    private void addOrUpdate(String nodeUrl, String value) {
        if (!exists(nodeUrl)) {
            add(nodeUrl, value);
        } else {
            update(nodeUrl, value);
        }
    }

    private boolean exists(String nodeUrl) {
        String value = getValue(nodeUrl);
        return !isNull(value);
    }

    private String getValue(String nodeUrl) {
        GenericUrl genericUrl = new GenericUrl(nodeUrl);
        try {
            HttpRequest getRequest = requestFactory.buildGetRequest(genericUrl);
            HttpResponse getResponse = getRequest.execute();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            IOUtils.copy(getResponse.getContent(), outputStream);
            String result = outputStream.toString();
            return result;
        } catch (IOException e) {
            throw newIllegalStateException(e, e.getMessage());
        }
    }

    private static boolean isNull(String value) {
        return "null".equals(value);
    }

    private void add(String nodeUrl, String value) {

    }

    private void update(String nodeUrl, String value) {

    }

    private static HttpRequestFactory requestFactory(HttpTransport httpTransport) {
        HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
        return requestFactory;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private HttpTransport httpTransport;
        private ThreadFactory threadFactory;

        private Builder() {
        }

        public Builder setHttpTransport(HttpTransport httpTransport) {
            this.httpTransport = httpTransport;
            return this;
        }

        public Builder setThreadFactory(ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
            return this;
        }

        public FirebaseRestClient build() {
            return new FirebaseRestClient(httpTransport, threadFactory);
        }
    }
}
