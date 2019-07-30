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

import io.grpc.stub.StreamObserver;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionId;
import io.spine.client.SubscriptionUpdate;
import io.spine.client.Topic;
import io.spine.client.grpc.SubscriptionServiceGrpc;
import io.spine.core.Response;

import static io.spine.base.Identifier.newUuid;

public class TestSubscriptionService extends SubscriptionServiceGrpc.SubscriptionServiceImplBase {

    @Override
    public void subscribe(Topic request, StreamObserver<Subscription> responseObserver) {
        Subscription subscription = Subscription
                .newBuilder()
                .setId(SubscriptionId.newBuilder().setValue(newUuid()))
                .setTopic(request)
                .vBuild();
        responseObserver.onNext(subscription);
        responseObserver.onCompleted();
    }

    @Override
    public void activate(Subscription request,
                         StreamObserver<SubscriptionUpdate> responseObserver) {
        // Do nothing.
    }

    @Override
    public void cancel(Subscription request, StreamObserver<Response> responseObserver) {
        // Do nothing.
    }
}
