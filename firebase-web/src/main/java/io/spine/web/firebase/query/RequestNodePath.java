package io.spine.web.firebase.query;

import io.spine.client.Query;
import io.spine.client.QueryId;
import io.spine.core.ActorContext;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.NodePaths;
import io.spine.web.firebase.subscription.SubscriptionToken;

/**
 * A factory creating {@link NodePath}s where query results are placed.
 */
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
        String queryId = subscriptionIdAsString(subscription);
        return NodePaths.of(type, tenantId, actor, queryId);
    }

    private static String tenantIdAsString(TenantId tenantId) {
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
