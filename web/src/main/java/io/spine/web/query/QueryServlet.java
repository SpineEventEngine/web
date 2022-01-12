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

package io.spine.web.query;

import com.google.protobuf.Message;
import io.spine.client.Query;
import io.spine.web.MessageServlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

/**
 * An {@link HttpServlet} which receives {@linkplain Query query requests} and handles them via
 * a {@link QueryBridge}.
 *
 * <p>The servlet supports only {@code POST} requests. {@code GET}, {@code HEAD}, {@code PUT},
 * {@code DELETE}, {@code OPTIONS}, and {@code TRACE} methods are not supported by default.
 *
 * <p>In order to perform a {@linkplain io.spine.client.Query query}, a client should send an HTTP
 * {@code POST} request to this servlet. The request body should be a {@linkplain io.spine.json.Json
 * JSON} representation of a {@link Query io.spine.client.Query}.
 *
 * <p>If the request is valid (i.e. the request body contains a valid {@link io.spine.client.Query
 * Query}), the response will contain a message with the query result. The format of the result
 * depends on the implementation of {@link QueryBridge}.
 *
 * <p>If the query cannot be parsed from the request, the response will be empty with the response
 * code {@link HttpServletResponse#SC_BAD_REQUEST 400}.
 *
 * <p>A typical implementation would extend this class and provide a {@link QueryBridge} in
 * the constructor. No additional config is required in order for this servlet to handle
 * the {@linkplain io.spine.client.Query entity queries}.
 *
 * <p>A {@code QueryServlet} does not support serialization. Please keep that in mind when selecting
 * a servlet container. When trying to serialize an instance of {@code QueryServlet}, an
 * {@link UnsupportedOperationException} is thrown.
 *
 * @param <T> the type of the query result
 */
@SuppressWarnings("serial") // Java serialization is not supported.
public abstract class QueryServlet<T extends Message> extends MessageServlet<Query, T> {

    private final QueryBridge<T> bridge;

    /**
     * Creates a new instance of {@code QueryServlet} with the given {@link QueryBridge}.
     *
     * @param bridge
     *         the query bridge to be used in this query servlet
     */
    protected QueryServlet(QueryBridge<T> bridge) {
        super();
        this.bridge = bridge;
    }

    @Override
    protected T handle(Query request) {
        return bridge.send(request);
    }
}
