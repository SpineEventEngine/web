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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.util.Map;

/**
 * Data classes for processing of entries retrieved from both Spine and Firebase storage.
 *
 * @author Mykhailo Drachuk
 */
final class FirebaseSubscriptionEntries {

    /**
     * An empty constructor preventing instantiation.
     */
    private FirebaseSubscriptionEntries() {
    }

    /**
     * An entry of an Entity received from Spine and serialized to JSON to be saved
     * to Firebase database.
     */
    static class UpToDateEntry {

        private final String data;
        private final JsonNode json;
        private final JsonNode id;
        private final boolean containsId;

        UpToDateEntry(String data) {
            this.data = data;
            this.json = toJson(data);
            this.id = this.json.get("id");
            this.containsId = id != null;
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
        String data() {
            return data;
        }

        /**
         * Returns {@code true} if the entity contains an {@code "id"} field and {@code false}
         * otherwise.
         */
        boolean containsId() {
            return containsId;
        }

        /**
         * A {@link JsonNode} representation of the entities {@code "id"} field.
         */
        JsonNode id() {
            return id;
        }
    }

    /**
     * An entry retrieved from Firebase database to check the {@link UpToDateEntry} against.
     */
    static class ExistingEntry {

        private final String key;
        private final String data;
        private final JsonNode json;
        private final JsonNode id;
        private final boolean containsId;

        private ExistingEntry(String key, String data) {
            this.key = key;
            this.data = data;
            this.json = toJson(data);
            this.id = this.json.get("id");
            this.containsId = id != null;
        }

        static ExistingEntry fromJsonObjectEntry(Map.Entry<String, JsonElement> entry) {
            return new ExistingEntry(entry.getKey(), entry.getValue().getAsString());
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
        String data() {
            return data;
        }

        /**
         * A Firebase key of an entity relative to the subscription root.
         */
        String key() {
            return key;
        }

        /**
         * Checks if this entries {@code "id"} field matches the provided one.
         */
        boolean idEquals(JsonNode id) {
            return containsId && this.id.equals(id);
        }
    }

    /**
     * An entry containing the data of an {@link UpToDateEntry} with an operation to be performed
     * to keep the Firebase database consistent with Spine state.
     */
    static class Entry {

        enum Operation {ADD, REMOVE, CHANGE, PASS}

        private final String key;
        private final String data;
        private final Operation operation;

        Entry(String key, String data, Operation operation) {
            this.key = key;
            this.data = data;
            this.operation = operation;
        }

        Entry(String data, Operation operation) {
            this.key = null;
            this.data = data;
            this.operation = operation;
        }

        /**
         * JSON serialized entity data.
         */
        String data() {
            return data;
        }

        /**
         * A Firebase key of an entity relative to the subscription root.
         */
        String key() {
            return key;
        }

        /**
         * An operation to be performed with this entry in the Firebase storage.
         */
        Operation operation() {
            return operation;
        }
    }

    private static JsonNode toJson(String jsonString) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(jsonString);
        } catch (IOException e) {
            throw new RuntimeException("Could not parse JSON.", e);
        }
    }
}
