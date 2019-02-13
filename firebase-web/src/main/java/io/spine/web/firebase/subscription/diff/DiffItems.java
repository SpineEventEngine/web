/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.web.firebase.subscription.diff;

/**
 * The records that get stored to Firebase database upon subscription updates.
 */
final class DiffItems {

    /**
     * An empty constructor preventing instantiation.
     */
    private DiffItems() {
    }

    /**
     * A record marking a subscription update with newly added entity.
     */
    public static class AddedItem {

        private final String data;

        AddedItem(String data) {
            this.data = data;
        }

        /**
         * JSON serialized entity data.
         */
        public String data() {
            return data;
        }
    }

    /**
     * A record marking a subscription update with an entity that was removed.
     */
    public static class RemovedItem {

        private final String key;

        RemovedItem(String key) {
            this.key = key;
        }

        /**
         * A Firebase key of an entity relative to the subscription root.
         */
        public String key() {
            return key;
        }
    }

    /**
     * A record marking a subscription update with an entity change.
     */
    public static class ChangedItem {

        private final String key;
        private final String data;

        ChangedItem(String key, String data) {
            this.key = key;
            this.data = data;
        }

        /**
         * A Firebase key of an entity relative to the subscription root.
         */
        public String key() {
            return key;
        }

        /**
         * JSON serialized entity data.
         */
        public String data() {
            return data;
        }
    }
}
