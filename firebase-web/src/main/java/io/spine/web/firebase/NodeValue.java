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

import com.google.api.client.http.ByteArrayContent;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.firebase.database.utilities.Clock;
import com.google.firebase.database.utilities.DefaultClock;
import com.google.firebase.database.utilities.OffsetClock;
import com.google.gson.JsonObject;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;

import static com.google.api.client.http.ByteArrayContent.fromString;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static com.google.firebase.database.utilities.PushIdGenerator.generatePushChildName;
import static io.spine.json.Json.fromJson;

/**
 * The Firebase database node value.
 */
@Internal
public final class NodeValue {

    private final JsonObject value;

    private NodeValue(JsonObject value) {
        this.value = value;
    }

    /**
     * Creates an empty {@code NodeValue}.
     *
     * <p>This is not equivalent to the {@code null} value. An empty value is supposed to be
     * filled with entries at some point after the creation.
     */
    public static NodeValue empty() {
        return new NodeValue(new JsonObject());
    }

    /**
     * Creates a {@code NodeValue} whose underlying {@link com.google.gson.JsonObject} is
     * parsed from the given {@code String}.
     */
    static NodeValue from(StoredJson json) {
        var value = json.asJsonObject();
        return new NodeValue(value);
    }

    /**
     * Creates a new node value with all the given JSONs as its children.
     *
     * <p>The child nodes are added under generated keys.
     *
     * @param jsons child nodes
     * @return new node value
     */
    public static NodeValue withChildren(Iterable<StoredJson> jsons) {
        var nodeValue = empty();
        for (var json : jsons) {
            nodeValue.addChild(json);
        }
        return nodeValue;
    }

    /**
     * Converts the value to the {@linkplain ByteArrayContent byte array}
     * suitable for usage in the HTTP request.
     */
    public ByteArrayContent toByteArray() {
        var result = fromString(JSON_UTF_8.toString(), value.toString());
        return result;
    }

    /**
     * Parses this node value as a message of the given type.
     *
     * @see io.spine.json.Json#fromJson(String, Class)
     */
    public <M extends Message> M as(Class<M> cls) {
        var jsonMessage = value.toString();
        return fromJson(jsonMessage, cls);
    }

    /**
     * Adds a child to the value.
     *
     * <p>The key for the child is generated via
     * {@linkplain com.google.firebase.database.utilities.PushIdGenerator standard Firebase
     * generation mechanism}.
     *
     * @return the generated key under which the data was stored
     */
    @CanIgnoreReturnValue
    public String addChild(StoredJson data) {
        var key = ChildKeyGenerator.newKey();
        addChild(key, data);
        return key;
    }

    /**
     * Adds a child to the value under a specified key.
     */
    public void addChild(String key, StoredJson data) {
        checkNotNull(key);
        checkNotNull(data);
        value.addProperty(key, data.value());
    }

    /**
     * Adds a `null` child to the value under a specified key.
     */
    public void addNullChild(String key) {
        value.add(key, null);
    }

    public JsonObject underlyingJson() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    /**
     * The generator of the push child keys which mimics how the keys are generated by the
     * Firebase Admin SDK.
     */
    private static class ChildKeyGenerator {

        private static final Clock CLOCK = new OffsetClock(new DefaultClock(), 0);

        private static String newKey() {
            return generatePushChildName(CLOCK.millis());
        }
    }
}
