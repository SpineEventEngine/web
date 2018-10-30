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
import com.google.firebase.database.utilities.Clock;
import com.google.firebase.database.utilities.DefaultClock;
import com.google.firebase.database.utilities.OffsetClock;
import com.google.gson.JsonObject;
import io.spine.web.http.RequestExecutor;

import java.util.concurrent.ThreadFactory;

import static com.google.common.net.MediaType.JSON_UTF_8;
import static com.google.firebase.database.utilities.PushIdGenerator.generatePushChildName;

class FirebaseRestClient implements FirebaseClient {

    private static final Clock CLOCK = new OffsetClock(new DefaultClock(), 0);

    private final RequestExecutor requestExecutor;
    private final ThreadFactory threadFactory;

    private FirebaseRestClient(RequestExecutor requestExecutor, ThreadFactory threadFactory) {
        this.requestExecutor = requestExecutor;
        this.threadFactory = threadFactory;
    }

    static FirebaseRestClient create(HttpTransport httpTransport, ThreadFactory threadFactory) {
        RequestExecutor requestExecutor = RequestExecutor.using(httpTransport);
        return new FirebaseRestClient(requestExecutor, threadFactory);
    }

    @Override
    public String get(String nodeUrl) {
        GenericUrl url = new GenericUrl(nodeUrl);
        String data = requestExecutor.get(url);
        return data;
    }

    @Override
    public void add(String nodeUrl, String value) {
        Thread thread = threadFactory.newThread(() -> addOrUpdate(nodeUrl, value));
        thread.start();
    }

    @Override
    public void update(String nodeUrl, JsonObject jsonObject) {
        Thread thread = threadFactory.newThread(() -> doUpdate(nodeUrl, jsonObject));
        thread.start();
    }

    @Override
    public void overwrite(String nodeUrl, JsonObject jsonObject) {
        Thread thread = threadFactory.newThread(() -> add(nodeUrl, jsonObject));
        thread.start();
    }

    /**
     * Adds the value to the referenced Firebase array path.
     *
     * @param nodeUrl a Firebase array reference which can be appended an object.
     * @param value      a String value to add to an Array inside of Firebase
     * @return a {@code Future} of an item being added
     */
    private void addOrUpdate(String nodeUrl, String value) {
        JsonObject firebaseEntry = toFirebaseEntry(value);
        if (!exists(nodeUrl)) {
            // todo make a separate FirebaseEntry class with NodeUrl and Data.
            add(nodeUrl, firebaseEntry);
        } else {
            doUpdate(nodeUrl, firebaseEntry);
        }
    }

    private boolean exists(String nodeUrl) {
        String content = get(nodeUrl);
        return !isNullData(content);
    }

    private void add(String nodeUrl, JsonObject firebaseEntry) {
        GenericUrl genericUrl = new GenericUrl(nodeUrl);
        ByteArrayContent content = byteArrayContent(firebaseEntry);
        requestExecutor.put(genericUrl, content);
    }

    // todo address naming
    private void doUpdate(String nodeUrl, JsonObject firebaseEntry) {
        GenericUrl genericUrl = new GenericUrl(nodeUrl);
        ByteArrayContent content = byteArrayContent(firebaseEntry);
        requestExecutor.patch(genericUrl, content);
    }

    private static JsonObject toFirebaseEntry(String value) {
        String generatedKey = newChildKey();
        JsonObject entry = new JsonObject();
        entry.addProperty(generatedKey, value);
        return entry;
    }

    static boolean isNullData(String value) {
        return "null".equals(value);
    }

    private static String newChildKey() {
        return generatePushChildName(CLOCK.millis());
    }

    private static ByteArrayContent byteArrayContent(JsonObject jsonObject) {
        String jsonString = jsonObject.toString();
        ByteArrayContent result = ByteArrayContent.fromString(JSON_UTF_8.toString(), jsonString);
        return result;
    }
}
