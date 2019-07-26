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

import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.client.Query;
import io.spine.client.QueryFactory;
import io.spine.server.QueryService;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.given.TestQueryService;
import io.spine.web.firebase.subscription.given.HasChildren;
import io.spine.web.query.QueryProcessingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.spine.json.Json.toCompactJson;
import static io.spine.web.firebase.subscription.given.HasChildren.anyKey;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("FirebaseQueryBridge should")
class FirebaseQueryBridgeTest {

    private static final QueryFactory queryFactory =
            new TestActorRequestFactory(FirebaseQueryBridgeTest.class).query();

    private FirebaseClient firebaseClient;

    @BeforeEach
    void setUp() {
        firebaseClient = mock(FirebaseClient.class);
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "CheckReturnValue"}) // Method called to throw.
    @Test
    @DisplayName("require Query Service set in class Builder")
    void requireQueryService() {
        FirebaseQueryBridge.Builder builder = FirebaseQueryBridge
                .newBuilder()
                .setFirebaseClient(mock(FirebaseClient.class));
        assertThrows(IllegalStateException.class, builder::build);
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "CheckReturnValue"}) // Method called to throw.
    @Test
    @DisplayName("require Firebase Client set in class Builder")
    void requireFirebaseClient() {
        FirebaseQueryBridge.Builder builder = FirebaseQueryBridge
                .newBuilder()
                .setQueryService(mock(QueryService.class));
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    @DisplayName("produce a database path for the given query results")
    void testMediate() {
        TestQueryService queryService = new TestQueryService();
        FirebaseQueryBridge bridge = FirebaseQueryBridge.newBuilder()
                                                        .setQueryService(queryService)
                                                        .setFirebaseClient(firebaseClient)
                                                        .build();
        Query query = queryFactory.all(Empty.class);
        QueryProcessingResult result = bridge.send(query);

        assertThat(result, instanceOf(QueryResult.class));
    }

    @Test
    @DisplayName("write query results to the database")
    void testWriteData() {
        Message dataElement = Time.currentTime();
        TestQueryService queryService = new TestQueryService(dataElement);
        FirebaseQueryBridge bridge = FirebaseQueryBridge.newBuilder()
                                                        .setQueryService(queryService)
                                                        .setFirebaseClient(firebaseClient)
                                                        .build();
        Query query = queryFactory.all(Timestamp.class);
        @SuppressWarnings("unused")
        QueryProcessingResult ignored = bridge.send(query);

        Map<String, String> expected = new HashMap<>();
        expected.put(anyKey(), toCompactJson(dataElement));
        verify(firebaseClient).create(any(), argThat(new HasChildren(expected)));
    }
}
