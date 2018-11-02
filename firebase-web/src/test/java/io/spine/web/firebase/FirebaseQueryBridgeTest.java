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

import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.client.Query;
import io.spine.client.QueryFactory;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.web.firebase.given.FirebaseQueryMediatorTestEnv.TestQueryService;
import io.spine.web.query.QueryProcessingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.spine.json.Json.toCompactJson;
import static io.spine.web.firebase.HasChildren.ANY_KEY;
import static io.spine.web.firebase.given.FirebaseQueryBridgeTestEnv.nonTransactionalQuery;
import static io.spine.web.firebase.given.FirebaseQueryBridgeTestEnv.transactionalQuery;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings("ConstantConditions") // Passing `null` to mocked methods.
@DisplayName("FirebaseQueryBridge should")
class FirebaseQueryBridgeTest {

    private static final String EMPTY_JSON = "{}";

    private static final QueryFactory queryFactory =
            TestActorRequestFactory.newInstance(FirebaseQueryBridgeTest.class)
                                   .query();

    private FirebaseClient firebaseClient;

    @BeforeEach
    void setUp() {
        firebaseClient = mock(FirebaseClient.class);
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
        QueryProcessingResult result = bridge.send(nonTransactionalQuery(query));

        assertThat(result, instanceOf(FirebaseQueryProcessingResult.class));
    }

    @Test
    @DisplayName("write query results to the database")
    void testWriteData() {
        Message dataElement = Time.getCurrentTime();
        TestQueryService queryService = new TestQueryService(dataElement);
        FirebaseQueryBridge bridge = FirebaseQueryBridge.newBuilder()
                                                        .setQueryService(queryService)
                                                        .setFirebaseClient(firebaseClient)
                                                        .build();
        Query query = queryFactory.all(Timestamp.class);
        @SuppressWarnings("unused")
        QueryProcessingResult ignored = bridge.send(nonTransactionalQuery(query));

        Map<String, String> expected = new HashMap<>();
        expected.put(ANY_KEY, toCompactJson(dataElement));
        verify(firebaseClient).addValue(any(), argThat(new HasChildren(expected)));
    }

    @Test
    @DisplayName("use transactional store call")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void testTransactionalQuery() {
        TestQueryService queryService = new TestQueryService(Empty.getDefaultInstance());
        FirebaseQueryBridge bridge = FirebaseQueryBridge.newBuilder()
                                                        .setQueryService(queryService)
                                                        .setFirebaseClient(firebaseClient)
                                                        .build();
        bridge.send(transactionalQuery(queryFactory.all(Empty.class)));

        Map<String, String> expected = new HashMap<>();
        expected.put(ANY_KEY, EMPTY_JSON);
        verify(firebaseClient).addValue(any(), argThat(new HasChildren(expected)));
    }

    @Test
    @DisplayName("use non-transactional store call")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void testNonTransactionalQuery() {
        TestQueryService queryService = new TestQueryService(Empty.getDefaultInstance());
        FirebaseQueryBridge bridge = FirebaseQueryBridge.newBuilder()
                                                        .setQueryService(queryService)
                                                        .setFirebaseClient(firebaseClient)
                                                        .build();
        bridge.send(nonTransactionalQuery(queryFactory.all(Empty.class)));

        Map<String, String> expected = new HashMap<>();
        expected.put(ANY_KEY, EMPTY_JSON);
        verify(firebaseClient).addValue(any(), argThat(new HasChildren(expected)));
    }
}
