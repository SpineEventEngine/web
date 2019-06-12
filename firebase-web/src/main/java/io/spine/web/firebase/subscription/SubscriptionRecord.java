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
import io.spine.client.EntityStateWithVersion;
import io.spine.client.QueryResponse;
import io.spine.json.Json;
import io.spine.protobuf.AnyPacker;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.NodeValue;
import io.spine.web.firebase.subscription.diff.Diff;
import io.spine.web.firebase.subscription.diff.DiffCalculator;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * A subscription record that gets stored into a Firebase database.
 *
 * <p>Supports both an initial store and consequent updates of the stored data.
 */
final class SubscriptionRecord {

    private final NodePath path;
    private final QueryResponse queryResponse;

    SubscriptionRecord(NodePath path, QueryResponse queryResponse) {
        this.path = path;
        this.queryResponse = queryResponse;
    }

    /**
     * Retrieves the database path to this record.
     */
    NodePath path() {
        return path;
    }

    /**
     * Writes this record to the Firebase database as initial data without checking what is
     * already stored in database at given location.
     */
    void storeAsInitial(FirebaseClient firebaseClient) {
        flushNewVia(firebaseClient);
    }

    /**
     * Flushes an array response of the query to the Firebase adding array items to storage in a
     * transaction.
     */
    private void flushNewVia(FirebaseClient firebaseClient) {
        flushEntries(mapMessagesToJson(), firebaseClient);
    }

    /**
     * Stores the data to the Firebase updating only the data that has changed.
     */
    void storeAsUpdate(FirebaseClient firebaseClient) {
        flushDiffVia(firebaseClient);
    }

    /**
     * Flushes an array response of the query to the Firebase, adding, removing, and updating items
     * already present in storage in a transaction.
     */
    private void flushDiffVia(FirebaseClient firebaseClient) {
        Optional<NodeValue> existingValue = firebaseClient.get(path);
        Stream<String> newEntries = mapMessagesToJson();
        if (existingValue.isPresent()) {
            DiffCalculator diffCalculator = DiffCalculator.from(existingValue.get());
            List<String> entriesList = newEntries.collect(toList());
            Diff diff = diffCalculator.compareWith(entriesList);
            updateWithDiff(diff, firebaseClient);
        } else {
            flushEntries(newEntries, firebaseClient);
        }
    }

    private void flushEntries(Stream<String> jsonEntries, FirebaseClient client) {
        NodeValue nodeValue = NodeValue.empty();
        jsonEntries.forEach(nodeValue::addChild);
        client.merge(path, nodeValue);
    }

    private void updateWithDiff(Diff diff, FirebaseClient firebaseClient) {
        NodeValue nodeValue = NodeValue.empty();
        diff.getChangedList()
            .forEach(record -> nodeValue.addChild(record.getKey(), record.getData()));
        diff.getRemovedList()
            .forEach(record -> nodeValue.addChild(record.getKey(), null));
        diff.getAddedList()
            .forEach(record -> nodeValue.addChild(record.getData()));
        firebaseClient.merge(path, nodeValue);
    }

    /**
     * Creates a stream of response messages mapping each response message to JSON.
     *
     * @param response
     *         response to an entity query
     * @return a stream of messages represented by JSON strings
     */
    @SuppressWarnings("RedundantTypeArguments") // AnyPacker::unpack type cannot be inferred.
    private Stream<String> mapMessagesToJson() {
        return queryResponse
                .getMessagesList()
                .stream()
                .unordered()
                .map(EntityStateWithVersion::getState)
                .map(AnyPacker::<Message>unpack)
                .map(Json::toCompactJson);
    }
}
