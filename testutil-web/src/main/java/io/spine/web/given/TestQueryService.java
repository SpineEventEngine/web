/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.web.given;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.client.EntityStateWithVersion;
import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.client.grpc.QueryServiceGrpc;
import io.spine.core.Version;

import java.util.Collection;

import static io.spine.core.Responses.ok;
import static io.spine.protobuf.AnyPacker.pack;
import static java.util.stream.Collectors.toSet;

public final class TestQueryService extends QueryServiceGrpc.QueryServiceImplBase {

    private final Collection<EntityStateWithVersion> response;

    public TestQueryService(Message... messages) {
        super();
        this.response = ImmutableSet
                .copyOf(messages)
                .stream()
                .map(TestQueryService::toEntityState)
                .collect(toSet());
    }

    @Override
    public void read(Query request, StreamObserver<QueryResponse> responseObserver) {
        var queryResponse = QueryResponse.newBuilder()
                .setResponse(ok())
                .addAllMessage(response)
                .vBuild();
        responseObserver.onNext(queryResponse);
        responseObserver.onCompleted();
    }

    private static EntityStateWithVersion toEntityState(Message message) {
        var result = EntityStateWithVersion.newBuilder()
                .setState(pack(message))
                .setVersion(Version.getDefaultInstance())
                .build();
        return result;
    }
}
