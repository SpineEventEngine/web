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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.json.Json;
import io.spine.type.TypeUrl;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.NodeValue;
import io.spine.web.firebase.StoredJson;
import io.spine.web.firebase.query.RequestNodePath;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.protobuf.util.Durations.compare;
import static com.google.protobuf.util.Timestamps.between;
import static io.spine.json.Json.fromJson;

// TODO:2019-07-29:dmytro.dashenkov: Find a better name.
final class SubscriptionRepository {

    private static final NodePath SUBSCRIPTIONS_ROOT = NodePath
            .newBuilder()
            .setValue("io.spine.web.subscription")
            .vBuild();
    private final FirebaseClient firebase;
    private final Duration expirationTimeout;

    SubscriptionRepository(FirebaseClient firebase, Duration timeout) {
        this.firebase = checkNotNull(firebase);
        this.expirationTimeout = checkNotNull(timeout);
    }

    public Optional<PersistedSubscription> read(SubscriptionToken token) {
        NodePath path = path(token);
        Optional<NodeValue> node = firebase.get(path);
        Optional<PersistedSubscription> subscription = node
                .map(val -> fromJson(val.toString(), PersistedSubscription.class))
                .flatMap(this::deleteIfOutdated);
        return subscription;
    }

    public boolean isActive(SubscriptionToken token) {
        return read(token).isPresent();
    }

    public List<PersistedSubscription> forType(TypeUrl targetType) {
        NodePath path = SUBSCRIPTIONS_ROOT.append(targetType.value());
        Optional<NodeValue> subscriptionRoot = firebase.get(path);
        if (!subscriptionRoot.isPresent()) {
            return ImmutableList.of();
        }
        Stream<JsonElement> jsons = subscriptionRoot
                .get()
                .underlyingJson()
                .entrySet()
                .stream()
                .map(Map.Entry::getValue);
        return parseActive(jsons);
    }

    public List<PersistedSubscription> all() {
        Optional<NodeValue> subscriptionRoot = firebase.get(SUBSCRIPTIONS_ROOT);
        if (!subscriptionRoot.isPresent()) {
            return ImmutableList.of();
        }
        NodeValue rootNode = subscriptionRoot.get();
        Stream<JsonElement> jsons = rootNode.underlyingJson()
                                             .entrySet()
                                             .stream()
                                             .flatMap(entry -> entry.getValue()
                                                                    .getAsJsonObject()
                                                                    .entrySet()
                                                                    .stream())
                                             .map(Map.Entry::getValue);
        return parseActive(jsons);
    }

    public Set<TypeUrl> allTypes() {
        Optional<NodeValue> subscriptionRoot = firebase.get(SUBSCRIPTIONS_ROOT);
        if (!subscriptionRoot.isPresent()) {
            return ImmutableSet.of();
        }
        NodeValue rootNode = subscriptionRoot.get();
        ImmutableSet<TypeUrl> types = rootNode.underlyingJson()
                                              .entrySet()
                                              .stream()
                                              .flatMap(entry -> entry.getValue()
                                                                     .getAsJsonObject()
                                                                     .entrySet()
                                                                     .stream())
                                              .map(entry -> TypeUrl.parse(entry.getKey()))
                                              .collect(toImmutableSet());
        return types;
    }

    private List<PersistedSubscription> parseActive(Stream<JsonElement> jsons) {
        return jsons.map(json -> Json.fromJson(json.toString(), PersistedSubscription.class))
                    .map(this::deleteIfOutdated)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(toImmutableList());
    }

    public void write(PersistedSubscription subscription) {
        NodePath path = path(subscription.token());
        StoredJson jsonSubscription = StoredJson.encode(subscription);
        firebase.update(path, jsonSubscription.asNodeValue());
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
        NodePath path = path(token);
        firebase.delete(path);
    }

    private static NodePath path(SubscriptionToken token) {
        NodePath path = RequestNodePath.of(token);
        return SUBSCRIPTIONS_ROOT.append(path);
    }
}
