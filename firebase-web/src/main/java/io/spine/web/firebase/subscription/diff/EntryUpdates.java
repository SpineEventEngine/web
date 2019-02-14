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

import static io.spine.web.firebase.subscription.diff.EntryUpdate.Operation.ADD;
import static io.spine.web.firebase.subscription.diff.EntryUpdate.Operation.CHANGE;
import static io.spine.web.firebase.subscription.diff.EntryUpdate.Operation.PASS;
import static io.spine.web.firebase.subscription.diff.EntryUpdate.Operation.REMOVE;

/**
 * Static factory for {@link EntryUpdate Entry Update} instance.
 */
class EntryUpdates {

    /**
     * Prevents instantiation of this static factory.
     */
    private EntryUpdates() {
    }

    static EntryUpdate changeEntry(UpToDateEntry entry, ExistingEntry matchingEntry) {
        return EntryUpdate
                .newBuilder()
                .setKey(matchingEntry.key())
                .setData(entry.data())
                .setOperation(CHANGE)
                .build();
    }

    static EntryUpdate passEntry(UpToDateEntry entry, ExistingEntry matchingEntry) {
        return EntryUpdate
                .newBuilder()
                .setKey(matchingEntry.key())
                .setData(entry.data())
                .setOperation(PASS)
                .build();
    }

    static EntryUpdate addEntry(UpToDateEntry entry) {
        return EntryUpdate
                .newBuilder()
                .setData(entry.data())
                .setOperation(ADD)
                .build();
    }

    static EntryUpdate removeEntry(ExistingEntry existing) {
        return EntryUpdate
                .newBuilder()
                .setKey(existing.key())
                .setData(existing.data())
                .setOperation(REMOVE)
                .build();
    }
}
