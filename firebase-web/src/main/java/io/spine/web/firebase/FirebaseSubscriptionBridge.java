/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.web.firebase;

import com.google.firebase.database.FirebaseDatabase;
import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.client.QueryVBuilder;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionId;
import io.spine.client.SubscriptionIdVBuilder;
import io.spine.client.SubscriptionVBuilder;
import io.spine.client.Topic;
import io.spine.client.grpc.QueryServiceGrpc;
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
import static io.spine.web.firebase.FirebaseDatabasePath.allocateForQuery;

/**
 * An implementation of {@link SubscriptionBridge} based on the Firebase Realtime Database.
 *
 * <p>The bridge allows to {@link #subscribe(Topic) subscribe} to some {@link Topic topic},
 * {@link #keepUp(Subscription) keep up} the created {@link Subscription subscription},
 * and {@link #cancel(Subscription) cancel} the created subscription.
 *
 * @author Mykhailo Drachuk
 */
public final class FirebaseSubscriptionBridge implements SubscriptionBridge {

    private final AsyncQueryService queryService;
    private final FirebaseDatabase database;

    private FirebaseSubscriptionBridge(FirebaseSubscriptionBridge.Builder builder) {
        this.queryService = builder.queryService;
        this.database = builder.database;
    }

    @Override
    public SubscribeResult subscribe(Topic topic) {
        Query query = newQueryForTopic(topic);
        CompletableFuture<QueryResponse> queryResponse = queryService.execute(query);
        FirebaseDatabasePath path = allocateForQuery(query);
        FirebaseSubscriptionRecord record = 
                new FirebaseSubscriptionRecord(path, queryResponse);
        record.storeAsInitial(database);
        Subscription subscription = newSubscription(topic, record.path());
        return new FirebaseSubscribeResult(subscription);
    }

    private static Query newQueryForTopic(Topic topic) {
        return QueryVBuilder.newBuilder()
                            .setId(generateId())
                            .setTarget(topic.getTarget())
                            .setContext(topic.getContext())
                            .build();
    }

    private static Subscription newSubscription(Topic topic, FirebaseDatabasePath path) {
        SubscriptionId subscriptionId = newSubscriptionId(path);
        return SubscriptionVBuilder.newBuilder()
                                   .setTopic(topic)
                                   .setId(subscriptionId)
                                   .build();
    }

    private static SubscriptionId newSubscriptionId(FirebaseDatabasePath path) {
        return SubscriptionIdVBuilder.newBuilder()
                                     .setValue(path.toString())
                                     .build();
    }

    @Override
    public SubscriptionKeepUpResult keepUp(Subscription subscription) {
        Query query = newQueryForTopic(subscription.getTopic());
        CompletableFuture<QueryResponse> queryResponse = queryService.execute(query);
        SubscriptionId id = subscription.getId();
        FirebaseDatabasePath path = FirebaseDatabasePath.fromString(id.getValue());
        FirebaseSubscriptionRecord record = 
                new FirebaseSubscriptionRecord(path, queryResponse);
        record.storeAsUpdate(database);
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

        /**
         * The default amount of seconds to wait for a single record to be written.
         */
        private AsyncQueryService queryService;
        private FirebaseDatabase database;

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

        public Builder setDatabase(FirebaseDatabase database) {
            this.database = checkNotNull(database);
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
            checkState(database != null,
                       "FirebaseDatabase is not set to to FirebaseSubscriptionBridge.");
            return new FirebaseSubscriptionBridge(this);
        }
    }
}
