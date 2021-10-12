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
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import io.spine.base.Error;
import io.spine.base.Time;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionId;
import io.spine.client.SubscriptionValidationError;
import io.spine.client.Target;
import io.spine.client.Topic;
import io.spine.client.grpc.SubscriptionServiceGrpc.SubscriptionServiceImplBase;
import io.spine.core.Response;
import io.spine.core.Responses;
import io.spine.core.Status;
import io.spine.type.TypeUrl;
import io.spine.web.Cancel;
import io.spine.web.Cancelling;
import io.spine.web.KeepUp;
import io.spine.web.KeepUpOutcome;
import io.spine.web.KeepingUp;
import io.spine.web.Subscribe;
import io.spine.web.Subscribing;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.RequestNodePath;
import io.spine.web.subscription.BlockingSubscriptionService;
import io.spine.web.subscription.SubscriptionBridge;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.protobuf.util.Durations.fromMinutes;
import static com.google.protobuf.util.Durations.isNegative;
import static com.google.protobuf.util.Durations.toNanos;
import static com.google.protobuf.util.Timestamps.add;
import static io.spine.base.Time.currentTime;
import static io.spine.client.SubscriptionValidationError.UNKNOWN_SUBSCRIPTION_VALUE;
import static io.spine.core.Responses.errorWith;
import static io.spine.core.Responses.ok;
import static io.spine.protobuf.AnyPacker.pack;
import static java.lang.Math.abs;
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

    private static final Duration MIN_PROLONGATION = Durations.fromSeconds(1);
    private final FirebaseClient firebaseClient;
    private final SubscriptionRepository repository;
    private final LocalSubscriptionRegistry subscriptionRegistry;
    private final Duration maxProlongation;

    private FirebaseSubscriptionBridge(Builder builder) {
        this.firebaseClient = builder.firebaseClient;
        this.maxProlongation = builder.maxProlongation;
        this.subscriptionRegistry = new LocalSubscriptionRegistry();
        this.repository = new SubscriptionRepository(firebaseClient,
                                                     builder.subscriptionService,
                                                     subscriptionRegistry);
        repository.subscribeToAll();
    }

    @Override
    public FirebaseSubscription subscribe(Topic topic) {
        validateTarget(topic.getTarget());
        NodePath path = RequestNodePath.of(topic);
        Subscription subscription = Subscription
                .newBuilder()
                .setId(SubscriptionId.newBuilder().setValue(path.getValue()))
                .setTopic(topic)
                .buildPartial();
        Timestamp validThru = add(currentTime(), maxProlongation);
        TimedSubscription timed = TimedSubscription.newBuilder()
                .setSubscription(subscription)
                .setValidThru(validThru)
                .build();
        repository.write(timed);
        return FirebaseSubscription
                .newBuilder()
                .setSubscription(subscription)
                .setNodePath(path)
                .vBuild();
    }

    @Override
    public Subscribing subscribe(Subscribe request) {
        return Subscribing.getDefaultInstance();
    }

    private static void validateTarget(Target target) {
        String type = target.getType();
        TypeUrl url = TypeUrl.parse(type);
        checkNotNull(url);
    }

    @Override
    public Response keepUp(Subscription subscription) {
        KeepUp request = KeepUp.newBuilder()
                .addSubscription(subscription.getId())
                .setAddToLifetime(maxProlongation)
                .build();
        KeepingUp response = keepUp(request);
        KeepUpOutcome outcome = response.getOutcome(0);
        if (outcome.hasError()) {
            return Response
                    .newBuilder()
                    .setStatus(errorWith(outcome.getError()))
                    .build();
        }
        return ok();
    }

    @Override
    public KeepingUp keepUp(KeepUp request) {
        Duration prolongation = normalizedProlongationFrom(request);
        KeepingUp.Builder response = KeepingUp.newBuilder();
        for (SubscriptionId id : request.getSubscriptionList()) {
            Optional<TimedSubscription> newSubscription = repository.updateExisting(id, old -> {
                Timestamp newTime = add(old.getValidThru(), prolongation);
                return old.toBuilder()
                        .setValidThru(newTime)
                        .build();
            });
            KeepUpOutcome.Builder outcome = KeepUpOutcome.newBuilder()
                    .setId(id);
            if (newSubscription.isPresent()) {
                outcome.setNewValidThru(newSubscription.get().getValidThru());
            } else {
                outcome.setError(missingError(id));
            }
            response.addOutcome(outcome);
        }
        return response.build();
    }

    private Duration normalizedProlongationFrom(KeepUp request) {
        Duration requested = request.getAddToLifetime();
        if (isNegative(requested)) {
            return requested;
        }
        if (Durations.compare(MIN_PROLONGATION, requested) > 0) {
            return MIN_PROLONGATION;
        }
        if (Durations.compare(maxProlongation, requested) < 0) {
            return maxProlongation;
        }
        return requested;
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
        return localSubscription.isPresent() ? ok() : missing(subscription.getId());
    }

    @Override
    public Cancelling cancel(Cancel request) {
        return Cancelling.getDefaultInstance();
    }

    private static Error missingError(SubscriptionId subscription) {
        String errorMessage =
                format("Subscription `%s` is unknown or already canceled.",
                       subscription.getValue());
        Error error = Error
                .newBuilder()
                .setMessage(errorMessage)
                .setType(SubscriptionValidationError.getDescriptor().getFullName())
                .setCode(UNKNOWN_SUBSCRIPTION_VALUE)
                .build();
        return error;
    }

    private static Response missing(SubscriptionId subscription) {
        Error error = missingError(subscription);
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

        private static final Duration DEFAULT_TIME_BETWEEN_KEEP_UPS = fromMinutes(10);

        private FirebaseClient firebaseClient;
        private BlockingSubscriptionService subscriptionService;
        private Duration subscriptionLifespan = DEFAULT_TIME_BETWEEN_KEEP_UPS;
        private Duration maxProlongation = DEFAULT_TIME_BETWEEN_KEEP_UPS;

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

        /**
         * Configures the amount of lifetime a subscription gains after a keep-up request.
         *
         * @deprecated Use {@link #setMaxProlongation} to manage subscription lifespans.
         */
        @Deprecated
        public Builder setSubscriptionLifeSpan(Duration subscriptionLifeSpan) {
            this.subscriptionLifespan = checkNotNull(subscriptionLifeSpan);
            return this;
        }

        public Builder setMaxProlongation(Duration maxProlongation) {
            this.maxProlongation = checkNotNull(maxProlongation);
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
