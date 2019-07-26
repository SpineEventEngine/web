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

import com.google.gson.JsonObject;
import io.spine.testing.UtilityClassTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static io.spine.web.firebase.subscription.diff.EntryUpdate.Operation.ADD;
import static io.spine.web.firebase.subscription.diff.EntryUpdate.Operation.CHANGE;
import static io.spine.web.firebase.subscription.diff.EntryUpdate.Operation.PASS;
import static io.spine.web.firebase.subscription.diff.EntryUpdate.Operation.REMOVE;
import static io.spine.web.firebase.subscription.diff.EntryUpdates.addEntry;
import static io.spine.web.firebase.subscription.diff.EntryUpdates.changeEntry;
import static io.spine.web.firebase.subscription.diff.EntryUpdates.passEntry;
import static io.spine.web.firebase.subscription.diff.EntryUpdates.removeEntry;
import static io.spine.web.firebase.subscription.diff.ExistingEntry.fromJson;
import static io.spine.web.firebase.subscription.given.HasChildren.anyKey;
import static java.lang.String.format;

/**
 * Test for {@link EntryUpdates}.
 *
 * <p>Static members are not extracted due to package visibility of the members.
 */
class EntryUpdatesTest extends UtilityClassTest<EntryUpdates> {

    private static final String KEY = anyKey();
    private static final String KEY_VALUE_TEMPLATE = "{\"%s\": %s}";
    private static final ExistingEntry EXISTING_ENTRY =
            existingEntry("{\"foo\": \"bar\"}");
    private static final UpToDateEntry UP_TO_DATE_ENTRY =
            upToDateEntry(format(KEY_VALUE_TEMPLATE, KEY, "{\"foo\": \"bar\"}"));
    private static final ExistingEntry OLD_ENTRY =
            existingEntry("{\"foo\": \"oldValue\"}");
    private static final UpToDateEntry NEW_ENTRY =
            upToDateEntry(format(KEY_VALUE_TEMPLATE, KEY, "{\"foo\": \"newValue\"}"));

    EntryUpdatesTest() {
        super(EntryUpdates.class);
    }

    @Test
    void createsChangeEntry() {
        EntryUpdate entry = changeEntry(NEW_ENTRY, OLD_ENTRY);

        assertThat(entry).isEqualTo(EntryUpdate
                                            .newBuilder()
                                            .setKey(OLD_ENTRY.key())
                                            .setData(NEW_ENTRY.rawData())
                                            .setOperation(CHANGE)
                                            .build());
    }

    @Test
    void createsPassEntry() {
        EntryUpdate entry = passEntry(UP_TO_DATE_ENTRY, EXISTING_ENTRY);

        assertThat(entry).isEqualTo(EntryUpdate
                                            .newBuilder()
                                            .setKey(EXISTING_ENTRY.key())
                                            .setData(UP_TO_DATE_ENTRY.rawData())
                                            .setOperation(PASS)
                                            .build());
    }

    @Test
    void createsAddEntry() {
        EntryUpdate entry = addEntry(UP_TO_DATE_ENTRY);

        assertThat(entry).isEqualTo(EntryUpdate
                                            .newBuilder()
                                            .setData(UP_TO_DATE_ENTRY.rawData())
                                            .setOperation(ADD)
                                            .build());
    }

    @Test
    void createsRemoveEntry() {
        EntryUpdate entry = removeEntry(EXISTING_ENTRY);

        assertThat(entry).isEqualTo(EntryUpdate
                                            .newBuilder()
                                            .setKey(EXISTING_ENTRY.key())
                                            .setData(EXISTING_ENTRY.rawData())
                                            .setOperation(REMOVE)
                                            .build());
    }

    private static ExistingEntry existingEntry(String value) {
        JsonObject json = new JsonObject();
        json.addProperty(KEY, value);
        return fromJson(json).get(0);
    }

    private static UpToDateEntry upToDateEntry(String s) {
        List<UpToDateEntry> upToDateEntries =
                UpToDateEntry.parse(newArrayList(s));
        return upToDateEntries.get(0);
    }
}
