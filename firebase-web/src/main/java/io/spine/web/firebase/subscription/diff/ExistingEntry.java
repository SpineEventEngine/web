package io.spine.web.firebase.subscription.diff;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonObject;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 * An entry retrieved from Firebase database to check the {@link UpToDateEntry} against.
 */
final class ExistingEntry extends Entry {

    private final String key;

    private ExistingEntry(String key, String data) {
        super(data);
        this.key = checkNotNull(key);
    }

    static List<ExistingEntry> fromJson(JsonObject object) {
        return object
                .entrySet()
                .stream()
                .map(entry -> new ExistingEntry(entry.getKey(), entry.getValue().getAsString()))
                .collect(toList());
    }

    /**
     * A Firebase key of an entity relative to the subscription root.
     */
    String key() {
        return key;
    }

    /**
     * Checks if this entry's {@code "id"} field matches the provided one.
     *
     * <p>Returns {@code false} if this entry has no {@code "id"} field
     *
     * @return {@code true} if the ID of this entry is equal to the given node,
     *         {@code false} otherwise
     */
    boolean idEquals(JsonNode id) {
        return id.equals(id());
    }
}
