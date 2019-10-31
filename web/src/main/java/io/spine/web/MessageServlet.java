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

package io.spine.web;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.reflect.GenericTypeIndex;
import io.spine.web.parser.HttpMessages;
import io.spine.web.parser.MessageFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

@SuppressWarnings("serial")
public abstract class MessageServlet<I extends Message, O extends Message>
        extends NonSerializableServlet {

    private final Class<I> requestType;

    protected MessageServlet() {
        super();
        @SuppressWarnings("unchecked")
        Class<I> type = (Class<I>) GenericParam.REQUEST.argumentIn(this.getClass());
        requestType = type;
    }

    @Override
    @VisibleForTesting
    public final void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        Optional<MessageFormat> optionalFormat = MessageFormat.formatOf(req);
        String body = HttpMessages.body(req);
        Optional<I> optionalMessage = optionalFormat.flatMap(
                fmt -> fmt.parse(body, requestType)
        );
        if (!optionalMessage.isPresent()) {
            resp.sendError(SC_BAD_REQUEST);
        } else {
            MessageFormat format = optionalFormat.get();
            I message = optionalMessage.get();
            O response = handle(message);
            writeResponse(resp, response, format);
        }
    }

    private static void
    writeResponse(HttpServletResponse servletResponse, Message response, MessageFormat format)
            throws IOException {
        String rawMessage = format.print(response);
        servletResponse.getWriter()
                       .append(rawMessage);
        servletResponse.setContentType(format.contentType()
                                             .toString());
    }

    protected abstract O handle(I request);

    private enum GenericParam implements GenericTypeIndex<MessageServlet> {

        REQUEST(0),
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
