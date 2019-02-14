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

import io.spine.web.firebase.client.NodeValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("DiffCalculator should")
class EntityEntryUpdatesDiffTest {

    @Test
    @DisplayName("acknowledge a changed object")
    void createChangedDiff() {
        NodeValue value = nodeValue("{\"id\":\"1\",\"a\":1,\"b\":3}");

        Diff diff = DiffCalculator
                .from(value)
                .compareWith(newArrayList("{\"id\":\"1\",\"a\":1,\"b\":2}"));

        assertEquals(1, diff.getChangedCount());
        assertEquals(0, diff.getAddedCount());
        assertEquals(0, diff.getRemovedCount());
    }

    @Test
    @DisplayName("acknowledge an added object")
    void createAddedDiff() {
        NodeValue value = NodeValue.empty();

        Diff diff = DiffCalculator
                .from(value)
                .compareWith(newArrayList("{\"id\":\"1\",\"a\":1,\"b\":2}"));

        assertEquals(0, diff.getChangedCount());
        assertEquals(1, diff.getAddedCount());
        assertEquals(0, diff.getRemovedCount());
    }

    @Test
    @DisplayName("acknowledge a removed object")
    void createRemovedDiff() {
        NodeValue value = nodeValue("{\"id\":\"1\",\"a\":1,\"b\":3}");

        Diff diff = DiffCalculator
                .from(value)
                .compareWith(newArrayList());

        assertEquals(0, diff.getChangedCount());
        assertEquals(0, diff.getAddedCount());
        assertEquals(1, diff.getRemovedCount());
    }

    @Test
    @DisplayName("acknowledge changes spanning multiple objects")
    void createComplexDiff() {
        NodeValue value = nodeValue(
                "{\"id\":\"1\",\"a\":1,\"b\":3}",
                "{\"x\":\"asd\",\"y\":3}",
                "{\"pass\":true}",
                "{\"id\":{\"value\": \"passed\"}}"
        );
        Diff diff = DiffCalculator
                .from(value)
                .compareWith(newArrayList("{\"id\":\"1\",\"a\":2,\"b\":4}", // changed 
                                          "{\"a\":1,\"b\":3}", // added
                                          "{\"id\":{\"value\": \"passed\"}}", // passed
                                          "{\"id\":\"2\",\"added\":1}", // added
                                          "{\"pass\": true}"));
        assertEquals(1, diff.getChangedCount());
        assertEquals(2, diff.getAddedCount());
        assertEquals(1, diff.getRemovedCount());
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "CheckReturnValue"}) // Method called to throw.
    @Test
    @DisplayName("throw RuntimeException in case the new entries are invalid")
    void throwOnIncorrectEntries() {
        String invalidJson = "invalidJson";
        List<String> newEntries = Collections.singletonList(invalidJson);
        NodeValue stubValue = NodeValue.empty();
        assertThrows(RuntimeException.class, () -> DiffCalculator.from(stubValue)
                                                                 .compareWith(newEntries));
    }

    private static NodeValue nodeValue(String... entries) {
        NodeValue nodeValue = NodeValue.empty();
        for (String entry : entries) {
            nodeValue.addChild(entry);
        }
        return nodeValue;
    }
}
