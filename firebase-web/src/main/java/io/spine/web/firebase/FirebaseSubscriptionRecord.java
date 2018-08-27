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
import com.google.firebase.database.core.Repo;
import com.google.protobuf.Message;
import io.spine.client.QueryResponse;
import io.spine.json.Json;
import io.spine.protobuf.AnyPacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.firebase.database.Transaction.success;
import static com.google.firebase.database.utilities.PushIdGenerator.generatePushChildName;
import static io.spine.web.firebase.FirebaseSubscriptionDiff.computeDiff;
import static java.util.Collections.unmodifiableList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

/**
 * A subscription record that gets stored into a {@link FirebaseDatabase}.
 *
 * <p>Supports both an initial store and consequent updates of the stored data.
 *
 * @author Mykhailo Drachuk
 */
final class FirebaseSubscriptionRecord {

    private final FirebaseDatabasePath path;
    private final CompletionStage<QueryResponse> queryResponse;
    private final long writeAwaitSeconds;

    FirebaseSubscriptionRecord(FirebaseDatabasePath path,
                               CompletionStage<QueryResponse> queryResponse,
                               long writeAwaitSeconds) {
        this.path = path;
        this.queryResponse = queryResponse;
        this.writeAwaitSeconds = writeAwaitSeconds;
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
        flushTo(reference);
    }

    /**
     * Flushes an array response of the query to the Firebase asynchronously,
     * adding array items to storage one-by-one.
     */
    private void flushTo(DatabaseReference reference) {
        queryResponse.thenAcceptAsync(
                response -> {
                    List<String> newEntries = mapMessagesToJson(response).collect(toList());
                    reference.runTransaction(new Transaction.Handler() {
                        @Override
                        public Transaction.Result doTransaction(MutableData currentData) {
                            newEntries.forEach(record -> {
                                Repo repo = reference.getRepo();
                                String childName = generatePushChildName(repo.getServerTime());
                                currentData.child(childName)
                                           .setValue(record);
                            });
                            return success(currentData);
                        }

                        @Override
                        public void onComplete(DatabaseError error, boolean committed,
                                               DataSnapshot currentData) {
                            if (!committed) {
                                log().error("Subscription initial state was not committed to the Firebase.");
                                if (error != null) {
                                    log().error(error.getMessage());
                                }
                            }
                        }
                    });

                }
        );
    }

    /**
     * Stores the data to the Firebase, updating only the data that has changed.
     */
    void storeAsUpdate(FirebaseDatabase database) {
        DatabaseReference reference = path().reference(database);
        QueryResponseConsumer consumer = new QueryResponseConsumer();
        queryResponse.thenAcceptAsync(response -> {
            consumer.accept(response);
            List<String> newEntries = consumer.jsonMessages();
            reference.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData currentData) {
                    FirebaseSubscriptionDiff diff = computeDiff(newEntries, currentData.getChildren());
                    
                    diff.changed()
                        .forEach(record -> currentData.child(record.key())
                                                      .setValue(record.data()));
                    diff.removed()
                        .forEach(record -> currentData.child(record.key())
                                                      .setValue(null));
                    Repo repo = reference.getRepo();
                    diff.added()
                        .forEach(record -> {
                            currentData.child(generatePushChildName(repo.getServerTime()))
                                       .setValue(record.data());
                        });
                    return success(currentData);
                }

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
            });
        });
    }

    /**
     * A consumer of a {@link QueryResponse} from which a list of JSON serialized messages
     * can be retrieved.
     */
    private static class QueryResponseConsumer implements Consumer<QueryResponse> {

        private final List<String> jsonMessages;

        private QueryResponseConsumer() {
            this.jsonMessages = newArrayList();
        }

        @Override
        public void accept(QueryResponse response) {
            this.jsonMessages.addAll(mapMessagesToJson(response).collect(toList()));
        }

        /**
         * A list of messages which were retrieved from the consumed {@link QueryResponse response}.
         *
         * @return a list of messages serialized to JSON
         */
        List<String> jsonMessages() {
            return unmodifiableList(jsonMessages);
        }
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

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(FirebaseSubscriptionRecord.class);
    }
}
