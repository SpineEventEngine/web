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
import io.grpc.stub.StreamObserver;
import io.spine.client.ActorRequestFactory;
import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.client.ResponseFormat;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionId;
import io.spine.client.Topic;
import io.spine.client.grpc.QueryServiceGrpc.QueryServiceImplBase;
import io.spine.core.UserId;
import io.spine.server.SubscriptionService;
import io.spine.type.TypeUrl;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.query.RequestNodePath;
import io.spine.web.query.BlockingQueryService;
import io.spine.web.subscription.SubscriptionBridge;
import io.spine.web.subscription.result.SubscribeResult;
import io.spine.web.subscription.result.SubscriptionCancelResult;
import io.spine.web.subscription.result.SubscriptionKeepUpResult;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.protobuf.util.Durations.fromMinutes;
import static io.spine.base.Time.currentTime;
import static io.spine.client.Queries.generateId;
import static io.spine.core.Responses.statusOk;
import static io.spine.web.firebase.subscription.Tokens.tokenFor;

/**
 * An implementation of {@link SubscriptionBridge} based on the Firebase Realtime Database.
 *
 * <p>The bridge allows to {@link #subscribe(Topic) subscribe} to some {@linkplain Topic topic},
 * {@linkplain #keepUp(Subscription) keep up} the created {@linkplain Subscription subscription},
 * and {@linkplain #cancel(Subscription) cancel} the created subscription.
 */
public final class FirebaseSubscriptionBridge implements SubscriptionBridge {

    private static final UserId SUBSCRIBER = UserId
            .newBuilder()
            .setValue(FirebaseSubscriptionBridge.class.getSimpleName())
            .vBuild();
    private static final ActorRequestFactory factory = ActorRequestFactory
            .newBuilder()
            .setActor(SUBSCRIBER)
            .build();
    private final BlockingQueryService queryService;
    private final FirebaseClient firebaseClient;
    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository repository;

    private FirebaseSubscriptionBridge(Builder builder) {
        this.queryService = builder.queryService;
        this.firebaseClient = builder.firebaseClient;
        this.subscriptionService = builder.subscriptionService;
        this.repository = new SubscriptionRepository(firebaseClient, builder.subscriptionLifeSpan);
        subscribeToAll();
    }

    private void subscribeToAll() {
        Set<TypeUrl> types = repository.allTypes();
        UpdateObserver updateObserver = new UpdateObserver(firebaseClient, repository);
        StreamObserver<Subscription> subscriptionObserver =
                new SubscriptionObserver(updateObserver, subscriptionService);
        for (TypeUrl type : types) {
            Topic topic = factory.topic().allOf(type.getMessageClass());
            subscriptionService.subscribe(topic, subscriptionObserver);
        }
    }

    @Override
    public SubscribeResult subscribe(Topic topic) {
        Query query = newQueryForTopic(topic);
        QueryResponse queryResponse = queryService.execute(query);
        NodePath path = RequestNodePath.of(query);
        SubscriptionRecord record = new SubscriptionRecord(path, queryResponse);
        record.storeAsInitial(firebaseClient);
        SubscriptionId id = newSubscriptionId(record.path());
        Subscription subscription = newSubscription(id, topic);
        store(subscription);
        return new FirebaseSubscribeResult(subscription);
    }

    private static Query newQueryForTopic(Topic topic) {
        ResponseFormat format = ResponseFormat
                .newBuilder()
                .setFieldMask(topic.getFieldMask())
                .buildPartial();
        return Query
                .newBuilder()
                .setId(generateId())
                .setTarget(topic.getTarget())
                .setContext(topic.getContext())
                .setFormat(format)
                .vBuild();
    }

    private static Subscription newSubscription(SubscriptionId subscriptionId, Topic topic) {
        return Subscription
                .newBuilder()
                .setId(subscriptionId)
                .setTopic(topic)
                .vBuild();
    }

    private static SubscriptionId newSubscriptionId(NodePath path) {
        return SubscriptionId
                .newBuilder()
                .setValue(path.getValue())
                .vBuild();
    }

    @Override
    public SubscriptionKeepUpResult keepUp(Subscription subscription) {
        store(subscription);
        return new FirebaseSubscriptionKeepUpResult(statusOk());
    }

    @Override
    public SubscriptionCancelResult cancel(Subscription subscription) {
        checkNotNull(subscription);
        repository.delete(tokenFor(subscription));
        return new FirebaseSubscriptionCancelResult(statusOk());
    }

    private void store(Subscription subscription) {
        PersistedSubscription persisted = PersistedSubscription
                .newBuilder()
                .setSubscription(subscription)
                .setWhenUpdated(currentTime())
                .vBuild();
        repository.write(persisted);
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

        private BlockingQueryService queryService;
        private FirebaseClient firebaseClient;
        private SubscriptionService subscriptionService;
        private Duration subscriptionLifeSpan = DEFAULT_SUBSCRIPTION_LIFE_SPAN;

        /**
         * Prevents local instantiation.
         */
        private Builder() {
        }

        public Builder setQueryService(QueryServiceImplBase service) {
            checkNotNull(service);
            this.queryService = new BlockingQueryService(service);
            return this;
        }

        public Builder setFirebaseClient(FirebaseClient firebaseClient) {
            this.firebaseClient = checkNotNull(firebaseClient);
            return this;
        }

        public Builder setSubscriptionService(SubscriptionService subscriptionService) {
            this.subscriptionService = checkNotNull(subscriptionService);
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
            checkState(queryService != null,
                       "Query Service is not set to FirebaseSubscriptionBridge.");
            checkState(firebaseClient != null,
                       "Firebase database client is not set to FirebaseSubscriptionBridge.");
            checkState(subscriptionService != null,
                       "Subscription Service is not set to FirebaseSubscriptionBridge.");
            return new FirebaseSubscriptionBridge(this);
        }
    }
}
