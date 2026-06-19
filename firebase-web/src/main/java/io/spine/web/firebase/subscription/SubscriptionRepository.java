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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionId;
import io.spine.client.Topic;
import io.spine.type.Json;
import io.spine.web.SubscriptionOrError;
import io.spine.web.WebSubscription;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.NodePaths;
import io.spine.web.firebase.StoredJson;
import io.spine.web.subscription.BlockingSubscriptionService;

import java.util.Optional;

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
                           BlockingSubscriptionService service) {
        this.firebase = checkNotNull(firebase);
        this.subscriptionService = checkNotNull(service);
        this.healthLog = new HealthLog();
        this.updateObserver = new UpdateObserver(firebase, healthLog, lazy(() -> this));
        this.subscriptionRegistry = new LocalSubscriptionRegistry();
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
    SubscriptionOrError create(TimedSubscription subscription) {
        var response = subscribe(subscription);
        if (response.hasSubscription()) {
            healthLog.put(subscription);
            var jsonSubscription = StoredJson.encode(subscription);
            var path = pathForMeta(subscription.id());
            firebase.update(path, jsonSubscription.asNodeValue());
        }
        return response;
    }

    Optional<TimedSubscription> find(SubscriptionId id) {
        var path = pathForMeta(id);
        return firebase.fetchNode(path)
                .map(node -> node.as(TimedSubscription.class));
    }

    Optional<Subscription> findLocal(SubscriptionId globalId) {
        var global = find(globalId);
        return global.flatMap(s -> subscriptionRegistry.localSubscriptionFor(s.topic()));
    }

    void update(TimedSubscription subscription) {
        var path = pathForMeta(subscription.getSubscription());
        healthLog.put(subscription);
        var jsonSubscription = StoredJson.encode(subscription);
        firebase.update(path, jsonSubscription.asNodeValue());
    }

    @CanIgnoreReturnValue
    private SubscriptionOrError subscribe(TimedSubscription timedSubscription) {
        var topic = timedSubscription.topic();
        var localSubscription = subscriptionRegistry.localSubscriptionFor(topic);
        if (localSubscription.isPresent()) {
            var existing = localSubscription.get();
            return SubscriptionOrError.newBuilder()
                    .setSubscription(WebSubscription.newBuilder().setSubscription(existing))
                    .build();
        }
        var response = newSubscription(timedSubscription, topic);
        return response;
    }

    private SubscriptionOrError newSubscription(TimedSubscription timedSubscription, Topic topic) {
        var response = subscriptionService.subscribe(topic, updateObserver);
        if (response.hasSubscription()) {
            subscriptionRegistry.register(response.getSubscription().getSubscription());
            var responseBuilder = response.toBuilder();
            responseBuilder.getSubscriptionBuilder()
                           .getSubscriptionBuilder()
                           .setId(timedSubscription.id());
            response = responseBuilder.build();
        }

        return response;
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
        var path = pathForMeta(subscription);
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
            var json = asJson(snapshot);
            var subscription = loadSubscription(json);
            deleteOrActivate(subscription);
        }

        private static String asJson(DataSnapshot snapshot) {
            var value = snapshot.getValue();
            var mapper = new ObjectMapper();
            try {
                return mapper.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                throw illegalStateWithCauseOf(e);
            }
        }

        private TimedSubscription loadSubscription(String json) {
            var subscription = Json.fromJson(TimedSubscription.class, json);
            repository.healthLog.put(subscription);
            return subscription;
        }

        private void deleteOrActivate(TimedSubscription subscription) {
            var healthLog = repository.healthLog;
            if (healthLog.isKnown(subscription) && subscription.isExpired()) {
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
