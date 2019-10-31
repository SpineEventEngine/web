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

package io.spine.web.query;

import com.google.protobuf.Message;
import io.spine.client.Query;
import io.spine.web.MessageServlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

/**
 * An {@link HttpServlet} which receives {@linkplain Query query requests}, passes them
 * into a {@link QueryBridge} and writes the {@linkplain QueryProcessingResult sending result} into
 * the response.
 *
 * <p>The servlet supports only {@code POST} requests. {@code GET}, {@code HEAD}, {@code PUT},
 * {@code DELETE}, {@code OPTIONS}, and {@code TRACE} methods are not supported by default.
 *
 * <p>In order to perform a {@linkplain io.spine.client.Query query}, a client should send an HTTP
 * {@code POST} request to this servlet. The request body should be a {@linkplain io.spine.json.Json
 * JSON} representation of a {@link Query io.spine.client.Query}.
 *
 * <p>If the request is valid (i.e. the request body contains a valid {@link io.spine.client.Query
 * Query}), the response will contain the {@linkplain QueryProcessingResult query sending result}.
 * Otherwise, the response will be empty with the response code
 * {@link HttpServletResponse#SC_BAD_REQUEST 400}.
 *
 * <p>A typical implementation would extend this class and provide a {@link QueryBridge} in
 * the constructor. No additional config is required in order for this servlet to handle
 * the {@linkplain io.spine.client.Query entity queries}.
 *
 * <p>A {@code QueryServlet} does not support serialization. Please keep that in mind when selecting
 * a servlet container. When trying to serialize an instance of {@code QueryServlet}, an
 * {@link UnsupportedOperationException} is thrown.
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
