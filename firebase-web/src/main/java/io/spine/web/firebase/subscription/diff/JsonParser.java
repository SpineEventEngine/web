package io.spine.web.firebase.subscription.diff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Parses JSON string to a {@link JsonNode} throwing a runtime exception in case of an error.
 */
class JsonParser {

    /** Prevents instantiation of this utility class. */
    private JsonParser() {
    }

    static JsonNode parse(String jsonString) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(jsonString);
        } catch (IOException e) {
            throw new RuntimeException("Could not parse JSON.", e);
        }
    }
}
