/*
 * Copyright 2021, TeamDev. All rights reserved.
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

import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.client.SubscriptionId;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.protobuf.util.Timestamps.compare;
import static java.util.Collections.synchronizedMap;

/**
 * The log of the {@code Topic}s to which the clients are still subscribed.
 */
final class HealthLog {

    private final Map<SubscriptionId, Timestamp> expirationTimes = synchronizedMap(new HashMap<>());

    /**
     * Updates the health record for the given {@code Topic} by recording the timestamp of its
     * context as a time of update.
     *
     * @param subscription
     *         the ID of the subscription
     * @param validThru
     *         the subscription expiration time
     */
    void put(SubscriptionId subscription, Timestamp validThru) {
        checkNotNull(subscription);
        checkNotNull(validThru);
        expirationTimes.put(subscription, validThru);
    }

    void put(TimedSubscription timed) {
        checkNotNull(timed);
        put(timed.getSubscription().getId(), timed.getValidThru());
    }

    /**
     * Tells whether any activity has been recorded for the given {@code Topic}.
     */
    boolean isKnown(SubscriptionId subscription) {
        checkNotNull(subscription);
        return expirationTimes.containsKey(subscription);
    }

    /**
     * Tells whether this topic is already stale, meaning that no updates were received for it for
     * longer than an expiration timeout set for this instance of {@code HealthLog}.
     *
     * <p>If the given {@code topic} isn't {@linkplain #isKnown known} to this log,
     * a {@linkplain NullPointerException} is thrown.
     *
     * @param subscription
     *         the ID of the subscription to detect staleness
     * @return {@code true} if the topic is stale, {@code false} otherwise
     */
    boolean isStale(SubscriptionId subscription) {
        checkNotNull(subscription);
        Timestamp validThru = expirationTimes.get(subscription);
        checkNotNull(validThru);
        Timestamp now = Time.currentTime();
        return compare(validThru, now) > 0;
    }
}
