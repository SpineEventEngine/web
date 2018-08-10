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

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.client.Query;
import io.spine.client.QueryFactory;
import io.spine.json.Json;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.web.given.QueryServletTestEnv.TestQueryServlet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.StringWriter;

import static io.spine.web.given.QueryServletTestEnv.TRANSACTIONAL_PARAMETER;
import static io.spine.web.given.Servlets.request;
import static io.spine.web.given.Servlets.response;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("QueryServlet should")
class QueryServletTest {

    private static final QueryFactory queryFactory =
            TestActorRequestFactory.newInstance(QueryServletTest.class).query();

    @Test
    @DisplayName("throw UnsupportedOperationException when trying to serialize")
    void testFailToSerialize() throws IOException {
        final QueryServlet servlet = new TestQueryServlet();
        final ObjectOutputStream stream = new ObjectOutputStream(new ByteArrayOutputStream());
        assertThrows(UnsupportedOperationException.class, () -> stream.writeObject(servlet));
        stream.close();
    }

    @Test
    @DisplayName("handle query POST requests")
    void testHandle() throws IOException {
        final Timestamp expectedData = Time.getCurrentTime();
        final QueryServlet servlet = new TestQueryServlet(expectedData);
        final StringWriter response = new StringWriter();
        final Query query = queryFactory.all(Timestamp.class);
        servlet.doPost(request(query), response(response));
        final Timestamp actualData = Json.fromJson(response.toString(), Timestamp.class);
        assertEquals(expectedData, actualData);
    }

    @Test
    @DisplayName("mark query as transactional if query parameter is true")
    void testTransactionalParameterTrue() throws IOException {
        QueryBridge bridge = mock(QueryBridge.class);

        QueryServlet servlet = new TestQueryServlet(bridge);

        HttpServletRequest request = request(queryFactory.all(Empty.class));
        when(request.getParameter(TRANSACTIONAL_PARAMETER)).thenReturn("true");

        when(bridge.send(any(WebQuery.class))).thenReturn(mock(QueryProcessingResult.class));
        servlet.doPost(request, response(new StringWriter()));

        ArgumentCaptor<WebQuery> captor = forClass(WebQuery.class);
        verify(bridge).send(captor.capture());
        assertTrue(captor.getValue()
                         .isDeliveredTransactionally());
    }

    @Test
    @DisplayName("mark query as non-transactional if query parameter is false")
    void testTransactionalParameterFalse() throws IOException {
        QueryBridge bridge = mock(QueryBridge.class);

        QueryServlet servlet = new TestQueryServlet(bridge);

        HttpServletRequest request = request(queryFactory.all(Empty.class));
        when(request.getParameter(TRANSACTIONAL_PARAMETER)).thenReturn("false");

        when(bridge.send(any(WebQuery.class))).thenReturn(mock(QueryProcessingResult.class));
        servlet.doPost(request, response(new StringWriter()));

        ArgumentCaptor<WebQuery> captor = forClass(WebQuery.class);
        verify(bridge).send(captor.capture());
        assertFalse(captor.getValue()
                          .isDeliveredTransactionally());
    }

    @Test
    @DisplayName("mark query as non-transactional by default")
    void testTransactionalParameterFalseByDefault() throws IOException {
        QueryBridge bridge = mock(QueryBridge.class);

        QueryServlet servlet = new TestQueryServlet(bridge);

        HttpServletRequest request = request(queryFactory.all(Empty.class));
        when(request.getParameter(TRANSACTIONAL_PARAMETER)).thenReturn(null);

        when(bridge.send(any(WebQuery.class))).thenReturn(mock(QueryProcessingResult.class));
        servlet.doPost(request, response(new StringWriter()));

        ArgumentCaptor<WebQuery> captor = forClass(WebQuery.class);
        verify(bridge).send(captor.capture());
        assertFalse(captor.getValue()
                          .isDeliveredTransactionally());
    }

    @Test
    @DisplayName("respond 400 to an invalid query")
    void testInvalidCommand() throws IOException {
        final QueryServlet servlet = new TestQueryServlet();
        final HttpServletResponse response = response(new StringWriter());
        servlet.doPost(request(Time.getCurrentTime()), response);
        verify(response).sendError(400);
    }
}
