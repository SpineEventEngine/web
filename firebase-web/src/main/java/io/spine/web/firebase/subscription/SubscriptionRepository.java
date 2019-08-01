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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.JsonElement;
import com.google.protobuf.Duration;
import io.spine.client.Subscription;
import io.spine.client.Topic;
import io.spine.json.Json;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.NodePaths;
import io.spine.web.firebase.NodeValue;
import io.spine.web.firebase.StoredJson;
import io.spine.web.subscription.BlockingSubscriptionService;

import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.web.firebase.RequestNodePath.tenantIdAsString;
import static io.spine.web.firebase.subscription.LazyRepository.lazy;

// TODO:2019-07-29:dmytro.dashenkov: Find a better name.
final class SubscriptionRepository {

    private static final NodePath SUBSCRIPTIONS_ROOT = NodePaths.of("subscriptions");

    private final FirebaseClient firebase;
    private final BlockingSubscriptionService subscriptionService;
    private final UpdateObserver updateObserver;
    private final SubscriptionHealthLog healthLog;

    SubscriptionRepository(FirebaseClient firebase,
                           BlockingSubscriptionService service,
                           Duration timeout) {
        this.firebase = checkNotNull(firebase);
        this.subscriptionService = checkNotNull(service);
        this.healthLog = SubscriptionHealthLog.withTimeout(checkNotNull(timeout));
        this.updateObserver = new UpdateObserver(firebase, healthLog, lazy(() -> this));
    }

    void subscribeToAll() {
        Optional<NodeValue> allSubscriptions = firebase.get(SUBSCRIPTIONS_ROOT);
        allSubscriptions.ifPresent(subscriptions -> subscriptions
                .underlyingJson()
                .entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .flatMap(element -> element.getAsJsonObject()
                                           .entrySet()
                                           .stream()
                                           .map(Map.Entry::getValue))
                .map(this::loadTopic)
                .map(this::deleteIfOutdated)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(topic -> subscriptionService.subscribe(topic, updateObserver)));
    }

    private Topic loadTopic(JsonElement json) {
        Topic topic = Json.fromJson(json.toString(), Topic.class);
        healthLog.put(topic);
        return topic;
    }

    @CanIgnoreReturnValue
    public Subscription write(Topic topic) {
        NodePath path = pathForSubscription(topic);
        StoredJson jsonSubscription = StoredJson.encode(topic);
        firebase.create(path, jsonSubscription.asNodeValue());
        healthLog.put(topic);
        return subscriptionService.subscribe(topic, updateObserver);
    }

    private Optional<Topic> deleteIfOutdated(Topic topic) {
        boolean active = healthLog.isActive(topic);
        if (!active) {
            delete(topic);
            return Optional.empty();
        } else {
            return Optional.of(topic);
        }
    }

    void cancel(Subscription subscription) {
        subscriptionService.cancel(subscription);
        delete(subscription.getTopic());
    }

    private void delete(Topic topic) {
        checkNotNull(topic);
        NodePath path = pathForSubscription(topic);
        firebase.delete(path);
    }

    private static NodePath pathForSubscription(Topic topic) {
        String tenant = tenantIdAsString(topic.getContext().getTenantId());
        String topicId = topic.getId().getValue();
        String targetType = topic.getTarget().getType();
        NodePath path = NodePaths.of(tenant, targetType, topicId);
        return SUBSCRIPTIONS_ROOT.append(path);
    }
}
