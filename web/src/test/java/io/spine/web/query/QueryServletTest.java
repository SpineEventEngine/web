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
import io.spine.client.QueryFactory;
import io.spine.type.Json;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.logging.mute.MuteLogging;
import io.spine.web.given.MemoizingResponse;
import io.spine.web.query.given.QueryServletTestEnv.TestQueryServlet;
import io.spine.web.test.Task;
import io.spine.web.test.TaskId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.StringWriter;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static io.spine.base.Time.currentTime;
import static io.spine.web.given.Servlets.request;
import static io.spine.web.given.Servlets.response;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`QueryServlet` should")
class QueryServletTest {

    private static final QueryFactory queryFactory =
            new TestActorRequestFactory(QueryServletTest.class).query();

    @Test
    @DisplayName("throw UnsupportedOperationException when trying to serialize")
    void testFailToSerialize() throws IOException {
        QueryServlet<Message> servlet = new TestQueryServlet();
        var stream = new ObjectOutputStream(new ByteArrayOutputStream());
        assertThrows(UnsupportedOperationException.class, () -> stream.writeObject(servlet));
        stream.close();
    }

    @Test
    @DisplayName("handle query POST requests")
    void testHandle() throws IOException {
        var task = Task.newBuilder()
                .setId(TaskId.generate())
                .setDescription("some-task-description")
                .build();
        QueryServlet<Message> servlet = new TestQueryServlet(task);
        var response = new StringWriter();
        var query = queryFactory.all(Task.class);
        HttpServletRequest request = request(query);
        servlet.doPost(request, response(response));
        var actualData = Json.fromJson(Task.class, response.toString());
        assertThat(actualData).isEqualTo(task);
    }

    @MuteLogging
    @Test
    @DisplayName("respond 400 to an invalid query")
    void testInvalidQuery() throws IOException {
        QueryServlet<Message> servlet = new TestQueryServlet();
        var response = new MemoizingResponse();
        servlet.doPost(request(currentTime()), response);
        assertThat(response.getStatus())
                .isEqualTo(SC_BAD_REQUEST);
    }
}
