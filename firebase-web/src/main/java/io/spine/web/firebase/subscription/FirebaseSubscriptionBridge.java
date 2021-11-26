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
import io.spine.base.Error;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionId;
import io.spine.client.SubscriptionValidationError;
import io.spine.client.Topic;
import io.spine.client.grpc.SubscriptionServiceGrpc.SubscriptionServiceImplBase;
import io.spine.core.Ack;
import io.spine.core.Status;
import io.spine.web.Cancel;
import io.spine.web.KeepUp;
import io.spine.web.KeepUpOutcome;
import io.spine.web.Subscribe;
import io.spine.web.SubscriptionOrError;
import io.spine.web.SubscriptionsCancelled;
import io.spine.web.SubscriptionsCreated;
import io.spine.web.SubscriptionsKeptUp;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.RequestNodePath;
import io.spine.web.subscription.BlockingSubscriptionService;
import io.spine.web.subscription.SubscriptionBridge;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.protobuf.util.Durations.compare;
import static com.google.protobuf.util.Durations.fromMinutes;
import static com.google.protobuf.util.Durations.isNegative;
import static com.google.protobuf.util.Timestamps.add;
import static io.spine.base.Time.currentTime;
import static io.spine.client.SubscriptionValidationError.INVALID_SUBSCRIPTION_VALUE;
import static io.spine.client.SubscriptionValidationError.UNKNOWN_SUBSCRIPTION_VALUE;
import static io.spine.core.Responses.statusOk;
import static io.spine.protobuf.AnyPacker.pack;
import static java.lang.String.format;

/**
 * An implementation of {@link SubscriptionBridge} based on the Firebase Realtime Database.
 *
 * <p>The bridge allows to {@link #subscribe subscribe} to some {@linkplain Topic topic},
 * {@linkplain #keepUp keep up} the created {@linkplain Subscription subscription},
 * and {@linkplain #cancel cancel} the created subscription.
 */
