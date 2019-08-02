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

package io.spine.web.subscription;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.grpc.stub.StreamObserver;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionUpdate;
import io.spine.client.Topic;
import io.spine.client.TopicId;
import io.spine.client.grpc.SubscriptionServiceGrpc;
import io.spine.client.grpc.SubscriptionServiceGrpc.SubscriptionServiceImplBase;
import io.spine.grpc.MemoizingObserver;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;
import static java.util.Collections.synchronizedSet;

public final class BlockingSubscriptionService {

    private static final String SUBSCRIBE_METHOD_NAME = SubscriptionServiceGrpc
            .getSubscribeMethod()
            .getFullMethodName();

    private final SubscriptionServiceImplBase subscriptionService;
    private final Set<TopicId> activeTopics = synchronizedSet(new HashSet<>());

    public BlockingSubscriptionService(SubscriptionServiceImplBase service) {
        this.subscriptionService = checkNotNull(service);
    }

    @CanIgnoreReturnValue
    public Subscription subscribe(Topic topic, StreamObserver<SubscriptionUpdate> updateObserver) {
        checkNotNull(topic);
        checkNotNull(updateObserver);

        MemoizingObserver<Subscription> subscriptionObserver = memoizingObserver();
        subscriptionService.subscribe(topic, subscriptionObserver);
        checkObserver(subscriptionObserver);
        Subscription subscription = subscriptionObserver.firstResponse();
        subscriptionService.activate(subscription, updateObserver);
        activeTopics.add(topic.getId());
        return subscription;
    }

    private void checkObserver(MemoizingObserver<Subscription> observer) {
        Throwable error = observer.getError();
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

    public void cancel(Subscription subscription) {
        checkNotNull(subscription);
        subscriptionService.cancel(subscription, noOpObserver());
        activeTopics.remove(subscription.getTopic().getId());
    }
}
