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

package io.spine.web.firebase.query;

import io.spine.client.EntityStateWithVersion;
import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.json.Json;
import io.spine.protobuf.AnyPacker;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.NodeValue;

/**
 * A record which can be stored into a Firebase database.
 *
 * <p>A single record represents a {@linkplain QueryResponse response to a single query}.
 */
final class QueryRecord {

    private final NodePath path;
    private final QueryResponse queryResponse;

    QueryRecord(Query query, QueryResponse queryResponse) {
        this.path = QueryNodePath.of(query);
        this.queryResponse = queryResponse;
    }

    /**
     * Retrieves the database path to this record.
     */
    NodePath path() {
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
     * Flushes the array response of the query to the Firebase, adding array items to storage one
     * by one.
     *
     * <p>Suitable for big queries, spanning thousands and millions of items.
     */
    private void flushTo(FirebaseClient firebaseClient) {
        queryResponse.getMessageList()
                     .stream()
                     .unordered()
                     .map(EntityStateWithVersion::getState)
                     .map(AnyPacker::unpack)
                     .map(Json::toCompactJson)
                     .map(NodeValue::withSingleChild)
                     .forEach(node -> firebaseClient.merge(path, node));
    }
}
