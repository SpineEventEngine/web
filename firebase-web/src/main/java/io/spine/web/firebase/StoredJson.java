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

package io.spine.web.firebase;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.protobuf.AnyPacker;
import io.spine.value.StringTypeValue;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.json.Json.toCompactJson;

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

    public static StoredJson from(String value) {
        checkNotNull(value);
        return JSON_NULL.equals(value)
               ? NULL_JSON
               : new StoredJson(value);
    }

    public static StoredJson encode(Message value) {
        checkNotNull(value);
        Message message = value;
        if (message instanceof Any) {
            message = AnyPacker.unpack((Any) message);
        }
        String json = toCompactJson(message);
        return from(json);
    }

    public JsonObject asJsonObject() {
        JsonParser parser = new JsonParser();
        JsonElement object = parser.parse(value());
        return object.getAsJsonObject();
    }

    public NodeValue asNodeValue() {
        checkState(!isNull(), "A null JSON cannot be converted into a NodeValue.");
        return NodeValue.from(this);
    }

    @SuppressWarnings("ReferenceEquality") // There is only one `null` object.
    public boolean isNull() {
        return this == NULL_JSON;
    }
}
