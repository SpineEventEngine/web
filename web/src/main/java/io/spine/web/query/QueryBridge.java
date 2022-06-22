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

/**
 * An {@linkplain io.spine.client.Query entity query} bridge.
 *
 * <p>Connects the {@link io.spine.server.QueryService QueryService} with a query response
 * processor. Typically, the query response processor is the channel which sends the query response
 * to the client.
 *
 * <p>No constrains are applied to the contents of the query. Neither any guaranties are made for
 * the query result. Refer to the concrete implementations to find out the details of their
 * behaviour.
 *
 * @param <T>
 *         the type of the query result
 */
public interface QueryBridge<T extends Message> {

    /**
     * Sends the given {@link io.spine.client.Query Query} to the
     * {@link io.spine.server.QueryService QueryService} and dispatches the query response
     * to the query response processor.
     *
     * <p>Returns the result of query processing.
     *
     * @param query
     *         the query to send
     * @return the query result
     */
    T send(Query query);
}
