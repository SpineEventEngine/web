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

package io.spine.web.firebase.subscription.given;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.spine.web.firebase.NodeValue;
import org.mockito.ArgumentMatcher;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 * A checker of {@code NodeValue} instances used in Firebase requests.
 *
 * <p>By default checks that the value contains all the {@linkplain #expected expected} entries
 * under the specified keys.
 *
 * <p>If {@link #ANY_KEY} is specified as entry key then the checker verifies that corresponding
 * value is present under any of the keys in the tested object.
 */
public class HasChildren implements ArgumentMatcher<NodeValue> {

    private static final String ANY_KEY = "any_key";
    public static final String JSON_NULL = "json_null";

    private final ImmutableMap<String, String> expected;

    /**
     * Creates a new matcher for the specified expected entries.
     *
     * @param expected
     *         the expected entries in "nodeKey-nodeValue" format
     */
    public HasChildren(Map<String, String> expected) {
        this.expected = ImmutableMap.copyOf(expected);
    }

    @Override
    public boolean matches(NodeValue nodeValue) {
        JsonObject actual = nodeValue.underlyingJson();
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!hasKeyAndValue(actual, key, value)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasKeyAndValue(JsonObject json, String key, String value) {
        checkNotNull(json);
        checkNotNull(key);
        if (key.startsWith(ANY_KEY)) {
            List<Map.Entry<String, JsonElement>> containingValue = json
                    .entrySet()
                    .stream()
                    .filter(e -> equalsOrNull(e.getValue(), value))
                    .collect(toList());
            boolean result = !containingValue.isEmpty();
            return result;
        } else {
            boolean result = json.has(key) && equalsOrNull(json.get(key), value);
            return result;
        }
    }

    public static String anyKey() {
        return ANY_KEY + UUID.randomUUID().toString();
    }

    private static boolean equalsOrNull(JsonElement element, String value) {
        if (element.isJsonNull()) {
            return value.equals(JSON_NULL);
        }

        return element.getAsString().equals(value);
    }
}
