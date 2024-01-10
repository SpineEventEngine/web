/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.protobuf.AnyPacker;
import io.spine.value.StringTypeValue;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.type.Json.toCompactJson;

/**
 * A JSON representation of data stored in a node of a Firebase Realtime DB.
 */
public final class StoredJson extends StringTypeValue {

    private static final long serialVersionUID = 0L;

    /**
     * The representation of the database {@code null} entry.
     *
     * <p>In Firebase the {@code null} node is deemed nonexistent.
     */
    private static final String JSON_NULL = "null";
    private static final StoredJson NULL_JSON = new StoredJson(JSON_NULL);

    private StoredJson(String value) {
        super(value);
    }

    /**
     * Creates a {@code StoredJson} from the given JSON string.
     */
    public static StoredJson from(String value) {
        checkNotNull(value);
        return JSON_NULL.equals(value)
               ? NULL_JSON
               : new StoredJson(value);
    }

    /**
     * Encodes the given message into a {@code StoredJson}.
     *
     * @param value
     *         message to encode
     */
    public static StoredJson encode(Message value) {
        checkNotNull(value);
        var message = value;
        if (message instanceof Any) {
            message = AnyPacker.unpack((Any) message);
        }
        var json = toCompactJson(message);
        return from(json);
    }

    /**
     * Obtains this JSON as a database node value.
     */
    public NodeValue asNodeValue() {
        return NodeValue.from(this);
    }

    /**
     * Parses the JSON string into a {@code JsonObject}.
     */
    @Internal
    public JsonObject asJsonObject() {
        var object = JsonParser.parseString(value());
        return object.getAsJsonObject();
    }

    /**
     * Checks if this JSON is equal to the {@code null} JSON value.
     */
    @SuppressWarnings("ReferenceEquality") // There is only one `null` object.
    public boolean isNull() {
        return this == NULL_JSON;
    }

    @VisibleForTesting
    public static StoredJson nullValue() {
        return NULL_JSON;
    }
}
