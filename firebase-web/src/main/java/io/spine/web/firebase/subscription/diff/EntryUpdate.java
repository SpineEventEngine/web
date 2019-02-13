package io.spine.web.firebase.subscription.diff;

/**
 * An update of a single entry in Firebase database to be performed.
 *
 * <p>An update is defined by the {@link #operation() operation} to be performed along with
 * {@link #key() entry key} and {@link #data()} data}.
 */
class EntryUpdate {

    enum Operation {ADD, REMOVE, CHANGE, PASS}

    private final String key;
    private final String data;
    private final Operation operation;

    EntryUpdate(String key, String data, Operation operation) {
        this.key = key;
        this.data = data;
        this.operation = operation;
    }

    EntryUpdate(String data, Operation operation) {
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
