/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.web;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.json.Json;
import io.spine.reflect.GenericTypeIndex;
import io.spine.web.parser.MessageFormat;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

import static com.google.common.net.MediaType.JSON_UTF_8;
import static java.util.stream.Collectors.joining;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

/**
 * An HTTP servlet which accepts POST requests with a message and responds with another message.
 *
 * @param <I>
 *         the type of the input message
 * @param <O>
 *         the type of the output message
 */
@SuppressWarnings("serial")
public abstract class MessageServlet<I extends Message, O extends Message>
        extends NonSerializableServlet {

    private final Class<I> requestType;

    protected MessageServlet() {
        super();
        @SuppressWarnings("unchecked")
        var type = (Class<I>) GenericParam.REQUEST.argumentIn(this.getClass());
        requestType = type;
    }

    /**
     * Handles the servlet request and produces the response message.
     *
     * @param request
     *         the request message
     * @return the response message
     */
    protected abstract O handle(I request);

    @Override
    @VisibleForTesting
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        var optionalMessage = parseRequest(req);
        if (optionalMessage.isEmpty()) {
            resp.sendError(SC_BAD_REQUEST);
        } else {
            var message = optionalMessage.get();
            var response = handle(message);
            writeResponse(resp, response);
        }
    }

    private Optional<I> parseRequest(HttpServletRequest req) throws IOException {
        var optionalFormat = MessageFormat.formatOf(req);
        var body = body(req);
        return optionalFormat.flatMap(
                fmt -> fmt.parse(body, requestType)
        );
    }

    private static String body(ServletRequest request) throws IOException {
        var result = request.getReader()
                            .lines()
                            .collect(joining(" "));
        return result;
    }

    private void writeResponse(HttpServletResponse servletResponse, O response)
            throws IOException {
        var json = Json.toCompactJson(response);
        servletResponse.getWriter()
                       .append(json);
        servletResponse.setContentType(JSON_UTF_8.toString());
    }

    /**
     * Index of type parameters of a {@link MessageServlet}.
     */
    @SuppressWarnings("rawtypes")   /* OK for the enumeration of generic parameters. */
    private enum GenericParam implements GenericTypeIndex<MessageServlet> {

        /**
         * The {@code I} type param.
         */
        REQUEST(0),

        /**
         * The {@code O} type param.
         */
        RESPONSE(1);

        private final int index;

        GenericParam(int index) {
            this.index = index;
        }

        @Override
        public int index() {
            return index;
        }
    }
}
