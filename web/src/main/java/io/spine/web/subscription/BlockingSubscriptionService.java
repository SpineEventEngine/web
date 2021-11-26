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

package io.spine.web.subscription;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.grpc.stub.StreamObserver;
import io.spine.annotation.Internal;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionUpdate;
import io.spine.client.Topic;
import io.spine.client.TopicId;
import io.spine.client.grpc.SubscriptionServiceGrpc;
import io.spine.client.grpc.SubscriptionServiceGrpc.SubscriptionServiceImplBase;
import io.spine.grpc.MemoizingObserver;
import io.spine.web.SubscriptionOrError;
import io.spine.web.WebSubscription;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.base.Errors.causeOf;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;
import static java.util.Collections.synchronizedSet;

/**
 * A wrapper for a local {@link io.spine.server.SubscriptionService} which returns the subscription
 * result in a blocking manner.
 */
@Internal
public final class BlockingSubscriptionService {

    private static final String SUBSCRIBE_METHOD_NAME = SubscriptionServiceGrpc
            .getSubscribeMethod()
            .getFullMethodName();

    private final SubscriptionServiceImplBase subscriptionService;
    private final Set<TopicId> activeTopics = synchronizedSet(new HashSet<>());

    public BlockingSubscriptionService(SubscriptionServiceImplBase service) {
        this.subscriptionService = checkNotNull(service);
    }

    /**
     * Creates and activates a subscription for the given topic.
     *
     * @param topic
     *         the subscription topic
     * @param updateObserver
     *         the subscription result observer
     * @return new subscription
     */
    @CanIgnoreReturnValue
    public SubscriptionOrError subscribe(Topic topic,
                                         StreamObserver<SubscriptionUpdate> updateObserver) {
        checkNotNull(topic);
        checkNotNull(updateObserver);

        MemoizingObserver<Subscription> subscriptionObserver = memoizingObserver();
        subscriptionService.subscribe(topic, subscriptionObserver);
        var error = subscriptionObserver.getError();
        if (error != null) {
            return SubscriptionOrError.newBuilder()
                    .setError(causeOf(error))
                    .build();
        }
        checkObserver(subscriptionObserver);
        var subscription = subscriptionObserver.firstResponse();
        subscriptionService.activate(subscription, updateObserver);
        activeTopics.add(topic.getId());
        return SubscriptionOrError.newBuilder()
                .setSubscription(WebSubscription.newBuilder().setSubscription(subscription))
                .build();
    }

    private void checkObserver(MemoizingObserver<Subscription> observer) {
        var error = observer.getError();
        if (error != null) {
            throw illegalStateWithCauseOf(error);
        } else {
            checkState(observer.isCompleted(),
                       "Provided SubscriptionService implementation (`%s`) must complete `%s`" +
                               " procedure at once.",
                       subscriptionService,
                       SUBSCRIBE_METHOD_NAME);
        }
    }

    /**
     * Cancels the given subscription.
     */
    public void cancel(Subscription subscription) {
        checkNotNull(subscription);
        subscriptionService.cancel(subscription, noOpObserver());
        activeTopics.remove(subscription.getTopic().getId());
    }
}
