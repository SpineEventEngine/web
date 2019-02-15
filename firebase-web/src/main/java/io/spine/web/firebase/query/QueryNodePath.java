package io.spine.web.firebase.query;

import com.google.common.base.Joiner;
import io.spine.client.Query;
import io.spine.client.QueryId;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.NodePaths;

import java.util.Collection;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;

/**
 * A factory creating {@link NodePath}s where query results are placed.
 */
public class QueryNodePath {

    private static final Pattern ILLEGAL_DATABASE_PATH_SYMBOL = Pattern.compile("[\\[\\].$#]");
    private static final String SUBSTITUTION_SYMBOL = "-";
    private static final String PATH_DELIMITER = "/";
    @SuppressWarnings("DuplicateStringLiteralInspection") // Random duplication.
    private static final String DEFAULT_TENANT = "common";

    /** Prevents instantiation of this static factory. */
    private QueryNodePath() {
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
        String path = constructPath(query);
        return NodePaths.of(path);
    }

    private static String constructPath(Query query) {
        String tenantId = tenantIdAsString(query);
        String actor = actorAsString(query);
        String queryId = queryIdAsString(query);
        Collection<String> pathElements = newArrayList();
        if (!tenantId.isEmpty()) {
            pathElements.add(escaped(tenantId));
        }
        if (!actor.isEmpty()) {
            pathElements.add(escaped(actor));
        }
        if (!queryId.isEmpty()) {
            pathElements.add(escaped(queryId));
        }
        String path = Joiner.on(PATH_DELIMITER)
                            .join(pathElements);
        return path;
    }

    @SuppressWarnings("UnnecessaryDefault")
    private static String tenantIdAsString(Query query) {
        TenantId tenantId = query.getContext()
                                 .getTenantId();
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

    private static String actorAsString(Query query) {
        UserId actor = query
                .getContext()
                .getActor();
        String result = actor.getValue();
        return result;
    }

    private static String queryIdAsString(Query query) {
        QueryId queryId = query.getId();
        String result = queryId.getValue();
        return result;
    }

    private static String escaped(String dirty) {
        return ILLEGAL_DATABASE_PATH_SYMBOL
                .matcher(dirty)
                .replaceAll(SUBSTITUTION_SYMBOL);
    }
}
