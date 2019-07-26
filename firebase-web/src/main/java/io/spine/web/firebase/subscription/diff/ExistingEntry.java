package io.spine.web.firebase.subscription.diff;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonObject;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * An entry retrieved from Firebase database to check the {@link UpToDateEntry} against.
 */
final class ExistingEntry {

    private final String key;
    private final String data;
    private final JsonNode json;
    private final JsonNode id;
    private final boolean containsId;

    private ExistingEntry(String key, String data) {
        this.key = key;
        this.data = data;
        this.json = JsonParser.parse(data);
        this.id = json.get("id");
        this.containsId = id != null;
    }

    static List<ExistingEntry> fromJson(JsonObject object) {
        return object
                .entrySet()
                .stream()
                .map(entry -> new ExistingEntry(entry.getKey(), entry.getValue()
                                                                     .getAsString()))
                .collect(toList());
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
     *
     * <p>Returns {@code false} if the ID is {@code null}.
     */
    boolean idEquals(@Nullable JsonNode id) {
        return containsId && this.id.equals(id);
    }
}
