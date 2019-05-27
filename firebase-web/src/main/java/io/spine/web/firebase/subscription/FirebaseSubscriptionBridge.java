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

import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionId;
import io.spine.client.Topic;
import io.spine.client.grpc.QueryServiceGrpc.QueryServiceImplBase;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.NodePaths;
import io.spine.web.firebase.query.QueryNodePath;
import io.spine.web.query.BlockingQueryService;
import io.spine.web.subscription.SubscriptionBridge;
import io.spine.web.subscription.result.SubscribeResult;
import io.spine.web.subscription.result.SubscriptionCancelResult;
import io.spine.web.subscription.result.SubscriptionKeepUpResult;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.client.Queries.generateId;
import static io.spine.core.Responses.statusOk;

/**
 * An implementation of {@link SubscriptionBridge} based on the Firebase Realtime Database.
 *
 * <p>The bridge allows to {@link #subscribe(Topic) subscribe} to some {@linkplain Topic topic},
 * {@linkplain #keepUp(Subscription) keep up} the created {@linkplain Subscription subscription},
 * and {@linkplain #cancel(Subscription) cancel} the created subscription.
 */
public final class FirebaseSubscriptionBridge implements SubscriptionBridge {

    private final BlockingQueryService queryService;
    private final FirebaseClient firebaseClient;

    private FirebaseSubscriptionBridge(Builder builder) {
        this.queryService = builder.queryService;
        this.firebaseClient = builder.firebaseClient;
    }

    @Override
    public SubscribeResult subscribe(Topic topic) {
        Query query = newQueryForTopic(topic);
        QueryResponse queryResponse = queryService.execute(query);
        NodePath path = QueryNodePath.of(query);
        SubscriptionRecord record = new SubscriptionRecord(path, queryResponse);
        record.storeAsInitial(firebaseClient);
        SubscriptionId id = newSubscriptionId(record.path());
        Subscription subscription = newSubscription(id, topic);
        return new FirebaseSubscribeResult(subscription);
    }

    private static Query newQueryForTopic(Topic topic) {
        return Query
                .newBuilder()
                .setId(generateId())
                .setTarget(topic.getTarget())
                .setFieldMask(topic.getFieldMask())
                .setContext(topic.getContext())
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
        Topic topic = subscription.getTopic();
        Query query = newQueryForTopic(topic);
        QueryResponse queryResponse = queryService.execute(query);
        SubscriptionId id = subscription.getId();
        NodePath path = NodePaths.of(id.getValue());
        SubscriptionRecord record = new SubscriptionRecord(path, queryResponse);
        record.storeAsUpdate(firebaseClient);
        return new FirebaseSubscriptionKeepUpResult(statusOk());
    }

    @Override
    public SubscriptionCancelResult cancel(Subscription subscription) {
        return new FirebaseSubscriptionCancelResult(statusOk());
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

        private BlockingQueryService queryService;
        private FirebaseClient firebaseClient;

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
            return new FirebaseSubscriptionBridge(this);
        }
    }
}
