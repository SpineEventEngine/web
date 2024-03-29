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

package io.spine.web.command;

import io.spine.base.Time;
import io.spine.client.CommandFactory;
import io.spine.core.Ack;
import io.spine.json.Json;
import io.spine.protobuf.AnyPacker;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.client.command.TestCommandMessage;
import io.spine.testing.logging.mute.MuteLogging;
import io.spine.web.command.given.DetachedCommandServlet;
import io.spine.web.given.MemoizingResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.StringWriter;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static io.spine.base.Identifier.newUuid;
import static io.spine.web.given.Servlets.request;
import static io.spine.web.given.Servlets.response;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`CommandServlet` should")
class CommandServletTest {

    private static final CommandFactory commandFactory =
            new TestActorRequestFactory(CommandServletTest.class).command();

    @Test
    @DisplayName("fail to serialize")
    void testSerialize() throws IOException {
        CommandServlet servlet = new DetachedCommandServlet();
        ObjectOutput stream = new ObjectOutputStream(new ByteArrayOutputStream());
        assertThrows(UnsupportedOperationException.class, () -> stream.writeObject(servlet));
    }

    @MuteLogging
    @Test
    @DisplayName("handle command POST requests")
    void testHandle() throws IOException {
        CommandServlet servlet = new DetachedCommandServlet();
        var response = new StringWriter();
        var createTask = TestCommandMessage.newBuilder()
                .setId(newUuid())
                .vBuild();
        var command = commandFactory.create(createTask);
        servlet.doPost(request(command), response(response));
        var ack = Json.fromJson(response.toString(), Ack.class);
        assertThat(command.getId())
                .isEqualTo(AnyPacker.unpack(ack.getMessageId()));
    }

    @MuteLogging
    @Test
    @DisplayName("respond 400 to an invalid command")
    void testInvalidCommand() throws IOException {
        CommandServlet servlet = new DetachedCommandServlet();
        var response = new MemoizingResponse();
        servlet.doPost(request(Time.currentTime()), response);
        assertThat(response.getStatus())
                .isEqualTo(SC_BAD_REQUEST);
    }
}
