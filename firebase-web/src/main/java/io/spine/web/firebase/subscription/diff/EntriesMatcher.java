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

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.web.firebase.subscription.diff.EntryUpdates.addEntry;
import static io.spine.web.firebase.subscription.diff.EntryUpdates.changeEntry;
import static io.spine.web.firebase.subscription.diff.EntryUpdates.passEntry;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

/**
 * A matcher of the up-to-date subscription state to the one stored in one Firebase database.
 */
final class EntriesMatcher {

    private final List<ExistingEntry> unmatchedEntries;

    EntriesMatcher(List<ExistingEntry> entries) {
        this.unmatchedEntries = new ArrayList<>(entries);
    }

    /**
     * Matches up-to-date entries retrieved from Spine to the entries from the Firebase storage.
     * Each {@linkplain UpToDateEntry up-to-date entry} is mapped to an {@linkplain EntryUpdate 
     * entry} specifying {@linkplain EntryUpdate.Operation operation} to be formed, along with 
     * data for this operation.
     *
     * @param entries a list of new entries with latest subscription state
     * @return a list of entries with operation appropriate to them
     */
    List<EntryUpdate> match(List<UpToDateEntry> entries) {
        List<EntryUpdate> matched = entries.stream()
                                           .map(this::match)
                                           .collect(toList());
        return concat(matched.stream(), unmatched().stream()).collect(toList());
    }

    private EntryUpdate match(UpToDateEntry entry) {
        return entry.containsId() ? matchById(entry) : shallowMatch(entry);
    }

    private EntryUpdate matchById(UpToDateEntry entry) {
        Optional<ExistingEntry> optionalMatchingEntry =
                unmatchedEntries.stream()
                                .filter(existing -> existing.idEquals(checkNotNull(entry.id())))
                                .findFirst();
        if (optionalMatchingEntry.isPresent()) {
            ExistingEntry matchingEntry = optionalMatchingEntry.get();
            unmatchedEntries.remove(matchingEntry);
            JsonNode matchingJson = matchingEntry.json();
            if (matchingJson.equals(entry.json())) {
                return passEntry(entry, matchingEntry);
            } else {
                return changeEntry(entry, matchingEntry);
            }
        } else {
            return addEntry(entry);
        }
    }

    private EntryUpdate shallowMatch(UpToDateEntry entry) {
        Optional<ExistingEntry> optionalMatchingEntry =
                unmatchedEntries.stream()
                                .filter(existing -> entry.json().equals(existing.json()))
                                .findFirst();
        if (optionalMatchingEntry.isPresent()) {
            ExistingEntry matchingEntry = optionalMatchingEntry.get();
            unmatchedEntries.remove(matchingEntry);
            return passEntry(entry, matchingEntry);
        }
        return addEntry(entry);
    }

    private List<EntryUpdate> unmatched() {
        return this.unmatchedEntries
                .stream()
                .map(EntryUpdates::removeEntry)
                .collect(toList());
    }
}
