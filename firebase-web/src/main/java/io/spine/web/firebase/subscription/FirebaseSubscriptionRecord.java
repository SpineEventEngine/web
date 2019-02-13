/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.web.firebase.subscription;

import com.google.protobuf.Message;
import io.spine.client.QueryResponse;
import io.spine.json.Json;
import io.spine.protobuf.AnyPacker;
import io.spine.web.firebase.client.FirebaseClient;
import io.spine.web.firebase.client.FirebaseDatabasePath;
import io.spine.web.firebase.client.FirebaseNodeValue;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import static io.spine.web.firebase.subscription.FirebaseSubscriptionDiff.computeDiff;
import static java.util.stream.Collectors.toList;

/**
 * A subscription record that gets stored into a Firebase database.
 *
 * <p>Supports both an initial store and consequent updates of the stored data.
 */
final class FirebaseSubscriptionRecord {

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
     * Writes this record to the Firebase database as initial data, without checking what is
     * already stored in database at given location.
     */
    void storeAsInitial(FirebaseClient firebaseClient) {
        flushNewVia(firebaseClient);
    }

    /**
     * Flushes an array response of the query to the Firebase, adding array items to storage in a
     * transaction.
     */
    private void flushNewVia(FirebaseClient firebaseClient) {
        queryResponse.thenAccept(response -> {
            List<String> newEntries = mapMessagesToJson(response).collect(toList());
            FirebaseNodeValue nodeValue = FirebaseNodeValue.empty();
            newEntries.forEach(nodeValue::addChild);
            firebaseClient.merge(path(), nodeValue);
        });
    }

    /**
     * Stores the data to the Firebase, updating only the data that has changed.
     */
    void storeAsUpdate(FirebaseClient firebaseClient) {
        flushDiffVia(firebaseClient);
    }

    /**
     * Flushes an array response of the query to the Firebase, adding, removing and updating items
     * already present in storage in a transaction.
     */
    private void flushDiffVia(FirebaseClient firebaseClient) {
        queryResponse.thenAccept(response -> {
            List<String> newEntries = mapMessagesToJson(response).collect(toList());
            Optional<FirebaseNodeValue> existingValue = firebaseClient.get(path());
            if (!existingValue.isPresent()) {
                FirebaseNodeValue nodeValue = FirebaseNodeValue.empty();
                newEntries.forEach(nodeValue::addChild);
                firebaseClient.merge(path(), nodeValue);
            } else {
                FirebaseSubscriptionDiff diff = computeDiff(newEntries, existingValue.get());
                updateWithDiff(diff, firebaseClient);
            }
        });
    }

    @SuppressWarnings("DuplicateStringLiteralInspection")
    private void updateWithDiff(FirebaseSubscriptionDiff diff, FirebaseClient firebaseClient) {
        FirebaseNodeValue nodeValue = FirebaseNodeValue.empty();
        diff.changed()
            .forEach(record -> nodeValue.addChild(record.key(), record.data()));
        diff.removed()
            .forEach(record -> nodeValue.addChild(record.key(), "null"));
        diff.added()
            .forEach(record -> nodeValue.addChild(record.data()));
        firebaseClient.merge(path(), nodeValue);
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
