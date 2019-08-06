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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.protobuf.Duration;
import io.spine.client.Subscription;
import io.spine.client.Topic;
import io.spine.json.Json;
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
 * Firebase, so that each instance can post updates for each subscription.
 */
final class SubscriptionRepository {

    private static final NodePath SUBSCRIPTIONS_ROOT = NodePaths.of("subscriptions");

    private final FirebaseClient firebase;
    private final BlockingSubscriptionService subscriptionService;
    private final UpdateObserver updateObserver;
    private final LocalSubscriptionRegistry subscriptionRegistry;
    private final SubscriptionHealthLog healthLog;

    SubscriptionRepository(FirebaseClient firebase,
                           BlockingSubscriptionService service,
                           Duration timeout,
                           LocalSubscriptionRegistry subscriptionRegistry) {
        this.firebase = checkNotNull(firebase);
        this.subscriptionService = checkNotNull(service);
        this.healthLog = SubscriptionHealthLog.withTimeout(checkNotNull(timeout));
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
        subscribeToSubscriptionUpdates();
    }

    private void subscribeToSubscriptionUpdates() {
        ChildEventListener observer = new SubscriptionChangeObserver(this);
        firebase.subscribeTo(SUBSCRIPTIONS_ROOT, observer);
    }

    /**
     * Subscribes to the given topic and activates the subscription.
     *
     * <p>The topic may be a new one received from the client or an existing one, fetched from the
     * storage.
     *
     * @param topic
     *         the subscription topic
     */
    void write(Topic topic) {
        StoredJson jsonSubscription = StoredJson.encode(topic);
        healthLog.put(topic);
        NodePath path = pathForSubscription(topic);
        System.out.println("> Writing topic into " + path.getValue());
        firebase.update(path, jsonSubscription.asNodeValue());
    }

    void updateExisting(Topic topic) {
        NodePath path = pathForSubscription(topic);
        Optional<String> existing = firebase.fetchString(path);
        if (existing.isPresent()) {
            StoredJson jsonSubscription = StoredJson.encode(topic);
            healthLog.put(topic);
            firebase.update(path, jsonSubscription.asNodeValue());
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
        delete(subscription.getTopic());
    }

    private void delete(Topic topic) {
        checkNotNull(topic);
        NodePath path = pathForSubscription(topic);
        firebase.delete(path);
    }

    private static NodePath pathForSubscription(Topic topic) {
        String topicId = topic.getId().getValue();
        return SUBSCRIPTIONS_ROOT.append(topicId);
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
            System.out.println("> Subscription: " + json);
            Topic topic = loadTopic(json);
            deleteOrActivate(topic);
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

        private Topic loadTopic(String json) {
            Topic topic = Json.fromJson(json, Topic.class);
            repository.healthLog.put(topic);
            return topic;
        }

        private void deleteOrActivate(Topic topic) {
            SubscriptionHealthLog healthLog = repository.healthLog;
            if (healthLog.isKnown(topic) && healthLog.isStale(topic)) {
                repository.delete(topic);
            } else {
                subscribe(topic);
            }
        }

        private void subscribe(Topic topic) {
            Subscription subscription =
                    repository.subscriptionService.subscribe(topic, repository.updateObserver);
            repository.subscriptionRegistry.register(subscription);
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
