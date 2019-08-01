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

package io.spine.web.firebase;

import io.spine.annotation.Internal;
import io.spine.client.Query;
import io.spine.client.QueryId;
import io.spine.core.ActorContext;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.web.firebase.subscription.SubscriptionToken;

/**
 * A factory creating {@link NodePath}s where query results are placed.
 */
@Internal
public class RequestNodePath {

    @SuppressWarnings("DuplicateStringLiteralInspection") // Random duplication.
    private static final String DEFAULT_TENANT = "common";

    /** Prevents instantiation of this static factory. */
    private RequestNodePath() {
    }

    /**
     * Creates an instance of {@code NodePath} which points to a database node storing
     * the {@link io.spine.client.QueryResponse QueryResponse} to the given {@link Query}.
     *
     * @param query
     *         the query to host the response of
     * @return new {@code NodePath}
     */
    public static NodePath of(Query query) {
        ActorContext context = query.getContext();
        String tenantId = tenantIdAsString(context.getTenantId());
        String actor = actorAsString(context.getActor());
        String queryId = queryIdAsString(query);
        return NodePaths.of(tenantId, actor, queryId);
    }

    public static NodePath of(SubscriptionToken subscription) {
        String type = subscription.getTarget();
        String tenantId = tenantIdAsString(subscription.getTenant());
        String actor = actorAsString(subscription.getUser());
        String subscriptionId = subscriptionIdAsString(subscription);
        return NodePaths.of(type, tenantId, actor, subscriptionId);
    }

    public static String tenantIdAsString(TenantId tenantId) {
        TenantId.KindCase kind = tenantId.getKindCase();
        switch (kind) {
            case EMAIL:
                return tenantId
                        .getEmail()
                        .getValue();
            case DOMAIN:
                return tenantId
                        .getDomain()
                        .getValue();
            case VALUE:
                return tenantId.getValue();
            case KIND_NOT_SET: // Fallthrough intended.
            default:
                return DEFAULT_TENANT;
        }
    }

    private static String actorAsString(UserId actor) {
        String result = actor.getValue();
        return result;
    }

    private static String queryIdAsString(Query query) {
        QueryId queryId = query.getId();
        String result = queryId.getValue();
        return result;
    }

    private static String subscriptionIdAsString(SubscriptionToken subscription) {
        return subscription.getId().getValue();
    }


}
