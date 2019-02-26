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

package io.spine.web.firebase.given;

import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.client.EntityStateWithVersion;
import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.client.QueryResponseVBuilder;
import io.spine.client.grpc.QueryServiceGrpc;
import io.spine.core.Version;

import java.util.ArrayList;
import java.util.Collection;

import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.protobuf.Any.pack;
import static io.spine.core.Responses.ok;
import static java.util.stream.Collectors.toSet;

public final class FirebaseQueryMediatorTestEnv {

    /**
     * Prevents the utility class instantiation.
     */
    private FirebaseQueryMediatorTestEnv() {
    }

    public static final class TestQueryService extends QueryServiceGrpc.QueryServiceImplBase {

        private final Collection<EntityStateWithVersion> response;

        public TestQueryService(Message... messages) {
            super();
            this.response = copyOf(messages).stream()
                                            .map(TestQueryService::toEntityState)
                                            .collect(toSet());
        }

        @Override
        public void read(Query request, StreamObserver<QueryResponse> responseObserver) {
            QueryResponse queryResponse =
                    QueryResponseVBuilder.newBuilder()
                                         .setResponse(ok())
                                         .addAllMessages(new ArrayList<>(response))
                                         .build();
            responseObserver.onNext(queryResponse);
            responseObserver.onCompleted();
        }

        private static EntityStateWithVersion toEntityState(Message message) {
            EntityStateWithVersion result = EntityStateWithVersion
                    .newBuilder()
                    .setState(pack(message))
                    .setVersion(Version.getDefaultInstance())
                    .build();
            return result;
        }
    }
}
