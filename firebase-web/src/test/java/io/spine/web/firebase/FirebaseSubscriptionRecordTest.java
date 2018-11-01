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

import io.spine.client.QueryResponse;
import io.spine.web.firebase.given.Book;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionStage;

import static io.spine.web.firebase.FirebaseDatabasePath.fromString;
import static io.spine.web.firebase.given.FirebaseSubscriptionRecordTestEnv.Authors.gangOfFour;
import static io.spine.web.firebase.given.FirebaseSubscriptionRecordTestEnv.Books.aliceInWonderland;
import static io.spine.web.firebase.given.FirebaseSubscriptionRecordTestEnv.Books.designPatterns;
import static io.spine.web.firebase.given.FirebaseSubscriptionRecordTestEnv.Books.donQuixote;
import static io.spine.web.firebase.given.FirebaseSubscriptionRecordTestEnv.Books.guideToTheGalaxy;
import static io.spine.web.firebase.given.FirebaseSubscriptionRecordTestEnv.mockQueryResponse;
import static io.spine.web.firebase.given.FirebaseSubscriptionRecordTestEnv.updateAuthors;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("FirebaseSubscriptionRecord should")
class FirebaseSubscriptionRecordTest {

    private final FirebaseClient firebaseClient = mock(FirebaseClient.class);

    @Test
    @DisplayName("store an initial subscription adding new entries")
    void storeInitial() {
        String dbPath = "subscription-create-db-path";

        @SuppressWarnings("unchecked")
        CompletionStage<QueryResponse> queryResponse = mock(CompletionStage.class);
        Book aliceInWonderland = aliceInWonderland();
        Book donQuixote = donQuixote();
        mockQueryResponse(queryResponse, aliceInWonderland, donQuixote);

        FirebaseSubscriptionRecord record = new FirebaseSubscriptionRecord(fromString(dbPath),
                                                                           queryResponse);
        record.storeAsInitial(firebaseClient);

        verify(firebaseClient).addContent(any(), any());
    }

    @Test
    @DisplayName("store a subscription update")
    void storeUpdate() {
        Book aliceInWonderland = aliceInWonderland();
        Book donQuixote = donQuixote();
        Book designPatterns = designPatterns();
        Book designPatternsWithAuthors = updateAuthors(designPatterns, gangOfFour());
        Book guideToTheGalaxy = guideToTheGalaxy();

        String dbPath = "subscription-update-db-path";

        @SuppressWarnings("unchecked")
        CompletionStage<QueryResponse> queryResponse = mock(CompletionStage.class);
        mockQueryResponse(queryResponse, aliceInWonderland, donQuixote, designPatternsWithAuthors,
                          guideToTheGalaxy);

        FirebaseSubscriptionRecord record = new FirebaseSubscriptionRecord(fromString(dbPath),
                                                                           queryResponse);
        record.storeAsUpdate(firebaseClient);

        verify(firebaseClient).addContent(any(), any());
    }

    @Test
    @DisplayName("store continuous updates for a single entity")
    void storeContinuousUpdates() {
        Book aliceInWonderland = aliceInWonderland();
        String dbPath = "subscription-continuous-db-updates-path";
        @SuppressWarnings("unchecked")
        CompletionStage<QueryResponse> queryResponse = mock(CompletionStage.class);
        mockQueryResponse(queryResponse, aliceInWonderland);

        FirebaseSubscriptionRecord record = new FirebaseSubscriptionRecord(fromString(dbPath),
                                                                           queryResponse);

        record.storeAsUpdate(firebaseClient);
        record.storeAsUpdate(firebaseClient);
        verify(firebaseClient, times(2)).addContent(any(), any());
    }
}
