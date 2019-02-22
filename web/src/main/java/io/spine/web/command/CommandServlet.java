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

package io.spine.web.command;

import com.google.common.net.MediaType;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.server.CommandService;
import io.spine.web.NonSerializableServlet;
import io.spine.web.future.FutureObserver;
import io.spine.web.parser.HttpMessages;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static io.spine.json.Json.toCompactJson;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

/**
 * An {@link HttpServlet} representing a command endpoint.
 *
 * <p>Handles {@code POST} requests with {@linkplain Command commands} in their bodies.
 *
 * @author Dmytro Dashenkov
 */
@SuppressWarnings("serial") // Java serialization is not supported.
public abstract class CommandServlet extends NonSerializableServlet {

    private static final MediaType MIME_TYPE = JSON_UTF_8;

    private final CommandService commandService;

    protected CommandServlet(CommandService commandService) {
        super();
        this.commandService = checkNotNull(commandService);
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        Optional<Command> parsed = HttpMessages.parse(req, Command.class);
        if (!parsed.isPresent()) {
            resp.sendError(SC_BAD_REQUEST);
        } else {
            Command command = parsed.get();
            FutureObserver<Ack> ack = FutureObserver.withDefault(Ack.getDefaultInstance());
            commandService.post(command, ack);
            Ack result = ack.toFuture()
                            .join();
            writeToResponse(result, resp);
        }
    }

    private static void writeToResponse(Ack ack, HttpServletResponse response)
            throws IOException {
        String json = toCompactJson(ack);
        response.getWriter().append(json);
        response.setContentType(MIME_TYPE.toString());
    }
}
