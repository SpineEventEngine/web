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

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.client.EntityStateWithVersion;
import io.spine.client.QueryResponse;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.NodeValue;
import io.spine.web.firebase.StoredJson;
import io.spine.web.firebase.subscription.diff.Diff;
import io.spine.web.firebase.subscription.diff.DiffCalculator;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.google.appengine.repackaged.com.google.gson.internal.$Gson$Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.toList;

/**
 * A subscription record that gets stored into a Firebase database.
 *
 * <p>Supports both an initial store and consequent updates of the stored data.
 */
final class SubscriptionRecord {

    private final NodePath path;
    private final ImmutableList<? extends Message> messages;

    SubscriptionRecord(NodePath path, QueryResponse queryResponse) {
        this(path, fromQueryResponse(queryResponse));
    }

    private static ImmutableList<Any> fromQueryResponse(QueryResponse queryResponse) {
        return queryResponse
                .getMessageList()
                .stream()
                .map(EntityStateWithVersion::getState)
                .collect(toImmutableList());
    }

    SubscriptionRecord(NodePath path, Collection<? extends Message> messages) {
        this.path = checkNotNull(path);
        this.messages = ImmutableList.copyOf(messages);
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
        flushEntries(mapMessagesToJson(), firebaseClient);
    }

    /**
     * Stores the data to the Firebase updating only the data that has changed.
     */
    void storeAsUpdate(FirebaseClient firebaseClient) {
        List<StoredJson> newEntries = mapMessagesToJson();

        if (DiffCalculator.canCalculateEfficientlyFor(newEntries)) {
            Optional<NodeValue> existingValue = firebaseClient.get(path);
            if (existingValue.isPresent()) {
                DiffCalculator diffCalculator = DiffCalculator.from(existingValue.get());
                Diff diff = diffCalculator.compareWith(newEntries);
                updateWithDiff(diff, firebaseClient);
            } else {
                storeAsInitial(firebaseClient);
            }
        } else {
            storeAsInitial(firebaseClient);
        }
    }

    private void flushEntries(Iterable<StoredJson> jsonEntries, FirebaseClient client) {
        NodeValue nodeValue = NodeValue.withChildren(jsonEntries);
        client.create(path, nodeValue);
    }

    private void updateWithDiff(Diff diff, FirebaseClient firebaseClient) {
        NodeValue nodeValue = NodeValue.empty();
        diff.getChangedList()
            .forEach(record -> nodeValue.addChild(record.getKey(), StoredJson.from(record.getData())));
        diff.getRemovedList()
            .forEach(record -> nodeValue.addNullChild(record.getKey()));
        diff.getAddedList()
            .forEach(record -> nodeValue.addChild(StoredJson.from(record.getData())));
        firebaseClient.update(path, nodeValue);
    }

    /**
     * Creates a stream of response messages mapping each response message to JSON.
     *
     * @return a stream of messages represented by JSON strings
     */
    private List<StoredJson> mapMessagesToJson() {
        return messages
                .stream()
                .map(StoredJson::encode)
                .collect(toList());
    }
}
