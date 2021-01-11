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

import com.google.protobuf.Duration;
import io.spine.base.Error;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionId;
import io.spine.client.Target;
import io.spine.client.Topic;
import io.spine.client.grpc.SubscriptionServiceGrpc.SubscriptionServiceImplBase;
import io.spine.core.Response;
import io.spine.core.Status;
import io.spine.type.TypeUrl;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.RequestNodePath;
import io.spine.web.subscription.BlockingSubscriptionService;
import io.spine.web.subscription.SubscriptionBridge;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.protobuf.util.Durations.fromMinutes;
import static io.spine.base.Time.currentTime;
import static io.spine.core.Responses.ok;
import static java.lang.String.format;

/**
 * An implementation of {@link SubscriptionBridge} based on the Firebase Realtime Database.
 *
 * <p>The bridge allows to {@link #subscribe(Topic) subscribe} to some {@linkplain Topic topic},
 * {@linkplain #keepUp(Subscription) keep up} the created {@linkplain Subscription subscription},
 * and {@linkplain #cancel(Subscription) cancel} the created subscription.
 */
public final class FirebaseSubscriptionBridge
        implements SubscriptionBridge<FirebaseSubscription, Response, Response> {

    private final FirebaseClient firebaseClient;
    private final SubscriptionRepository repository;
    private final LocalSubscriptionRegistry subscriptionRegistry;

    private FirebaseSubscriptionBridge(Builder builder) {
        this.firebaseClient = builder.firebaseClient;
        this.subscriptionRegistry = new LocalSubscriptionRegistry();
        this.repository = new SubscriptionRepository(firebaseClient,
                                                     builder.subscriptionService,
                                                     builder.subscriptionLifeSpan,
                                                     subscriptionRegistry);
        repository.subscribeToAll();
    }

    @Override
    public FirebaseSubscription subscribe(Topic topic) {
        validateTarget(topic.getTarget());
        repository.write(topic);
        NodePath path = RequestNodePath.of(topic);
        Subscription subscription = Subscription
                .newBuilder()
                .setId(SubscriptionId.newBuilder().setValue(path.getValue()))
                .setTopic(topic)
                .buildPartial();
        return FirebaseSubscription
                .newBuilder()
                .setSubscription(subscription)
                .setNodePath(path)
                .vBuild();
    }

    private static void validateTarget(Target target) {
        String type = target.getType();
        TypeUrl url = TypeUrl.parse(type);
        checkNotNull(url);
    }

    @Override
    public Response keepUp(Subscription subscription) {
        Topic.Builder topic = subscription.getTopic()
                                          .toBuilder();
        topic.getContextBuilder().setTimestamp(currentTime());
        boolean exists = repository.updateExisting(topic.buildPartial());
        return exists ? ok() : missing(subscription);
    }

    @Override
    public Response cancel(Subscription subscription) {
        checkNotNull(subscription);
        Topic topic = subscription.getTopic();
        Optional<Subscription> localSubscription = subscriptionRegistry.localSubscriptionFor(topic);
        localSubscription.ifPresent(local -> {
            repository.cancel(local);
            NodePath updatesPath = RequestNodePath.of(topic);
            firebaseClient.delete(updatesPath);
        });
        return localSubscription.isPresent() ? ok() : missing(subscription);
    }

    private static Response missing(Subscription subscription) {
        String errorMessage =
                format("Subscription `%s` is unknown or already canceled.",
                       subscription.getId().getValue());
        Error error = Error
                .newBuilder()
                .setMessage(errorMessage)
                .buildPartial();
        Status errorStatus = Status
                .newBuilder()
                .setError(error)
                .buildPartial();
        return Response
                .newBuilder()
                .setStatus(errorStatus)
                .vBuild();
    }

    /**
     * Creates a new instance of {@code Builder} for {@code FirebaseQueryBridge} instances.
     *
     * @return new instance of {@code Builder}
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * A builder for the {@code FirebaseQueryBridge} instances.
     */
    public static final class Builder {

        private static final Duration DEFAULT_SUBSCRIPTION_LIFE_SPAN = fromMinutes(10);

        private FirebaseClient firebaseClient;
        private BlockingSubscriptionService subscriptionService;
        private Duration subscriptionLifeSpan = DEFAULT_SUBSCRIPTION_LIFE_SPAN;

        /**
         * Prevents local instantiation.
         */
        private Builder() {
        }

        public Builder setFirebaseClient(FirebaseClient firebaseClient) {
            this.firebaseClient = checkNotNull(firebaseClient);
            return this;
        }

        public Builder setSubscriptionService(SubscriptionServiceImplBase subscriptionService) {
            checkNotNull(subscriptionService);
            this.subscriptionService = new BlockingSubscriptionService(subscriptionService);
            return this;
        }

        public Builder setSubscriptionLifeSpan(Duration subscriptionLifeSpan) {
            this.subscriptionLifeSpan = checkNotNull(subscriptionLifeSpan);
            return this;
        }

        /**
         * Creates a new instance of {@code FirebaseQueryBridge}.
         *
         * @return new instance of {@code FirebaseQueryBridge}
         */
        public FirebaseSubscriptionBridge build() {
            checkState(firebaseClient != null,
                       "Firebase database client is not set to FirebaseSubscriptionBridge.");
            checkState(subscriptionService != null,
                       "Subscription Service is not set to FirebaseSubscriptionBridge.");
            return new FirebaseSubscriptionBridge(this);
        }
    }
}
