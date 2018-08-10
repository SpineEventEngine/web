/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.web;

import io.spine.client.Query;

/**
 * A query received from Spines JS client.
 *
 * <p>Complements the {@link Query Query} sent to Spine specifying one of two query strategies:
 * <ol>
 *    <li>A non-transactional supplying items to destination one at a time.
 *    <li>A transactional strategy supplying items to destination in one batch.  
 * </ol>
 *
 * @author Mykhailo Drachuk
 */
public class WebQuery {

    private final Query query;
    private final boolean isDeliveredTransactionally;

    private WebQuery(Query query, boolean transactional) {
        this.query = query;
        this.isDeliveredTransactionally = transactional;
    }

    /**
     * @return a query to be sent to Spine
     */
    public Query getQuery() {
        return query;
    }

    /**
     * Specifies if the {@code Query} results should be delivered to client transactionally
     * (in a single batch).
     *
     * @return {@code true} if the query results should be delivered in one batch,
     *         {@code false} otherwise.
     */
    public boolean isDeliveredTransactionally() {
        return isDeliveredTransactionally;
    }

    /**
     * Builds a new {@code WebQuery} that should be delivered to client in a single batch.
     *
     * @param query the {@code Query} to be executed
     * @return a new {@link WebQuery WebQuery} instance
     */
    public static WebQuery transactionalQuery(Query query) {
        return new WebQuery(query, true);
    }

    /**
     * Builds a new {@code WebQuery} results from which can be sent to a client one by one.
     *
     * @param query the {@code Query} to be executed
     * @return a new {@link WebQuery WebQuery} instance
     */
    public static WebQuery nonTransactionalQuery(Query query) {
        return new WebQuery(query, false);
    }
}
