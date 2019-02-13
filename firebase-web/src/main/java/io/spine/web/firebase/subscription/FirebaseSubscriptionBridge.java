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
import io.spine.client.QueryVBuilder;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionId;
import io.spine.client.SubscriptionIdVBuilder;
import io.spine.client.SubscriptionVBuilder;
import io.spine.client.Topic;
import io.spine.client.grpc.QueryServiceGrpc;
import io.spine.web.firebase.client.FirebaseClient;
import io.spine.web.firebase.client.NodePath;
import io.spine.web.firebase.subscription.cancel.FirebaseSubscriptionCancelResult;
import io.spine.web.firebase.subscription.keepup.FirebaseSubscriptionKeepUpResult;
import io.spine.web.firebase.subscription.subscribe.FirebaseSubscribeResult;
import io.spine.web.query.service.AsyncQueryService;
import io.spine.web.subscription.SubscriptionBridge;
import io.spine.web.subscription.result.SubscribeResult;
import io.spine.web.subscription.result.SubscriptionCancelResult;
import io.spine.web.subscription.result.SubscriptionKeepUpResult;

import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.client.Queries.generateId;
import static io.spine.core.Responses.statusOk;
import static io.spine.web.firebase.query.QueryDatabasePathFactory.allocateForQuery;

/**
 * An implementation of {@link SubscriptionBridge} based on the Firebase Realtime Database.
 *
 * <p>The bridge allows to {@link #subscribe(Topic) subscribe} to some {@link Topic topic},
 * {@link #keepUp(Subscription) keep up} the created {@link Subscription subscription},
 * and {@link #cancel(Subscription) cancel} the created subscription.
 */
public final class FirebaseSubscriptionBridge implements SubscriptionBridge {

    private final AsyncQueryService queryService;
    private final FirebaseClient firebaseClient;

    private FirebaseSubscriptionBridge(Builder builder) {
        this.queryService = builder.queryService;
        this.firebaseClient = builder.firebaseClient;
    }

    @Override
    public SubscribeResult subscribe(Topic topic) {
        Query query = newQueryForTopic(topic);
        CompletableFuture<QueryResponse> queryResponse = queryService.execute(query);
        NodePath path = allocateForQuery(query);
        SubscriptionRecord record =
                new SubscriptionRecord(path, queryResponse);
        record.storeAsInitial(firebaseClient);
        SubscriptionId id = newSubscriptionId(record.path());
        Subscription subscription = newSubscription(id, topic);
        return new FirebaseSubscribeResult(subscription);
    }

    private static Query newQueryForTopic(Topic topic) {
        return QueryVBuilder.newBuilder()
                            .setId(generateId())
                            .setTarget(topic.getTarget())
                            .setFieldMask(topic.getFieldMask())
                            .setContext(topic.getContext())
                            .build();
    }

    private static Subscription newSubscription(SubscriptionId subscriptionId, Topic topic) {
        return SubscriptionVBuilder.newBuilder()
                                   .setId(subscriptionId)
                                   .setTopic(topic)
                                   .build();
    }

    private static SubscriptionId newSubscriptionId(NodePath path) {
        return SubscriptionIdVBuilder.newBuilder()
                                     .setValue(path.toString())
                                     .build();
    }

    @Override
    public SubscriptionKeepUpResult keepUp(Subscription subscription) {
        Topic topic = subscription.getTopic();
        Query query = newQueryForTopic(topic);
        CompletableFuture<QueryResponse> queryResponse = queryService.execute(query);
        SubscriptionId id = subscription.getId();
        NodePath path = NodePath.fromString(id.getValue());
        SubscriptionRecord record =
                new SubscriptionRecord(path, queryResponse);
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

        private AsyncQueryService queryService;
        private FirebaseClient firebaseClient;

        /**
         * Prevents local instantiation.
         */
        private Builder() {
        }

        public Builder setQueryService(
                QueryServiceGrpc.QueryServiceImplBase service) {
            checkNotNull(service);
            this.queryService = AsyncQueryService.local(service);
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
