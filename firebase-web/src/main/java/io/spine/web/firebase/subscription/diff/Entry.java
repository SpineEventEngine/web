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
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation base for a Firebase Realtime Database entry.
 *
 * <p>An entry is a JSON structure which represents a domain object requested by a user.
 */
abstract class Entry {

    private static final String ID = "id";

    private final @Nullable JsonNode id;
    private final JsonNode json;
    private final String rawData;

    Entry(String rawData) {
        this.rawData = checkNotNull(rawData);
        this.json = JsonParser.parse(rawData);
        this.id = json.get(ID);
    }

    /**
     * Returns {@code true} if the entity contains an {@code "id"} field and {@code false}
     * otherwise.
     */
    boolean containsId() {
        return id != null;
    }

    /**
     * A {@link JsonNode} representation of the entities {@code "id"} field.
     */
    @Nullable JsonNode id() {
        return id;
    }

    /**
     * JSON data of this entry.
     */
    JsonNode json() {
        return json;
    }

    /**
     * JSON serialized entity data represented as a string.
     */
    String rawData() {
        return rawData;
    }
}