@SuppressWarnings("OverlyCoupledClass") /* This is fine for this integration node. */
public final class FirebaseSubscriptionBridge
        implements SubscriptionBridge {

    private static final Duration MIN_PROLONGATION = Durations.fromSeconds(1);
    private final SubscriptionRepository repository;
    private final Duration maxProlongation;

    private FirebaseSubscriptionBridge(Builder builder) {
        this.maxProlongation = builder.maxProlongation;
        this.repository = new SubscriptionRepository(builder.firebaseClient,
                                                     builder.subscriptionService);
        repository.subscribeToAll();
    }

    @Override
    public SubscriptionsCreated subscribe(Subscribe request) {
        checkNotNull(request);
        var expirationTime = calculateExpirationTime(request.getLifespan());
        if (expirationTime.isEmpty()) {
            return invalidDuration(request);
        }
        var validThru = expirationTime.get();
        var result = SubscriptionsCreated.newBuilder();
        for (var topic : request.getTopicList()) {
            result.addResult(subscribe(topic, validThru));
        }
        return result.setValidThru(validThru)
                     .build();
    }

    private static SubscriptionsCreated invalidDuration(Subscribe request) {
        var error = invalidDurationError(request.getLifespan(), "create");
        var subscriptionOrError = SubscriptionOrError.newBuilder()
                .setError(error)
                .build();
        return SubscriptionsCreated.newBuilder()
                .addResult(subscriptionOrError)
                .build();
    }

    private static Error invalidDurationError(Duration duration, String operationName) {
        var error = Error.newBuilder()
                .setType(SubscriptionValidationError.class.getName())
                .setCode(INVALID_SUBSCRIPTION_VALUE)
                .setMessage(format(
                        "Cannot %s a subscription. Invalid duration: %s.",
                        operationName,
                        Durations.toString(duration)
                ))
                .build();
        return error;
    }

    private SubscriptionOrError subscribe(Topic topic, Timestamp validThru) {
        var path = RequestNodePath.of(topic);
        var subscription = Subscription.newBuilder()
                .setId(path.asSubscriptionId())
                .setTopic(topic)
                .buildPartial();
        var timed = TimedSubscription.newBuilder()
                .setSubscription(subscription)
                .setValidThru(validThru)
                .build();
        var response = repository.create(timed);
        response = enrichSubscription(response, path);
        return response;
    }

    private static SubscriptionOrError enrichSubscription(SubscriptionOrError subscription,
                                                          NodePath firebasePath) {
        var builder = subscription.toBuilder();
        builder.getSubscriptionBuilder()
               .addExtra(pack(firebasePath));
        return builder.build();
    }

    private Optional<Timestamp> calculateExpirationTime(Duration suggestedLifespan) {
        var lifespan = normalizedProlongation(suggestedLifespan);
        return lifespan.map(l -> add(currentTime(), l));
    }

    @Override
    public SubscriptionsKeptUp keepUp(KeepUp request) {
        var duration = normalizedProlongation(request.getProlongBy());
        if (duration.isEmpty()) {
            return invalidProlongation(request);
        }
        var prolongation = duration.get();
        var response = SubscriptionsKeptUp.newBuilder();
        for (var id : request.getSubscriptionList()) {
            response.addOutcome(keepUp(prolongation, id));
        }
        return response.build();
    }

    private static SubscriptionsKeptUp invalidProlongation(KeepUp request) {
        var outcome = KeepUpOutcome.newBuilder()
                .setError(invalidDurationError(request.getProlongBy(), "keep up"))
                .build();
        return SubscriptionsKeptUp.newBuilder()
                .addOutcome(outcome)
                .build();
    }

    private KeepUpOutcome keepUp(Duration prolongation, SubscriptionId id) {
        var outcome = KeepUpOutcome.newBuilder()
                .setId(id);
        var subscription = repository.find(id);
        if (subscription.isPresent()) {
            var oldValue = subscription.get();
            var newTime = add(oldValue.getValidThru(), prolongation);
            var newValue = oldValue.toBuilder()
                    .setValidThru(newTime)
                    .build();
            repository.update(newValue);
            outcome.setNewValidThru(newTime);
        } else {
            outcome.setError(missingError(id));
        }
        return outcome.build();
    }

    private Optional<Duration> normalizedProlongation(Duration requested) {
        if (isNegative(requested)) {
            return Optional.empty();
        }
        if (compare(MIN_PROLONGATION, requested) > 0) {
            return Optional.of(MIN_PROLONGATION);
        }
        if (compare(maxProlongation, requested) < 0) {
            return Optional.of(maxProlongation);
        }
        return Optional.of(requested);
    }

    @Override
    public SubscriptionsCancelled cancel(Cancel request) {
        checkNotNull(request);
        var result = SubscriptionsCancelled.newBuilder();
        for (var id : request.getSubscriptionList()) {
            result.addAck(cancel(id));
        }
        return result.build();
    }

    private Ack cancel(SubscriptionId id) {
        var ack = Ack.newBuilder()
                .setMessageId(pack(id));
        var subscription = repository.findLocal(id);
        if (subscription.isPresent()) {
            var localSubscription = subscription.get();
            repository.cancel(localSubscription);
            ack.setStatus(statusOk());
        } else {
            ack.setStatus(missingStatus(id));
        }
        return ack.build();
    }

    private static Error missingError(SubscriptionId subscription) {
        var errorMessage = format("Subscription `%s` is unknown or already canceled.",
                                  subscription.getValue());
        var error = Error.newBuilder()
                .setMessage(errorMessage)
                .setType(SubscriptionValidationError.getDescriptor()
                                                    .getFullName())
                .setCode(UNKNOWN_SUBSCRIPTION_VALUE)
                .build();
        return error;
    }

    private static Status missingStatus(SubscriptionId subscription) {
        var error = missingError(subscription);
        return Status.newBuilder()
                .setError(error)
                .buildPartial();
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
