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
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.util.IOUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.utilities.Clock;
import com.google.firebase.database.utilities.DefaultClock;
import com.google.firebase.database.utilities.OffsetClock;
import com.google.gson.JsonObject;
import com.google.protobuf.Message;
import io.spine.client.QueryResponse;
import io.spine.json.Json;
import io.spine.protobuf.AnyPacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import static com.google.firebase.database.utilities.PushIdGenerator.generatePushChildName;
import static io.spine.web.firebase.FirebaseRest.byteArrayContent;
import static io.spine.web.firebase.FirebaseRest.getContent;
import static io.spine.web.firebase.FirebaseRest.httpRequestFactory;
import static io.spine.web.firebase.FirebaseRest.nodeUrl;
import static io.spine.web.firebase.FirebaseSubscriptionDiff.computeDiff;
import static java.util.stream.Collectors.toList;

/**
 * A subscription record that gets stored into a {@link FirebaseDatabase}.
 *
 * <p>Supports both an initial store and consequent updates of the stored data.
 *
 * @author Mykhailo Drachuk
 */
final class FirebaseSubscriptionRecord {

    private static final Clock CLOCK = new OffsetClock(new DefaultClock(), 0);

    private final FirebaseDatabasePath path;
    private final CompletionStage<QueryResponse> queryResponse;

    FirebaseSubscriptionRecord(FirebaseDatabasePath path,
                               CompletionStage<QueryResponse> queryResponse) {
        this.path = path;
        this.queryResponse = queryResponse;
    }

    /**
     * Retrieves the database path to this record.
     */
    FirebaseDatabasePath path() {
        return path;
    }

    /**
     * Writes this record to the given {@link FirebaseDatabase} as initial data, without checking
     * what is already stored in database at given location.
     */
    void storeAsInitial(FirebaseDatabase database) {
        DatabaseReference reference = path().reference(database);
        flushNewTo(reference);
    }

    /**
     * Flushes an array response of the query to the Firebase asynchronously,
     * adding array items to storage in a transaction.
     */
    @SuppressWarnings("Duplicates")
    private void flushNewTo(DatabaseReference reference) {
        queryResponse.thenAccept(response -> {
            try {
                List<String> newEntries = mapMessagesToJson(response).collect(toList());

                JsonObject jsonObject = new JsonObject();
                newEntries.forEach(entry -> put(entry, jsonObject));
                ByteArrayContent content = byteArrayContent(jsonObject);
                GenericUrl url = nodeUrl(reference);
                log().warn("Flushing new subscription content " + jsonObject.toString() + " to url " + url);
                HttpRequest request = httpRequestFactory().buildPutRequest(url, content);
                HttpResponse firebaseResponse = request.execute();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                IOUtils.copy(firebaseResponse.getContent(), outputStream);
                String firebaseResponseStr = outputStream.toString();
                log().warn("Firebase response to flushing new subscription: status - "
                        + firebaseResponse.getStatusCode() + ", text: " + firebaseResponseStr);
            } catch (Throwable e) {
                log().error("2: Exception when writing content of the new subscription: " + e.getLocalizedMessage());
            }
        });
    }

    /**
     * Adds the provided entries as children to the provided mutable record.
     *
     * @param currentData an instance of mutable data of existing subscription state
     * @param entries     new subscription entries to be added to Firebase
     */
    private static void addEntriesToData(MutableData currentData, Iterable<String> entries) {
        entries.forEach(entry -> currentData.child(newChildKey())
                                            .setValue(entry));
    }

    private static String newChildKey() {
        return generatePushChildName(CLOCK.millis());
    }

    /**
     * Stores the data to the Firebase, updating only the data that has changed.
     */
    void storeAsUpdate(FirebaseDatabase database) {
        DatabaseReference reference = path().reference(database);
        flushDiffTo(reference);
    }

