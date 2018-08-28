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

import com.google.firebase.database.MutableData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.web.firebase.FirebaseSubscriptionDiff.computeDiff;
import static io.spine.web.firebase.given.FirebaseSubscriptionDiffTestEnv.dataReturning;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Mykhailo Drachuk
 */
@DisplayName("FirebaseSubscriptionDiff should")
class FirebaseSubscriptionDiffTest {

    @Test
    @DisplayName("acknowledge a changed object")
    void createChangedDiff() {
        MutableData mock = dataReturning("{\"id\":\"1\",\"a\":1,\"b\":3}");

        FirebaseSubscriptionDiff diff = computeDiff(
                newArrayList("{\"id\":\"1\",\"a\":1,\"b\":2}"),
                newArrayList(mock)
        );

        assertEquals(1, diff.changed().size());
        assertEquals(0, diff.added().size());
        assertEquals(0, diff.removed().size());
    }

    @Test
    @DisplayName("acknowledge an added object")
    void createAddedDiff() {
        FirebaseSubscriptionDiff diff = computeDiff(
                newArrayList("{\"id\":\"1\",\"a\":1,\"b\":2}"),
                newArrayList()
        );

        assertEquals(0, diff.changed().size());
        assertEquals(1, diff.added().size());
        assertEquals(0, diff.removed().size());
    }

    @Test
    @DisplayName("acknowledge a removed object")
    void createRemovedDiff() {
        MutableData mock = dataReturning("{\"id\":\"1\",\"a\":1,\"b\":3}");

        FirebaseSubscriptionDiff diff = computeDiff(
                newArrayList(),
                newArrayList(mock)
        );

        assertEquals(0, diff.changed().size());
        assertEquals(0, diff.added().size());
        assertEquals(1, diff.removed().size());
    }

    @Test
    @DisplayName("acknowledge changes spanning multiple objects")
    void createComplexDiff() {
        MutableData changedMock = dataReturning("{\"id\":\"1\",\"a\":1,\"b\":3}");
        MutableData removedMock = dataReturning("{\"x\":\"asd\",\"y\":3}");
        MutableData passMock = dataReturning("{\"pass\":true}");
        MutableData passByIdMock = dataReturning("{\"id\":{\"value\": \"passed\"}}");

        FirebaseSubscriptionDiff diff = computeDiff(
                newArrayList("{\"id\":\"1\",\"a\":2,\"b\":4}", // changed 
                             "{\"a\":1,\"b\":3}", // added
                             "{\"id\":{\"value\": \"passed\"}}", // passed
                             "{\"id\":\"2\",\"added\":1}", // added
                             "{\"pass\": true}"), // passed
                newArrayList(changedMock, removedMock, passMock, passByIdMock)
        );

        assertEquals(1, diff.changed().size());
        assertEquals(2, diff.added().size());
        assertEquals(1, diff.removed().size());
    }

}
