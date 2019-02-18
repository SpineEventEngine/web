package io.spine.web.firebase.subscription.diff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * Parses JSON string to a {@link JsonNode} throwing a runtime exception in case of an error.
 */
final class JsonParser {

    /** Prevents instantiation of this utility class. */
    private JsonParser() {
    }

    static JsonNode parse(String jsonString) {
        checkNotNull(jsonString);
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(jsonString);
        } catch (IOException e) {
            throw newIllegalArgumentException("Could not parse JSON.", e);
        }
    }
}
