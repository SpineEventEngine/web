package io.spine.web.firebase.subscription.diff;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * An entry received from Spine and serialized to JSON to be saved to Firebase database.
 */
class UpToDateEntry {

    private final String data;
    private final JsonNode json;
    private final JsonNode id;
    private final boolean containsId;

    private UpToDateEntry(String data) {
        this.data = data;
        this.json = JsonParser.parse(data);
        this.id = json.get("id");
        this.containsId = id != null;
    }

    static List<UpToDateEntry> parse(List<String> json) {
        return json.stream()
                   .map(UpToDateEntry::new)
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
