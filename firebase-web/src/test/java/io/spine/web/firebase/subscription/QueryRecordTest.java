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

import io.spine.client.QueryResponse;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.NodePaths;
import io.spine.web.firebase.NodeValue;
import io.spine.web.firebase.given.Book;
import io.spine.web.firebase.given.TestFirebaseClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.web.firebase.StoredJson.encode;
import static io.spine.web.firebase.given.FirebaseSubscriptionRecordTestEnv.Authors.gangOfFour;
import static io.spine.web.firebase.given.FirebaseSubscriptionRecordTestEnv.Books.aliceInWonderland;
import static io.spine.web.firebase.given.FirebaseSubscriptionRecordTestEnv.Books.designPatterns;
import static io.spine.web.firebase.given.FirebaseSubscriptionRecordTestEnv.Books.donQuixote;
import static io.spine.web.firebase.given.FirebaseSubscriptionRecordTestEnv.Books.guideToTheGalaxy;
import static io.spine.web.firebase.given.FirebaseSubscriptionRecordTestEnv.mockQueryResponse;
import static io.spine.web.firebase.given.FirebaseSubscriptionRecordTestEnv.updateAuthors;
import static java.time.Duration.ZERO;

@DisplayName("SubscriptionRecord should")
class QueryRecordTest {

    private final TestFirebaseClient firebaseClient = TestFirebaseClient.withSimulatedLatency(ZERO);

    @Test
    @DisplayName("store an initial subscription adding new entries")
    void storeInitial() {
        String dbPath = "subscription-create-db-path";

        Book aliceInWonderland = aliceInWonderland();
        Book donQuixote = donQuixote();
        QueryResponse queryResponse = mockQueryResponse(aliceInWonderland, donQuixote);

        NodePath queryResponsePath = NodePaths.of(dbPath);
        SubscriptionRecord record = new SubscriptionRecord(queryResponsePath, queryResponse);
        record.store(firebaseClient);
        assertThat(firebaseClient.writes()).contains(queryResponsePath);
    }

    @Test
    @DisplayName("store a subscription update")
    void storeUpdate() {
        // Kept untouched. Present in both DB and QueryResponse.
        Book aliceInWonderland = aliceInWonderland();
        // Added to the Firebase. Present only in QueryResponse.
        Book donQuixote = donQuixote();
        // Updated with new authors. Present in both DB and QueryResponse
        // with same ID but different data.
        Book designPatterns = designPatterns();
        Book designPatternsWithAuthors = updateAuthors(designPatterns, gangOfFour());
        // Removed from Firebase. Present in DB but not the QueryResponse.
        Book guideToTheGalaxy = guideToTheGalaxy();
        String dbPath = "subscription-update-db-path";

        QueryResponse queryResponse = mockQueryResponse(aliceInWonderland,
                                                        donQuixote,
                                                        designPatternsWithAuthors);
        NodePath queryResponsePath = NodePaths.of(dbPath);
        SubscriptionRecord record = new SubscriptionRecord(queryResponsePath,
                                                           queryResponse);
        NodeValue existingValue = NodeValue.empty();
        existingValue.addChild(encode(aliceInWonderland));
        existingValue.addChild(encode(designPatterns));
        existingValue.addChild(encode(guideToTheGalaxy));

        record.store(firebaseClient);

        assertThat(firebaseClient.writes()).contains(queryResponsePath);
    }

    @Test
    @DisplayName("store a subscription update even when no initial record is present")
    void storeUpdateWhenNoInitialPresent() {
        String dbPath = "subscription-update-no-initial";

        Book aliceInWonderland = aliceInWonderland();
        QueryResponse queryResponse = mockQueryResponse(aliceInWonderland);

        NodePath queryResponsePath = NodePaths.of(dbPath);
        SubscriptionRecord record = new SubscriptionRecord(queryResponsePath,
                                                           queryResponse);
        record.store(firebaseClient);

        assertThat(firebaseClient.writes()).contains(queryResponsePath);
    }
}
