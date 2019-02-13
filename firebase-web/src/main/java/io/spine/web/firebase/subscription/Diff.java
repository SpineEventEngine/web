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

import com.google.gson.JsonObject;
import io.spine.web.firebase.client.NodeValue;
import io.spine.web.firebase.subscription.DiffItems.AddedItem;
import io.spine.web.firebase.subscription.DiffItems.ChangedItem;
import io.spine.web.firebase.subscription.DiffItems.RemovedItem;
import io.spine.web.firebase.subscription.Entries.Entry;
import io.spine.web.firebase.subscription.Entries.ExistingEntry;
import io.spine.web.firebase.subscription.Entries.UpToDateEntry;

import java.util.List;

import static io.spine.web.firebase.subscription.Entries.Entry.Operation.ADD;
import static io.spine.web.firebase.subscription.Entries.Entry.Operation.CHANGE;
import static io.spine.web.firebase.subscription.Entries.Entry.Operation.REMOVE;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

/**
 * A diff of the Firebase storage state to an actual state of entities, 
 * used to execute updates on Firebase storage.
 */
final class Diff {

    private final List<AddedItem> added;
    private final List<ChangedItem> changed;
    private final List<RemovedItem> removed;

    private Diff(List<AddedItem> added,
                 List<ChangedItem> changed,
                 List<RemovedItem> removed) {
        this.added = unmodifiableList(added);
        this.changed = unmodifiableList(changed);
        this.removed = unmodifiableList(removed);
    }

    List<AddedItem> added() {
        return unmodifiableList(added);
    }

    List<ChangedItem> changed() {
        return unmodifiableList(changed);
    }

    List<RemovedItem> removed() {
        return unmodifiableList(removed);
    }

    /**
     * Compares the actual state represented by {@code newEntries} to the state of the Firebase
     * database represented by a {@link NodeValue}.
     *
     * @param newEntries
     *         a list of JSON serialized entries retrieved from Spine
     * @param currentData
     *         the current node data to match new data to
     * @return a diff between Spine and Firebase data states
     */
    static Diff
    computeDiff(List<String> newEntries, NodeValue currentData) {
        JsonObject jsonObject = currentData.underlyingJson();
        List<ExistingEntry> existingEntries = existingEntries(jsonObject);
        EntriesMatcher matcher =
                new EntriesMatcher(existingEntries);
        List<UpToDateEntry> entries = upToDateEntries(newEntries);
        List<Entry> entryUpdates = matcher.match(entries);
        return new Diff(entriesToAdd(entryUpdates),
                        entriesToChange(entryUpdates),
                        entriesToRemove(entryUpdates));
    }

    private static List<ExistingEntry> existingEntries(JsonObject object) {
        return object.entrySet()
                     .stream()
                     .map(ExistingEntry::fromJsonObjectEntry)
                     .collect(toList());
    }

    private static List<UpToDateEntry> upToDateEntries(List<String> newEntries) {
        return newEntries.stream()
                         .map(UpToDateEntry::new)
                         .collect(toList());
    }

    private static List<RemovedItem> entriesToRemove(List<Entry> entries) {
        return entries.stream()
                      .filter(entry -> entry.operation() == REMOVE)
                      .map(entry -> new RemovedItem(entry.key()))
                      .collect(toList());
    }

    private static List<ChangedItem> entriesToChange(List<Entry> entries) {
        return entries.stream()
                      .filter(entry -> entry.operation() == CHANGE)
                      .map(entry -> new ChangedItem(entry.key(), entry.data()))
                      .collect(toList());
    }

    private static List<AddedItem> entriesToAdd(List<Entry> entries) {
        return entries.stream()
                      .filter(entry -> entry.operation() == ADD)
                      .map(entry -> new AddedItem(entry.data()))
                      .collect(toList());
    }
}
