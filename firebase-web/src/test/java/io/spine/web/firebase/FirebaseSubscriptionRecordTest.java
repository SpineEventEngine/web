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

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import io.spine.client.QueryResponse;
import io.spine.web.firebase.given.Book;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionStage;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.json.Json.toCompactJson;
import static io.spine.web.firebase.FirebaseDatabasePath.fromString;
import static io.spine.web.firebase.given.FirebaseSubscriptionRecordTestEnv.Authors.gangOfFour;
import static io.spine.web.firebase.given.FirebaseSubscriptionRecordTestEnv.Books.aliceInWonderland;
import static io.spine.web.firebase.given.FirebaseSubscriptionRecordTestEnv.Books.designPatterns;
import static io.spine.web.firebase.given.FirebaseSubscriptionRecordTestEnv.Books.donQuixote;
import static io.spine.web.firebase.given.FirebaseSubscriptionRecordTestEnv.Books.guideToTheGalaxy;
import static io.spine.web.firebase.given.FirebaseSubscriptionRecordTestEnv.mockQueryResponse;
import static io.spine.web.firebase.given.FirebaseSubscriptionRecordTestEnv.mockTransactionalWrite;
import static io.spine.web.firebase.given.FirebaseSubscriptionRecordTestEnv.mutableBookData;
import static io.spine.web.firebase.given.FirebaseSubscriptionRecordTestEnv.updateAuthors;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Mykhailo Drachuk
 */
@DisplayName("FirebaseSubscriptionRecord should")
class FirebaseSubscriptionRecordTest {

    @Test
    @DisplayName("store an initial subscription adding new entries")
    void storeInitial() {
        String dbPath = "subscription-create-db-path";
        FirebaseDatabase db = mock(FirebaseDatabase.class);
        DatabaseReference ref = mock(DatabaseReference.class);
        when(db.getReference(dbPath)).thenReturn(ref);

        @SuppressWarnings("unchecked")
        CompletionStage<QueryResponse> queryResponse = mock(CompletionStage.class);
        Book aliceInWonderland = aliceInWonderland();
        Book donQuixote = donQuixote();
        mockQueryResponse(queryResponse, aliceInWonderland, donQuixote);

        MutableData mutableData = mock(MutableData.class);
        MutableData mutableItem = mock(MutableData.class);
        when(mutableData.child(anyString())).thenReturn(mutableItem);
        mockTransactionalWrite(ref, mutableData);

        FirebaseSubscriptionRecord record = new FirebaseSubscriptionRecord(fromString(dbPath),
                                                                           queryResponse);
        record.storeAsInitial(db);

        verify(mutableItem, times(2)).setValue(any());
        verify(mutableItem).setValue(toCompactJson(aliceInWonderland));
        verify(mutableItem).setValue(toCompactJson(donQuixote));
    }

    @Test
    @DisplayName("store a subscription update adding, changing, and removing entries")
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
        FirebaseDatabase db = mock(FirebaseDatabase.class);
        DatabaseReference ref = mock(DatabaseReference.class);
        when(db.getReference(dbPath)).thenReturn(ref);

        @SuppressWarnings("unchecked")
        CompletionStage<QueryResponse> queryResponse = mock(CompletionStage.class);
        mockQueryResponse(queryResponse, aliceInWonderland, donQuixote, designPatternsWithAuthors);

        MutableData mutableData = mock(MutableData.class);
        MutableData mutableItem = mock(MutableData.class);

        String guideToTheGalaxyKey = "guide";
        String patternsKey = "patterns";
        String aliceKey = "alice";

        Iterable<MutableData> mutableEntries =
                newArrayList(mutableBookData(guideToTheGalaxyKey, guideToTheGalaxy),
                             mutableBookData(patternsKey, designPatterns),
                             mutableBookData(aliceKey, aliceInWonderland));
        when(mutableData.getChildren()).thenReturn(mutableEntries);
        when(mutableData.child(anyString())).thenReturn(mutableItem);
        mockTransactionalWrite(ref, mutableData);

        FirebaseSubscriptionRecord record = new FirebaseSubscriptionRecord(fromString(dbPath),
                                                                           queryResponse);
        record.storeAsUpdate(db);

        verify(mutableItem, times(3)).setValue(any());
        verify(mutableData, times(3)).child(anyString());

        verify(mutableItem).setValue(toCompactJson(donQuixote));

        verify(mutableData).child(patternsKey);
        verify(mutableItem).setValue(toCompactJson(designPatternsWithAuthors));

        verify(mutableData).child(guideToTheGalaxyKey);
        verify(mutableItem).setValue(null);
    }
}
