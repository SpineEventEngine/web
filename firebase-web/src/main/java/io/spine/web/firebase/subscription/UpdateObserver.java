/*
 * Copyright 2023, TeamDev. All rights reserved.
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

package io.spine.web.firebase.subscription;

import io.grpc.stub.StreamObserver;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionUpdate;
import io.spine.client.Topic;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.RequestNodePath;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;

/**
 * A {@code StreamObserver} for subscription updates which writes the updates into a Firebase
 * database.
 */
final class UpdateObserver implements StreamObserver<SubscriptionUpdate> {

    private final FirebaseClient firebase;
    private final HealthLog healthLog;
    private final LazyRepository repository;

    UpdateObserver(FirebaseClient firebase,
                   HealthLog healthLog,
                   LazyRepository repository) {
        this.firebase = checkNotNull(firebase);
        this.healthLog = checkNotNull(healthLog);
        this.repository = checkNotNull(repository);
    }

    @Override
    public void onNext(SubscriptionUpdate update) {
        Subscription subscription = update.getSubscription();
        Topic topic = subscription.getTopic();
        if (!healthLog.isStale(topic)) {
            UpdatePayload payload = UpdatePayload.from(update);
            NodePath path = RequestNodePath.of(subscription.getTopic());
            SubscriptionRecord record = new SubscriptionRecord(path, payload);
            record.store(firebase);
        } else {
            repository.get().cancel(subscription);
        }
    }

    @Override
    public void onError(Throwable t) {
        throw illegalStateWithCauseOf(t);
    }

    @Override
    public void onCompleted() {
        // Do nothing.
    }
}
