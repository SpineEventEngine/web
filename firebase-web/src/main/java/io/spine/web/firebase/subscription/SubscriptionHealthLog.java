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

package io.spine.web.firebase.subscription;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.client.Topic;
import io.spine.client.TopicId;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.Durations.compare;
import static com.google.protobuf.util.Timestamps.between;
import static java.util.Collections.synchronizedMap;

final class SubscriptionHealthLog {

    private final Map<TopicId, Timestamp> updateTimes;
    private final Duration expirationTimeout;

    private SubscriptionHealthLog(Map<TopicId, Timestamp> updateTimes, Duration expirationTimeout) {
        this.updateTimes = checkNotNull(updateTimes);
        this.expirationTimeout = checkNotNull(expirationTimeout);
    }

    static SubscriptionHealthLog withTimeout(Duration expirationTimeout) {
        return new SubscriptionHealthLog(synchronizedMap(new HashMap<>()), expirationTimeout);
    }

    void put(Topic topic) {
        TopicId id = topic.getId();
        Timestamp updateTime = topic.getContext()
                                    .getTimestamp();
        updateTimes.put(id, updateTime);
    }

    boolean isActive(Topic topic) {
        TopicId id = topic.getId();
        Timestamp lastUpdate = updateTimes.get(id);
        Timestamp now = Time.currentTime();
        Duration elapsed = between(lastUpdate, now);
        return compare(elapsed, expirationTimeout) < 0;
    }
}
