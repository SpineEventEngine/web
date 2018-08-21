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

package io.spine.web.query;

import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.client.Query;
import io.spine.client.QueryFactory;
import io.spine.json.Json;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.web.WebQuery;
import io.spine.web.query.given.QueryServletTestEnv.TestQueryServlet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.StringWriter;

import static io.spine.web.given.Servlets.request;
import static io.spine.web.given.Servlets.response;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("QueryServlet should")
class QueryServletTest {

    private static final QueryFactory queryFactory =
            TestActorRequestFactory.newInstance(QueryServletTest.class)
                                   .query();

    @Test
    @DisplayName("throw UnsupportedOperationException when trying to serialize")
    void testFailToSerialize() throws IOException {
        QueryServlet servlet = new TestQueryServlet();
        ObjectOutputStream stream = new ObjectOutputStream(new ByteArrayOutputStream());
        assertThrows(UnsupportedOperationException.class, () -> stream.writeObject(servlet));
        stream.close();
    }

    @Test
    @DisplayName("handle query POST requests")
    void testHandle() throws IOException {
        Timestamp expectedData = Time.getCurrentTime();
        QueryServlet servlet = new TestQueryServlet(expectedData);
        StringWriter response = new StringWriter();
        Query query = queryFactory.all(Timestamp.class);
        HttpServletRequest request = request(newTransactionalQuery(query));
        servlet.doPost(request, response(response));
        Timestamp actualData = Json.fromJson(response.toString(), Timestamp.class);
        assertEquals(expectedData, actualData);
    }

    private static WebQuery newTransactionalQuery(Query query) {
        return WebQueryVBuilder.newBuilder()
                               .setQuery(query)
                               .setDeliveredTransactionally(false)
                               .build();
    }

    @Test
    @DisplayName("respond 400 to an invalid query")
    void testInvalidCommand() throws IOException {
        QueryServlet servlet = new TestQueryServlet();
        HttpServletResponse response = response(new StringWriter());
        servlet.doPost(request(Time.getCurrentTime()), response);
        verify(response).sendError(400);
    }
}
