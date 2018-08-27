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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.MutableData;
import io.spine.web.firebase.FirebaseSubscriptionEntries.Entry;
import io.spine.web.firebase.FirebaseSubscriptionEntries.ExistingEntry;
import io.spine.web.firebase.FirebaseSubscriptionEntries.UpToDateEntry;
import io.spine.web.firebase.FirebaseSubscriptionRecords.AddedRecord;
import io.spine.web.firebase.FirebaseSubscriptionRecords.ChangedRecord;
import io.spine.web.firebase.FirebaseSubscriptionRecords.RemovedRecord;

import java.util.List;
import java.util.stream.StreamSupport;

import static io.spine.web.firebase.FirebaseSubscriptionEntries.Entry.Operation.ADD;
import static io.spine.web.firebase.FirebaseSubscriptionEntries.Entry.Operation.CHANGE;
import static io.spine.web.firebase.FirebaseSubscriptionEntries.Entry.Operation.REMOVE;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

/**
 * A diff of the Firebase storage state to an actual state of entities, used to execute updates on
 * Firebase storage.
 *
 * @author Mykhailo Drachuk
 */
class FirebaseSubscriptionDiff {

    private final List<AddedRecord> added;
    private final List<ChangedRecord> changed;
    private final List<RemovedRecord> removed;

    private FirebaseSubscriptionDiff(List<AddedRecord> added,
                                     List<ChangedRecord> changed,
                                     List<RemovedRecord> removed) {
        this.added = unmodifiableList(added);
        this.changed = unmodifiableList(changed);
        this.removed = unmodifiableList(removed);
    }

    List<AddedRecord> added() {
        return added;
    }

    List<ChangedRecord> changed() {
        return changed;
    }

    List<RemovedRecord> removed() {
        return removed;
    }

    /**
     * Compares the actual state represented by {@code newEntries} to the state of the Firebase
     * database represented by a a list of {@link DataSnapshot data snapshots}.
     *
     * @param newEntries      a list of JSON serialized entries retrieved from Spine
     * @param firebaseEntries a list of Firebase {@code DataSnapshot}s to match new data to
     * @return a diff between Spine and Firebase data states
     */
    static FirebaseSubscriptionDiff computeDiff(List<String> newEntries,
                                                Iterable<MutableData> firebaseEntries) {
        List<ExistingEntry> existingEntries = existingEntries(firebaseEntries);
        FirebaseSubscriptionEntriesMatcher matcher = 
                new FirebaseSubscriptionEntriesMatcher(existingEntries);
        List<Entry> entryUpdates = matcher.match(upToDateEntries(newEntries));
        return new FirebaseSubscriptionDiff(entriesToAdd(entryUpdates),
                                            entriesToChange(entryUpdates),
                                            entriesToRemove(entryUpdates));
    }

    private static List<ExistingEntry> existingEntries(Iterable<MutableData> entries) {
        return StreamSupport.stream(entries.spliterator(), true)
                            .map(ExistingEntry::fromFirebaseData)
                            .collect(toList());
    }

    private static List<UpToDateEntry> upToDateEntries(List<String> newEntries) {
        return newEntries.stream()
                         .map(UpToDateEntry::new)
                         .collect(toList());
    }

    private static List<RemovedRecord> entriesToRemove(List<Entry> entries) {
        return entries.stream()
                      .filter(entry -> entry.operation() == REMOVE)
                      .map(entry -> new RemovedRecord(entry.key()))
                      .collect(toList());
    }

    private static List<ChangedRecord> entriesToChange(List<Entry> entries) {
        return entries.stream()
                      .filter(entry -> entry.operation() == CHANGE)
                      .map(entry -> new ChangedRecord(entry.key(), entry.data()))
                      .collect(toList());
    }

    private static List<AddedRecord> entriesToAdd(List<Entry> entries) {
        return entries.stream()
                      .filter(entry -> entry.operation() == ADD)
                      .map(entry -> new AddedRecord(entry.data()))
                      .collect(toList());
    }
}
