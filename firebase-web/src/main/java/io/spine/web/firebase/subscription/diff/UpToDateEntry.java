package io.spine.web.firebase.subscription.diff;

import io.spine.web.firebase.StoredJson;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * An entry received from Spine and serialized to JSON to be saved to Firebase database.
 */
final class UpToDateEntry extends Entry {

    private UpToDateEntry(String rawData) {
        super(rawData);
    }

    static UpToDateEntry parse(String json) {
        return new UpToDateEntry(json);
    }

    static List<UpToDateEntry> parse(List<StoredJson> json) {
        return json.stream()
                   .map(StoredJson::value)
                   .map(UpToDateEntry::parse)
                   .collect(toList());
    }
}
