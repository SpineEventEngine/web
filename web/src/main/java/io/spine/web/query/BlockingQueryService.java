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

package io.spine.web.query;

import io.spine.annotation.Internal;
import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.client.grpc.QueryServiceGrpc.QueryServiceImplBase;
import io.spine.grpc.MemoizingObserver;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;

/**
 * A wrapper for a local {@link io.spine.server.QueryService} which returns the query execution
 * result in a blocking manner.
 */
@Internal
public final class BlockingQueryService {

    private final QueryServiceImplBase queryService;

    public BlockingQueryService(QueryServiceImplBase service) {
        queryService = checkNotNull(service);
    }

    /**
     * Executes the given query.
     */
    public QueryResponse execute(Query query) {
        MemoizingObserver<QueryResponse> observer = memoizingObserver();
        queryService.read(query, observer);
        Throwable error = observer.getError();
        if (error != null) {
            throw illegalStateWithCauseOf(error);
        }
        checkState(observer.isCompleted());
        QueryResponse response = observer.firstResponse();
        return response;
    }
}
