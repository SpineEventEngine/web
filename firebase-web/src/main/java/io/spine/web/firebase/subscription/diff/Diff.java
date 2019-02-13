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

package io.spine.web.firebase.subscription.diff;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import io.spine.web.firebase.client.NodeValue;
import io.spine.web.firebase.subscription.diff.DiffItems.AddedItem;
import io.spine.web.firebase.subscription.diff.DiffItems.ChangedItem;
import io.spine.web.firebase.subscription.diff.DiffItems.RemovedItem;

import java.util.List;

import static com.google.common.collect.ImmutableList.builder;
import static java.util.Collections.unmodifiableList;

/**
 * A diff of the Firebase storage state to an actual state of entities, 
 * used to execute updates on Firebase storage.
 */
public final class Diff {

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

    public List<AddedItem> added() {
        return added;
    }

    public List<ChangedItem> changed() {
        return changed;
    }

    public List<RemovedItem> removed() {
        return removed;
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
    public static Diff computeDiff(List<String> newEntries, NodeValue currentData) {
        JsonObject jsonObject = currentData.underlyingJson();
        List<ExistingEntry> existingEntries = ExistingEntry.fromJson(jsonObject);
        List<UpToDateEntry> entries = UpToDateEntry.parse(newEntries);
        EntriesMatcher matcher = new EntriesMatcher(existingEntries);
        List<EntryUpdate> updates = matcher.match(entries);
        return toDiff(updates);
    }

    private static Diff toDiff(List<EntryUpdate> updates) {
        ImmutableList.Builder<AddedItem> added = builder();
        ImmutableList.Builder<ChangedItem> changed = builder();
        ImmutableList.Builder<RemovedItem> removed = builder();
        updates.forEach(update -> {
            switch (update.operation()) {
                case ADD:
                    added.add(new AddedItem(update.data()));
                    break;
                case REMOVE:
                    removed.add(new RemovedItem(update.key()));
                    break;
                case CHANGE:
                    changed.add(new ChangedItem(update.key(), update.data()));
                    break;
                case PASS:
                    break;
            }
        });
        return new Diff(added.build(),
                        changed.build(),
                        removed.build());
    }
}
