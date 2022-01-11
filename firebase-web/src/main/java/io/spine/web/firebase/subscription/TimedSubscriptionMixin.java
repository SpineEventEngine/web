/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import io.spine.annotation.GeneratedMixin;
import io.spine.base.Time;
import io.spine.client.SubscriptionId;
import io.spine.client.Topic;
import io.spine.client.TopicId;

import static com.google.protobuf.util.Timestamps.compare;

/**
 * A generated mixin for the {@link TimedSubscription} type.
 */
@GeneratedMixin
interface TimedSubscriptionMixin extends TimedSubscriptionOrBuilder {

    /**
     * Obtains the subscription ID.
     */
    default SubscriptionId id() {
        return getSubscription().getId();
    }

    /**
     * Obtains the topic ID.
     */
    default TopicId topicId() {
        return topic().getId();
    }

    /**
     * Obtains the subscription topic.
     */
    default Topic topic() {
        return getSubscription().getTopic();
    }

    /**
     * Checks if this subscription is expired.
     *
     * <p>A subscription is considered expired only if the {@code valid_thru} time has passed.
     * More formally, a subscription is expired if the {@link Time#currentTime()} is strictly
     * greater than the {@code valid_thru} time.
     *
     * @return {@code true} if the subscription is expired and should be inactivated,
     *         {@code false} if the subscription is still relevant
     */
    default boolean isExpired() {
        var validThru = getValidThru();
        var now = Time.currentTime();
        return compare(now, validThru) > 0;
    }
}
