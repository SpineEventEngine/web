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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.utilities.Clock;
import com.google.firebase.database.utilities.DefaultClock;
import com.google.firebase.database.utilities.OffsetClock;
import com.google.protobuf.Message;
import io.spine.client.QueryResponse;
import io.spine.json.Json;
import io.spine.protobuf.AnyPacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import static com.google.firebase.database.Transaction.success;
import static com.google.firebase.database.utilities.PushIdGenerator.generatePushChildName;
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
    private void flushNewTo(DatabaseReference reference) {
        queryResponse.thenAcceptAsync(response -> {
            List<String> newEntries = mapMessagesToJson(response).collect(toList());
            reference.runTransaction(new SubscriptionUpdateTransactionHandler() {
                @Override
                public Transaction.Result doTransaction(MutableData currentData) {
                    addEntriesToData(currentData, newEntries);
                    return success(currentData);
                }
            });
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
    private void flushDiffTo(DatabaseReference reference) {
        queryResponse.thenAcceptAsync(response -> {
            List<String> newEntries = mapMessagesToJson(response).collect(toList());
            reference.runTransaction(new SubscriptionUpdateTransactionHandler() {
                @Override
                public Transaction.Result doTransaction(MutableData currentData) {
                    Iterable<MutableData> children = currentData.getChildren();
                    FirebaseSubscriptionDiff diff = computeDiff(newEntries, children);
                    updateWithDiff(currentData, diff);
                    return success(currentData);
                }
            });
        });
    }

    /**
     * Updates the provided MutableData with provided subscription diff.
     *
     * @param currentData an instance of mutable data of existing subscription state
     * @param diff        a diff between updated and existing subscription states
     */
    private static void updateWithDiff(MutableData currentData, FirebaseSubscriptionDiff diff) {
        diff.changed()
            .forEach(record -> currentData.child(record.key())
                                          .setValue(record.data()));
        diff.removed()
            .forEach(record -> currentData.child(record.key())
                                          .setValue(null));
        diff.added()
            .forEach(record -> currentData.child(newChildKey())
                                          .setValue(record.data()));
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
                       .parallelStream()
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
