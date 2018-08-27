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

import com.google.api.core.ApiFuture;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.protobuf.Message;
import io.spine.client.QueryResponse;
import io.spine.json.Json;
import io.spine.protobuf.AnyPacker;
import io.spine.web.firebase.FirebaseSubscriptionRecords.AddedRecord;
import io.spine.web.firebase.FirebaseSubscriptionRecords.ChangedRecord;
import io.spine.web.firebase.FirebaseSubscriptionRecords.RemovedRecord;
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
                response -> mapMessagesToJson(response)
                        .map(json -> addRecord(reference, new AddedRecord(json)))
                        .forEach(this::mute)
        );
    }

    /**
     * Stores the data to the Firebase, updating only the data that has changed.
     */
    void storeAsUpdate(FirebaseDatabase database) {
        DatabaseReference reference = path().reference(database);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                QueryResponseConsumer consumer = new QueryResponseConsumer();
                queryResponse.thenAccept(consumer);
                List<String> newEntries = consumer.jsonMessages();
                FirebaseSubscriptionDiff diff = computeDiff(newEntries, snapshot.getChildren());
                flushTo(reference, diff);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                log().error("An error retrieving values from the Firebase database.");
            }
        });
    }

    /**
     * Updates the database reference using the retrieved diff. Adds, updates and removes the
     * values from the database.
     *
     * @param reference a Firebase reference to the location of data in database
     * @param diff      a diff between current Firebase storage state and data actual at current moment
     */
    private void flushTo(DatabaseReference reference, FirebaseSubscriptionDiff diff) {
        diff.added()
            .parallelStream()
            .map(record -> addRecord(reference, record))
            .forEach(this::mute);
        diff.removed()
            .parallelStream()
            .map(record -> removeRecord(reference, record))
            .forEach(this::mute);
        diff.changed()
            .parallelStream()
            .map(record -> update(reference, record))
            .forEach(this::mute);
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
     * Stores a new record to the database asynchronously.
     */
    private static ApiFuture<Void> addRecord(DatabaseReference reference, AddedRecord record) {
        return reference.push()
                        .setValueAsync(record.data());
    }

    /**
     * Removes a record from the database asynchronously.
     */
    private static ApiFuture<Void> removeRecord(DatabaseReference reference, RemovedRecord record) {
        return reference.child(record.key())
                        .removeValueAsync();
    }

    /**
     * Updates the record in the database asynchronously.
     */
    private static ApiFuture<Void> update(DatabaseReference reference, ChangedRecord record) {
        return reference.child(record.key())
                        .setValueAsync(record.data());
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
     * Awaits the given {@link Future} and catches all the exceptions.
     *
     * <p>The encountered exceptions are logged and never thrown.
     */
    private void mute(Future<?> future) {
        try {
            future.get(writeAwaitSeconds, SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log().error(e.getMessage());
        }
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
