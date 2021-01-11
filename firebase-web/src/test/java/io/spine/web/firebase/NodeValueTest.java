/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.common.collect.Iterators;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static com.google.common.collect.testing.Helpers.assertEmpty;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("`NodeValue` should")
class NodeValueTest {

    private static final String KEY = "theKey";
    private static final StoredJson VALUE = StoredJson.from("theValue");
    private static final StoredJson DATA = StoredJson.from("{\"" + KEY + "\":\"" + VALUE + "\"}");

    @Test
    @DisplayName("be empty when created via the default constructor")
    void beCreatedEmpty() {
        NodeValue value = NodeValue.empty();
        JsonObject underlyingJson = value.underlyingJson();
        assertEmpty(underlyingJson.entrySet());
    }

    @Test
    @DisplayName("allow creation from the existing JSON string")
    void beCreatedFromString() {
        NodeValue value = NodeValue.from(DATA);
        assertSingleChild(value, KEY, VALUE);
    }

    @Test
    @DisplayName("add a new child under the generated key")
    void pushChild() {
        NodeValue value = NodeValue.empty();
        value.addChild(VALUE);
        assertSingleChild(value, VALUE);
    }

    @Test
    @DisplayName("add a new child with a predefined key")
    void addChildWithKey() {
        NodeValue value = NodeValue.empty();
        value.addChild(KEY, VALUE);
        assertSingleChild(value, KEY, VALUE);
    }

    private static void assertSingleChild(NodeValue value, StoredJson childValue) {
        JsonObject underlyingJson = value.underlyingJson();
        Set<Map.Entry<String, JsonElement>> entries = underlyingJson.entrySet();
        assertThat(entries)
                .hasSize(1);

        String valueString = Iterators
                .getOnlyElement(entries.iterator())
                .getValue()
                .getAsString();
        assertThat(valueString)
                .isEqualTo(childValue.value());
    }

    private static void
    assertSingleChild(NodeValue value, String childKey, StoredJson childValue) {
        JsonObject underlyingJson = value.underlyingJson();
        assertTrue(underlyingJson.has(childKey));
        String actual = underlyingJson.get(childKey)
                                      .getAsString();
        assertThat(actual)
                .isEqualTo(childValue.value());
    }
}
