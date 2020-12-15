/*
 * Copyright 2020, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.web.firebase.subscription;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.client.Subscription;
import io.spine.client.Topic;
import io.spine.client.TopicId;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.Durations.compare;
import static com.google.protobuf.util.Timestamps.between;
import static java.util.Collections.synchronizedMap;

/**
 * The log of the {@code Topic}s to which the clients are still subscribed.
 *
 * <p>To understand whether a client is still listening to the {@code Topic} updates, she
 * periodically sends a {@link FirebaseSubscriptionBridge#keepUp(Subscription) keepUp(Subscription)}
 * request. The server records the timestamps of these requests in this log and counts the client
 * alive, as long as the {@linkplain #withTimeout(Duration)} configured} timeout does not pass
 * since the last request.
 */
final class HealthLog {

    private final Map<TopicId, Timestamp> updateTimes;
    private final Duration expirationTimeout;

    private HealthLog(Map<TopicId, Timestamp> updateTimes, Duration expirationTimeout) {
        this.updateTimes = checkNotNull(updateTimes);
        this.expirationTimeout = checkNotNull(expirationTimeout);
    }

    /**
     * Creates a {@code HealthLog} with a certain topic expiration timeout.
     *
     * @param expirationTimeout
     *         a duration of a timeout to count a {@code Topic} active since its last {@code keepUp}
     *         request is received
     * @return a new instance of {@code HealthLog}
     */
    static HealthLog withTimeout(Duration expirationTimeout) {
        return new HealthLog(synchronizedMap(new HashMap<>()), expirationTimeout);
    }

    /**
     * Updates the health record for the given {@code Topic} by recording the timestamp of its
     * context as a time of update.
     *
     * @param topic
     *         the topic to update.
     */
    void put(Topic topic) {
        TopicId id = topic.getId();
        Timestamp updateTime = topic.getContext()
                                    .getTimestamp();
        updateTimes.put(id, updateTime);
    }

    /**
     * Tells whether any activity has been recorded for the given {@code Topic}.
     */
    boolean isKnown(Topic topic) {
        TopicId id = topic.getId();
        return updateTimes.containsKey(id);
    }

    /**
     * Tells whether this topic is already stale, meaning that no updates were received for it for
     * longer than an expiration timeout set for this instance of {@code HealthLog}.
     *
     * <p>If the given {@code topic} isn't {@linkplain #isKnown(Topic) known} to this log,
     * a {@linkplain NullPointerException} is thrown.
     *
     * @param topic
     *         the topic to detect staleness
     * @return {@code true} if the topic is stale, {@code false} otherwise
     */
    boolean isStale(Topic topic) {
        TopicId id = topic.getId();
        Timestamp lastUpdate = updateTimes.get(id);
        checkNotNull(lastUpdate);
        Timestamp now = Time.currentTime();
        Duration elapsed = between(lastUpdate, now);
        return compare(elapsed, expirationTimeout) > 0;
    }
}
