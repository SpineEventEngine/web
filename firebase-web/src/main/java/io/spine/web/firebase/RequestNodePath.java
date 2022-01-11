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

package io.spine.web.firebase;

import io.spine.annotation.Internal;
import io.spine.client.Query;
import io.spine.client.Topic;
import io.spine.core.TenantId;

/**
 * A factory creating {@link NodePath}s for placing actor request results.
 */
@Internal
public final class RequestNodePath {

    @SuppressWarnings("DuplicateStringLiteralInspection") // Random duplication.
    private static final NodePath DEFAULT_TENANT = NodePaths.of("common");

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
        var context = query.getContext();
        var tenantId = tenantIdAsPath(context.getTenantId());
        var actor = context.getActor().getValue();
        var queryId = queryIdAsString(query);
        return NodePaths.of(tenantId.getValue(), actor, queryId);
    }

    /**
     * Creates an instance of {@code NodePath} which points to a database node storing
     * updates of the given subscription {@link Topic}.
     *
     * @param topic
     *         the topic to host updates for
     * @return new {@code NodePath}
     */
    public static NodePath of(Topic topic) {
        var context = topic.getContext();
        var tenantId = tenantIdAsPath(context.getTenantId());
        var actor = context.getActor().getValue();
        var topicId = topic.getId().getValue();
        return NodePaths.of(tenantId.getValue(), actor, topicId);
    }

    /**
     * Converts a given tenant ID into a database node path.
     *
     * <p>When given a default instance, returns {@code common} as a node path. Otherwise, returns
     * the string representation of the tenant ID: an email, a domain name, or a plain string value,
     * depending on the kind of the ID.
     *
     * <p>The resulting value is a valid node path, i.e. does not contain
     * {@linkplain NodePaths#of illegal path characters}.
     *
     * @param tenantId
     *         the tenant ID
     * @return new {@code NodePath}
     */
    public static NodePath tenantIdAsPath(TenantId tenantId) {
        var kind = tenantId.getKindCase();
        switch (kind) {
            case EMAIL:
                return NodePaths.of(tenantId.getEmail().getValue());
            case DOMAIN:
                return NodePaths.of(tenantId.getDomain().getValue());
            case VALUE:
                return NodePaths.of(tenantId.getValue());
            case KIND_NOT_SET: // Fallthrough intended.
            default:
                return DEFAULT_TENANT;
        }
    }

    private static String queryIdAsString(Query query) {
        var queryId = query.getId();
        var result = queryId.getValue();
        return result;
    }
}
