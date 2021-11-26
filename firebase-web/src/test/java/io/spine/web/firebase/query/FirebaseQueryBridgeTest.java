/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.common.collect.Iterators;
import com.google.gson.JsonElement;
import com.google.protobuf.Message;
import io.spine.client.QueryFactory;
import io.spine.json.Json;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.QueryService;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.web.firebase.NodePaths;
import io.spine.web.firebase.NodeValue;
import io.spine.web.firebase.StoredJson;
import io.spine.web.firebase.given.Book;
import io.spine.web.firebase.given.BookId;
import io.spine.web.firebase.given.MemoizingFirebase;
import io.spine.web.given.TestQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static io.spine.base.Identifier.newUuid;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`FirebaseQueryBridge` should")
class FirebaseQueryBridgeTest {

    private static final QueryFactory queryFactory =
            new TestActorRequestFactory(FirebaseQueryBridgeTest.class).query();

    private MemoizingFirebase firebaseClient;

    @BeforeEach
    void initClient() {
        firebaseClient = MemoizingFirebase.withNoLatency();
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "CheckReturnValue"}) // Method called to throw.
    @Test
    @DisplayName("require Query Service set in class Builder")
    void requireQueryService() {
        var builder = FirebaseQueryBridge.newBuilder()
                .setFirebaseClient(firebaseClient);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    @DisplayName("require Firebase Client set in class Builder")
    @SuppressWarnings({"ResultOfMethodCallIgnored", "CheckReturnValue"}) // Method called to throw.
    void requireFirebaseClient() {
        var queryService = QueryService.newBuilder()
                .add(BoundedContextBuilder.assumingTests().build())
                .build();
        var builder = FirebaseQueryBridge.newBuilder()
                .setQueryService(queryService);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    @DisplayName("produce a database path for the given query results")
    void testMediate() {
        var queryService = new TestQueryService();
        var bridge = FirebaseQueryBridge.newBuilder()
                .setQueryService(queryService)
                .setFirebaseClient(firebaseClient)
                .build();
        var query = queryFactory.all(Book.class);
        var response = bridge.send(query);
        assertThat(response)
             .isNotNull();
    }

    @Test
    @DisplayName("write query results to the database")
    void testWriteData() {
        var id = BookId.newBuilder()
                .setValue(newUuid())
                .build();
        var book = Book.newBuilder()
                .setId(id)
                .setName(newUuid())
                .build();
        var queryService = new TestQueryService(book);
        var bridge = FirebaseQueryBridge.newBuilder()
                .setQueryService(queryService)
                .setFirebaseClient(firebaseClient)
                .build();
        var query = queryFactory.all(Book.class);
        var response = bridge.send(query);
        var nodeValue = firebaseClient.valueFor(NodePaths.of(response.getPath()));
        var actual = firstFieldOf(nodeValue, Book.class);
        assertThat(actual)
                .isEqualTo(book);
    }

    /**
     * Returns the {@code message} stored in the first JSON element of the {@code nodeValue}.
     *
     * <p>The node has a single randomized field with the field value being a serialized
     * processed message.
     *
     * @implNote The {@code nodeValue} holds data as a JSON primitive string (i.e. an
     *         escaped string that actually holds a JSON object), thus we're forced to parse
     *         the string into a JSON object first and then convert it back to a string.
     */
    private static <T extends Message> T firstFieldOf(NodeValue nodeValue, Class<T> message) {
        var json = nodeValue.underlyingJson();
        var entries = json.entrySet();
        JsonElement value = Iterators
                .getOnlyElement(entries.iterator())
                .getValue();
        var messageJson = StoredJson
                .from(value.getAsString())
                .asJsonObject()
                .toString();
        return Json.fromJson(messageJson, message);
    }
}
