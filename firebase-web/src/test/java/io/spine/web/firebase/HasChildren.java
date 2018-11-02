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

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.mockito.ArgumentMatcher;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableMap.copyOf;
import static java.util.stream.Collectors.toList;

/**
 * A checker of {@code FirebaseNodeValue} instances used in Firebase requests.
 *
 * <p>By default checks that the value contains all the {@linkplain #expected expected} entries
 * under the specified keys.
 *
 * <p>If {@link #ANY_KEY} is specified as entry key then the checker verifies the corresponding
 * value is present under any of the keys in the tested object.
 */
class HasChildren implements ArgumentMatcher<FirebaseNodeValue> {

    static final String ANY_KEY = "any_key";

    private final ImmutableMap<String, String> expected;

    /**
     * Creates a new matcher for the specified expected entries.
     *
     * @param expected
     *         the expected entries in "nodeKey-nodeValue" format
     */
    HasChildren(Map<String, String> expected) {
        this.expected = copyOf(expected);
    }

    @Override
    public boolean matches(FirebaseNodeValue firebaseNodeValue) {
        JsonObject actual = firebaseNodeValue.underlyingJson();
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
        if (ANY_KEY.equals(key)) {
            List<Map.Entry<String, JsonElement>> containingValue = json
                    .entrySet()
                    .stream()
                    .filter(e -> value.equals(e.getValue()
                                               .getAsString()))
                    .collect(toList());
            boolean result = !containingValue.isEmpty();
            return result;
        } else {
            boolean result = json.has(key) && value.equals(json.get(key)
                                                               .getAsString());
            return result;
        }
    }
}
