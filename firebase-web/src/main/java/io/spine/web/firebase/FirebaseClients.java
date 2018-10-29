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

import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.appengine.api.utils.SystemProperty;

import java.util.concurrent.ThreadFactory;

import static com.google.appengine.api.ThreadManager.backgroundThreadFactory;
import static java.util.concurrent.Executors.defaultThreadFactory;

final class FirebaseClients {

    private FirebaseClients() {
    }

    static FirebaseClient forCurrentEnv() {
        try {
            Class.forName(SystemProperty.class.getName());
            return gae();
        } catch (ClassNotFoundException ignored) {
            return local();
        }
    }

    private static FirebaseClient local() {
        ApacheHttpTransport httpTransport = new ApacheHttpTransport();
        ThreadFactory threadFactory = defaultThreadFactory();
        return create(httpTransport, threadFactory);
    }

    private static FirebaseClient gae() {
        UrlFetchTransport httpTransport = new UrlFetchTransport();
        ThreadFactory threadFactory = backgroundThreadFactory();
        return create(httpTransport, threadFactory);
    }

    private static FirebaseClient
    create(HttpTransport httpTransport, ThreadFactory threadFactory) {
        FirebaseClient client = FirebaseRestClient
                .newBuilder()
                .setHttpTransport(httpTransport)
                .setThreadFactory(threadFactory)
                .build();
        return client;
    }
}
