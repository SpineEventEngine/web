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

import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.client.IdFilter;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionUpdate;
import io.spine.client.Target;
import io.spine.client.TargetFilters;
import io.spine.type.TypeUrl;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.query.RequestNodePath;
import io.spine.web.firebase.subscription.matcher.CompositeFilters;
import io.spine.web.firebase.subscription.matcher.IdMatcher;

import java.util.List;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;

final class UpdateObserver implements StreamObserver<SubscriptionUpdate> {

    private final FirebaseClient firebase;
    private final SubscriptionRepository repository;

    UpdateObserver(FirebaseClient firebase, SubscriptionRepository repository) {
        this.firebase = checkNotNull(firebase);
        this.repository = checkNotNull(repository);
    }

    @Override
    public void onNext(SubscriptionUpdate update) {
        Subscription subscription = update.getSubscription();
        TypeUrl type = TypeUrl.parse(subscription.getTopic()
                                                 .getTarget()
                                                 .getType());
        List<PersistedSubscription> subscriptions = repository.forType(type);
        for (PersistedSubscription userSubscription : subscriptions) {
            UpdatePayload payload = extractMatching(update, userSubscription);
            if (!payload.isEmpty()) {
                NodePath token = RequestNodePath.of(userSubscription.token());
                SubscriptionRecord record = new SubscriptionRecord(token, payload);
                record.storeAsUpdate(firebase);
            }
        }
    }

    private static UpdatePayload
    extractMatching(SubscriptionUpdate update, PersistedSubscription targetSubscription) {
        UpdatePayload payload = UpdatePayload.from(update);
        Target target = targetSubscription.getSubscription()
                                          .getTopic()
                                          .getTarget();
        if (target.getIncludeAll()) {
            return payload;
        } else {
            UpdatePayload matching = filteredMessages(payload, target);
            return matching;
        }
    }

    private static UpdatePayload filteredMessages(UpdatePayload update, Target target) {
        TargetFilters filters = target.getFilters();
        IdFilter idFilter = filters.getIdFilter();
        Predicate<Message> idMatches = new IdMatcher(idFilter);
        Predicate<Message> fieldsMatch = CompositeFilters.toPredicate(filters.getFilterList());
        return update.filter(idMatches.and(fieldsMatch));
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
