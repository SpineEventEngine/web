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
import io.grpc.stub.StreamObserver;
import io.spine.base.Time;
import io.spine.client.Subscription;
import io.spine.client.Topic;
import io.spine.client.grpc.SubscriptionServiceGrpc.SubscriptionServiceImplBase;
import io.spine.json.Json;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.NodePaths;
import io.spine.web.firebase.NodeValue;
import io.spine.web.firebase.StoredJson;

import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.Durations.compare;
import static com.google.protobuf.util.Timestamps.between;
import static io.spine.web.firebase.RequestNodePath.tenantIdAsString;

// TODO:2019-07-29:dmytro.dashenkov: Find a better name.
final class SubscriptionRepository {

    private static final NodePath SUBSCRIPTIONS_ROOT = NodePaths.of("subscriptions");

    private final FirebaseClient firebase;
    private final SubscriptionServiceImplBase subscriptionService;
    private final Duration expirationTimeout;
    private final StreamObserver<Subscription> subscriptionObserver;

    SubscriptionRepository(FirebaseClient firebase,
                           SubscriptionServiceImplBase service,
                           Duration timeout) {
        this.firebase = checkNotNull(firebase);
        this.subscriptionService = checkNotNull(service);
        this.expirationTimeout = checkNotNull(timeout);
        UpdateObserver observer = new UpdateObserver(firebase);
        this.subscriptionObserver = new SubscriptionObserver(observer, subscriptionService);
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
                .map(json -> Json.fromJson(json.toString(),
                                           PersistedSubscription.class))
                .map(this::deleteIfOutdated)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(persisted -> persisted.getSubscription()
                                           .getTopic())
                .forEach(topic -> subscriptionService.subscribe(topic, subscriptionObserver)));
    }

    public void write(PersistedSubscription subscription) {
        SubscriptionToken token = subscription.token();
        NodePath path = pathForSubscription(token);
        StoredJson jsonSubscription = StoredJson.encode(subscription);
        firebase.create(path, jsonSubscription.asNodeValue());
        Topic topic = subscription.getSubscription()
                                  .getTopic();
        subscriptionService.subscribe(topic,
                                      subscriptionObserver);
    }

    private Optional<PersistedSubscription> deleteIfOutdated(PersistedSubscription subscription) {
        Timestamp lastUpdate = subscription.getWhenUpdated();
        Timestamp now = Time.currentTime();
        Duration elapsed = between(lastUpdate, now);
        if (compare(elapsed, expirationTimeout) > 0) {
            delete(subscription.token());
            return Optional.empty();
        } else {
            return Optional.of(subscription);
        }
    }

    void delete(SubscriptionToken token) {
        checkNotNull(token);
        NodePath path = pathForSubscription(token);
        firebase.delete(path);
    }

    private static NodePath pathForSubscription(SubscriptionToken token) {
        String tenant = tenantIdAsString(token.getTenant());
        String subscriptionId = token.getId()
                                     .getValue();
        String targetType = token.getTarget();
        NodePath path = NodePaths.of(tenant, targetType, subscriptionId);
        return SUBSCRIPTIONS_ROOT.append(path);
    }
}