    /**
     * Flushes an array response of the query to the Firebase asynchronously,
     * adding, removing and updating items already present in storage in a transaction.
     */
    @SuppressWarnings("Duplicates")
    private void flushDiffTo(DatabaseReference reference) {
        queryResponse.thenAccept(response -> {
            try {
                List<String> newEntries = mapMessagesToJson(response).collect(toList());

                String restData = getContent(reference);
                if ("null".equals(restData)) {
                    JsonObject jsonObject = new JsonObject();
                    newEntries.forEach(entry -> put(entry, jsonObject));
                    ByteArrayContent content = byteArrayContent(jsonObject);
                    GenericUrl url = nodeUrl(reference);
                    log().warn("Flushing kept up subscription content from scratch " + jsonObject.toString() + " to url " + url);
                    HttpRequest request = httpRequestFactory().buildPutRequest(url, content);
                    HttpResponse firebaseResponse = request.execute();
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    IOUtils.copy(firebaseResponse.getContent(), outputStream);
                    String firebaseResponseStr = outputStream.toString();
                    log().warn("Firebase response to flushing kept up subscription from scratch: status - "
                            + firebaseResponse.getStatusCode() + ", text: " + firebaseResponseStr);
                } else {
                    FirebaseSubscriptionDiff diff = computeDiff(newEntries, restData);
                    updateWithDiff(reference, diff);
                }
            } catch (Throwable e) {
                log().error("Exception when writing subscription content from scratch: "
                        + e.getLocalizedMessage());
            }
        });
    }

    private static void put(String entry, JsonObject jsonObject) {
        String key = newChildKey();
        jsonObject.addProperty(key, entry);
    }

    private static void updateWithDiff(DatabaseReference reference, FirebaseSubscriptionDiff diff) {
        JsonObject jsonObject = new JsonObject();

        diff.changed()
                .forEach(record -> {
                    log().warn("Changed: " + record.key());
                    jsonObject.addProperty(record.key(), record.data());
                });
        diff.removed()
                .forEach(record -> {
                    log().warn("Removed: " + record.key());
                    jsonObject.addProperty(record.key(), "null");
                });
        diff.added()
                .forEach(record -> {
                    final String key = newChildKey();
                    log().warn("Added: " + key);
                    jsonObject.addProperty(key, record.data());
                });

        ByteArrayContent content = byteArrayContent(jsonObject);
        try {
            GenericUrl url = nodeUrl(reference);
            log().warn("Flushing kept up subscription diff content " + jsonObject.toString() + " to url " + url);
            HttpRequest request = httpRequestFactory().buildPatchRequest(url, content);
            HttpResponse firebaseResponse = request.execute();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            IOUtils.copy(firebaseResponse.getContent(), outputStream);
            String firebaseResponseStr = outputStream.toString();
            log().warn("Firebase response to flushing kept up subscription diff: status - "
                    + firebaseResponse.getStatusCode() + ", text: " + firebaseResponseStr);
        } catch (Throwable e) {
            log().error("Exception when updating with diff: " + e.getLocalizedMessage());
        }
    }

    /**
     * Updates the provided MutableData with provided subscription diff.
     *
     * @param currentData an instance of mutable data of existing subscription state
     * @param diff        a diff between updated and existing subscription states
     */
    private static void updateWithDiff(MutableData currentData, FirebaseSubscriptionDiff diff) {
        diff.changed()
                .forEach(record -> {
                    log().warn("Changed: " + record.key());
                    currentData.child(record.key())
                            .setValue(record.data());
                });
        diff.removed()
                .forEach(record -> {
                    log().warn("Removed: " + record.key());
                    currentData.child(record.key())
                            .setValue(null);
                });
        diff.added()
                .forEach(record -> {
                    final String key = newChildKey();
                    log().warn("Added: " + key);
                    currentData.child(key)
                            .setValue(record.data());
                });
    }

    /**
     * Creates a stream of response messages, mapping each each response message to JSON.
     *
     * @param response Spines response to a query
     * @return a stream of messages represented by JSON strings
     */
    @SuppressWarnings("RedundantTypeArguments") // AnyPacker::unpack type cannot be inferred.
    private static Stream<String> mapMessagesToJson(QueryResponse response) {
        return response.getMessagesList()
                       .stream()
                       .unordered()
                       .map(AnyPacker::<Message>unpack)
                       .map(Json::toCompactJson);
    }

    /**
     * An abstract base for a subscription transaction handler.
     *
     * <p>Logs an error in case it occurs, leaving the {@link #doTransaction(MutableData)}
     * to actual implementors.
     */
    private abstract static class SubscriptionUpdateTransactionHandler
            implements Transaction.Handler {

        @Override
        public void onComplete(DatabaseError error, boolean committed,
                               DataSnapshot currentData) {
            if (!committed) {
                log().error("Subscription update was not committed to the Firebase.");
                if (error != null) {
                    log().error(error.getMessage());
                }
            }
        }
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass") // A log method is placed in the outer class.
    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(FirebaseSubscriptionRecord.class);
    }
}
