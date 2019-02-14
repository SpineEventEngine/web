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

import java.util.List;

import static com.google.common.collect.ImmutableList.builder;

/**
 * Calculates a diff of the Firebase storage state to an actual state of entities, 
 * used to execute updates on Firebase storage.
 */
public final class DiffCalculator {

    private final List<ExistingEntry> existingEntries;

    private DiffCalculator(List<ExistingEntry> existingEntries) {
        this.existingEntries = existingEntries;
    }

    /**
     * Create a new {@code DiffCalculator} with state current in Firebase.
     *
     * @param currentData
     *         the current node data to match new data to
     */
    public static DiffCalculator from(NodeValue currentData) {
        JsonObject jsonObject = currentData.underlyingJson();
        List<ExistingEntry> existingEntries = ExistingEntry.fromJson(jsonObject);
        return new DiffCalculator(existingEntries);
    }

    /**
     * Compares the actual state represented by {@code newEntries} to the state of the Firebase
     * database represented by a {@link NodeValue}.
     *
     * @param newEntries
     *         a list of JSON serialized entries retrieved from Spine
     * @return a diff between Spine and Firebase data states
     */
    public Diff compareWith(List<String> newEntries) {
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
            switch (update.getOperation()) {
                case ADD:
                    added.add(AddedItem.newBuilder()
                                       .setData(update.getData())
                                       .build());
                    break;
                case REMOVE:
                    removed.add(RemovedItem.newBuilder()
                                           .setKey(update.getKey())
                                           .build());
                    break;
                case CHANGE:
                    changed.add(ChangedItem.newBuilder()
                                           .setKey(update.getKey())
                                           .setData(update.getData())
                                           .build());
                    break;
                case PASS:
                case UNRECOGNIZED:
                default:
                    break;
            }
        });
        return diff(added.build(), changed.build(), removed.build());
    }

    private static Diff diff(Iterable<AddedItem> added,
                             Iterable<ChangedItem> changed,
                             Iterable<RemovedItem> removed) {
        return Diff
                .newBuilder()
                .addAllAdded(added)
                .addAllChanged(changed)
                .addAllRemoved(removed)
                .build();
    }
}
