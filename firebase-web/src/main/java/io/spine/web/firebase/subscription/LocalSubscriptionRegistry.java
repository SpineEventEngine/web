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

import io.spine.client.Subscription;
import io.spine.client.SubscriptionId;
import io.spine.client.Topic;
import io.spine.client.TopicId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.synchronizedMap;

/**
 * An in-memory cache of {@link Subscription}s and corresponding {@link Topic}s.
 *
 * <p>{@code Topic}s of active {@code Subscription}s may be shared across different node
 * of the application. An identifier of {@code Subscription} is UUID-based, so two nodes of
 * the application will have different {@code SubscriptionId}s for the shared {@code Topic}.
 * This cache correlates the locally-created {@code Subscription}s with the identifier of
 * the shared {@code Topic}.
 */
final class LocalSubscriptionRegistry {

    private final Map<TopicId, SubscriptionId> ids = synchronizedMap(new HashMap<>());

    /**
     * Registers the passed {@code Subscription} and remembers its {@code Topic}.
     */
    void register(Subscription subscription) {
        var subscriptionId = subscription.getId();
        var topicId = subscription.getTopic().getId();
        ids.put(topicId, subscriptionId);
    }

    /**
     * Removes the given subscription from the registry.
     */
    void unregister(Subscription subscription) {
        var topicId = subscription.getTopic().getId();
        ids.remove(topicId);
    }

    /**
     * Fetches the {@code Subscription} for the given {@code Topic}, if it is present in
     * this registry.
     */
    Optional<Subscription> localSubscriptionFor(Topic topic) {
        var topicId = topic.getId();
        var subscriptionId = ids.get(topicId);
        return Optional.ofNullable(subscriptionId)
                       .map(id -> Subscription.newBuilder()
                               .setId(id)
                               .setTopic(topic)
                               .buildPartial());
    }
}
