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

package io.spine.web.firebase.query;

import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.client.grpc.QueryServiceGrpc.QueryServiceImplBase;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.query.BlockingQueryService;
import io.spine.web.query.QueryBridge;
import io.spine.web.query.QueryProcessingResult;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * An implementation of {@link QueryBridge} based on the Firebase Realtime Database.
 *
 * <p>This bridge stores the {@link QueryResponse} data to a location in a given Firebase database
 * and retrieves the database path to that response as the result.
 *
 * <p>More formally, for each encountered {@link Query}, the bridge performs a call to
 * the {@code QueryService} and stores the resulting entity states into the given database. The data
 * is stored as a list of strings. Each entry is
 * a {@linkplain io.spine.json.Json JSON representation} of an entity state. The path produced by
 * the bridge as a result is the path to the database node containing all those records.
 * The absolute position of such a node is not specified, thus the result path is the only way
 * to read the data from the database.
 */
public final class FirebaseQueryBridge implements QueryBridge {

    private final BlockingQueryService queryService;
    private final FirebaseClient firebaseClient;

    private FirebaseQueryBridge(Builder builder) {
        this.queryService = builder.queryService;
        this.firebaseClient = builder.firebaseClient;
    }

    /**
     * Sends the given {@link Query} to the {@code QueryService} and
     * stores the query response into the database.
     *
     * <p>Returns the path in the database under which the query response is stored.
     *
     * @param query the query to send
     * @return a path in the database
     */
    @Override
    public QueryProcessingResult send(Query query) {
        QueryResponse queryResponse = queryService.execute(query);
        QueryRecord record = new QueryRecord(query, queryResponse);
        record.storeVia(firebaseClient);

        QueryProcessingResult result =
                new QueryResult(record.path(), queryResponse.getMessageCount());
        return result;
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
        public FirebaseQueryBridge build() {
            checkState(queryService != null, "Query Service is not set.");
            checkState(firebaseClient != null, "Firebase database client is not set.");
            return new FirebaseQueryBridge(this);
        }
    }
}
