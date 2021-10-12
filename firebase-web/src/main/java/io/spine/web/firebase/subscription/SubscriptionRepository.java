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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionId;
import io.spine.client.Topic;
import io.spine.json.Json;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.NodePaths;
import io.spine.web.firebase.NodeValue;
import io.spine.web.firebase.StoredJson;
import io.spine.web.subscription.BlockingSubscriptionService;

import java.util.Optional;
import java.util.function.UnaryOperator;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;
import static io.spine.web.firebase.subscription.LazyRepository.lazy;

/**
 * A repository of entity/event subscriptions.
 *
 * <p>Registers the received from the client {@linkplain Topic subscription topics} in
 * the {@code SubscriptionService}, activates them, and cancels when the client requests so or when
 * the subscription becomes outdated.
 *
 * <p>Subscription repositories running on different sever instances exchange subscriptions via
 * Firebase RDB, so that each instance can post updates for each subscription.
 */
final class SubscriptionRepository {

    private static final NodePath SUBSCRIPTIONS_ROOT = NodePaths.of("subscriptions");

    private final FirebaseClient firebase;
    private final BlockingSubscriptionService subscriptionService;
    private final UpdateObserver updateObserver;
    private final LocalSubscriptionRegistry subscriptionRegistry;
    private final HealthLog healthLog;

    SubscriptionRepository(FirebaseClient firebase,
                           BlockingSubscriptionService service,
                           LocalSubscriptionRegistry subscriptionRegistry) {
        this.firebase = checkNotNull(firebase);
        this.subscriptionService = checkNotNull(service);
        this.healthLog = new HealthLog();
        this.updateObserver = new UpdateObserver(firebase, healthLog, lazy(() -> this));
        this.subscriptionRegistry = subscriptionRegistry;
    }

    /**
     * Fetches all the existing subscriptions from the Firebase and activates them.
     *
     * <p>After calling this method, all the new subscriptions are automatically activates on this
     * server instance.
     */
    void subscribeToAll() {
        ChildEventListener observer = new SubscriptionChangeObserver(this);
        firebase.subscribeTo(SUBSCRIPTIONS_ROOT, observer);
    }

    /**
     * Subscribes to the given topic and activates the subscription.
     *
     * <p>The topic may be a new one received from the client or an existing one, fetched from the
     * storage.
     *
     * @param subscription
     *         the subscription to store
     */
    void write(TimedSubscription subscription) {
        healthLog.put(subscription);
        StoredJson jsonSubscription = StoredJson.encode(subscription);
        NodePath path = pathForMeta(subscription.id());
        subscribe(subscription);
        firebase.update(path, jsonSubscription.asNodeValue());
    }

    Optional<TimedSubscription> find(SubscriptionId id) {
        NodePath path = pathForMeta(id);
        return firebase.fetchNode(path)
                .map(node -> node.as(TimedSubscription.class));
    }

    /**
     * Updates a subscription topic only if it already exists.
     *
     * @param topic
     *         the subscription topic
     * @return {@code true} if a subscription with such a topic ID exists, {@code false} otherwise
     */
    boolean updateExisting(TimedSubscription subscription) {
        NodePath path = pathForMeta(subscription.getSubscription());
        Optional<?> existing = firebase.fetchNode(path);
        if (existing.isPresent()) {
            healthLog.put(subscription);
            StoredJson jsonSubscription = StoredJson.encode(subscription);
            firebase.update(path, jsonSubscription.asNodeValue());
        }
        return existing.isPresent();
    }

    Optional<TimedSubscription> updateExisting(SubscriptionId id,
                                               UnaryOperator<TimedSubscription> update) {
        NodePath path = pathForMeta(id);
        Optional<NodeValue> existing = firebase.fetchNode(path);
        if (existing.isPresent()) {
            TimedSubscription oldValue = existing.get().as(TimedSubscription.class);
            TimedSubscription newValue = update.apply(oldValue);
            healthLog.prolong(newValue.id(), newValue.getValidThru());
            StoredJson jsonSubscription = StoredJson.encode(newValue);
            firebase.update(path, jsonSubscription.asNodeValue());
            return Optional.of(newValue);
        }
        return Optional.empty();
    }

    private void subscribe(TimedSubscription timedSubscription) {
        Topic topic = timedSubscription.topic();
        Optional<Subscription> localSubscription =
                subscriptionRegistry.localSubscriptionFor(topic);
        if (!localSubscription.isPresent()) {
            Subscription subscription = subscriptionService.subscribe(topic, updateObserver);
            subscriptionRegistry.register(subscription);
        }
    }

    /**
     * Cancels the given subscription.
     *
     * <p>After this method, all the other server instances will <i>eventually</i> stop publishing
     * updates for the subscription.
     */
    void cancel(Subscription subscription) {
        subscriptionService.cancel(subscription);
        subscriptionRegistry.unregister(subscription);
        delete(subscription.getId());
    }

    private void delete(SubscriptionId subscription) {
        NodePath path = pathForMeta(subscription);
        firebase.delete(path);
    }

    /**
     * Obtains the Firebase Realtime DB path where the subscription metadata is stored.
     */
    private static NodePath pathForMeta(Subscription subscription) {
        return pathForMeta(subscription.getId());
    }

    private static NodePath pathForMeta(SubscriptionId subscription) {
        return SUBSCRIPTIONS_ROOT.append(subscription.getValue());
    }

    /**
     * An event listener for separate subscription updates.
     *
     * <p>When a subscription is updated, checks if it is stale or not and either cancels it or
     * re-activates for this server instance.
     */
    private static final class SubscriptionChangeObserver implements ChildEventListener {

        private final SubscriptionRepository repository;

        private SubscriptionChangeObserver(SubscriptionRepository repository) {
            this.repository = repository;
        }

        @Override
        public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
            updateSubscription(snapshot);
        }

        @Override
        public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
            updateSubscription(snapshot);
        }

        private void updateSubscription(DataSnapshot snapshot) {
            String json = asJson(snapshot);
            TimedSubscription subscription = loadSubscription(json);
            deleteOrActivate(subscription);
        }

        private static String asJson(DataSnapshot snapshot) {
            Object value = snapshot.getValue();
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                throw illegalStateWithCauseOf(e);
            }
        }

        private TimedSubscription loadSubscription(String json) {
            TimedSubscription subscription = Json.fromJson(json, TimedSubscription.class);
            repository.healthLog.put(subscription);
            return subscription;
        }

        private void deleteOrActivate(TimedSubscription subscription) {
            HealthLog healthLog = repository.healthLog;
            if (healthLog.isKnown(subscription.id()) && subscription.isExpired()) {
                repository.delete(subscription.id());
            } else {
                repository.subscribe(subscription);
            }
        }

        @Override
        public void onChildRemoved(DataSnapshot snapshot) {
            // NOP.
        }

        @Override
        public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
            // NOP.
        }

        @Override
        public void onCancelled(DatabaseError error) {
            throw error.toException();
        }
    }
}
