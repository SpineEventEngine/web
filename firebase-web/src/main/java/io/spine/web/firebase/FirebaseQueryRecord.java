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

import com.google.protobuf.Message;
import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.json.Json;
import io.spine.protobuf.AnyPacker;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * A record which can be stored into a Firebase database.
 *
 * <p>A single record represents a {@linkplain QueryResponse response to a single query}.
 */
final class FirebaseQueryRecord {

    private final FirebaseDatabasePath path;
    private final CompletionStage<QueryResponse> queryResponse;

    FirebaseQueryRecord(Query query, CompletionStage<QueryResponse> queryResponse) {
        this.path = FirebaseDatabasePath.allocateForQuery(query);
        this.queryResponse = queryResponse;
    }

    /**
     * Retrieves the database path to this record.
     */
    FirebaseDatabasePath path() {
        return path;
    }

    /**
     * Writes this record to the given Firebase database.
     *
     * @see FirebaseQueryBridge FirebaseQueryBridge for the detailed storage protocol
     */
    void storeVia(FirebaseClient firebaseClient) {
        flushTo(firebaseClient);
    }

    /**
     * Writes this record to the Firebase database in a single transaction
     * (i.e. in a single batch).
     */
    void storeTransactionallyVia(FirebaseClient firebaseClient) {
        flushTransactionally(firebaseClient);
    }

    /**
     * Synchronously retrieves a count of records that will be supplied to the client.
     *
     * @return an integer number of records
     */
    long getCount() {
        CountConsumer countConsumer = new CountConsumer();
        queryResponse.thenAccept(countConsumer);
        return countConsumer.getValue();
    }

    /**
     * A consumer that counts the number of messages in {@link QueryResponse Query Response}.
     */
    private static class CountConsumer implements Consumer<QueryResponse> {

        private long value;

        @Override
        public void accept(QueryResponse response) {
            this.value = response.getMessagesCount();
        }

        /**
         * Returns the count of messages in the consumed response.
         */
        public long getValue() {
            return value;
        }
    }

    /**
     * Flushes the array response of the query to the Firebase, adding array items to storage one
     * by one.
     *
     * <p>Suitable for big queries, spanning thousands and millions of items.
     */
    private void flushTo(FirebaseClient firebaseClient) {
        queryResponse.thenAccept(
                response -> mapMessagesToJson(response)
                        .forEach(json -> {
                            FirebaseNodeValue value = FirebaseNodeValue.withSingleChild(json);
                            firebaseClient.addValue(path(), value);
                        })
        );
    }

    /**
     * Flushes the array response of the query to the Firebase in one go.
     */
    private void flushTransactionally(FirebaseClient firebaseClient) {
        queryResponse.thenAccept(
                response -> {
                    List<String> jsonItems = mapMessagesToJson(response).collect(toList());
                    jsonItems.forEach(item -> {
                        FirebaseNodeValue value = FirebaseNodeValue.withSingleChild(item);
                        firebaseClient.addValue(path(), value);
                    });
                }
        );
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
}
