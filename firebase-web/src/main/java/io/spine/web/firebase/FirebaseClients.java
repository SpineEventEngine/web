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
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.appengine.api.utils.SystemProperty;

import static io.spine.web.firebase.FirebaseRestClient.create;

public final class FirebaseClients {

    private FirebaseClients() {
    }

    public static FirebaseClient rest(String databaseUrl) {
        return restForCurrentEnv(databaseUrl);
    }

    private static FirebaseClient restForCurrentEnv(String databaseUrl) {
        try {
            Class.forName(SystemProperty.class.getName());
            return gae(databaseUrl);
        } catch (ClassNotFoundException ignored) {
            return local(databaseUrl);
        }
    }

    private static FirebaseClient local(String databaseUrl) {
        ApacheHttpTransport httpTransport = new ApacheHttpTransport();
        return create(databaseUrl, httpTransport);
    }

    private static FirebaseClient gae(String databaseUrl) {
        UrlFetchTransport httpTransport = UrlFetchTransport.getDefaultInstance();
        return create(databaseUrl, httpTransport);
    }
}
