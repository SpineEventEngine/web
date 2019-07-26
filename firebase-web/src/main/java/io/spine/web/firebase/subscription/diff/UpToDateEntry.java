package io.spine.web.firebase.subscription.diff;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * An entry received from Spine and serialized to JSON to be saved to Firebase database.
 */
final class UpToDateEntry extends Entry {

    private UpToDateEntry(String rawData) {
        super(rawData);
    }

    static List<UpToDateEntry> parse(List<String> json) {
        return json.stream()
                   .map(UpToDateEntry::new)
                   .collect(toList());
    }
}
