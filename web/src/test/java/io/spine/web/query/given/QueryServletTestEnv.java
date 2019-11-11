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

package io.spine.web.query.given;

import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.client.Query;
import io.spine.web.query.QueryBridge;
import io.spine.web.query.QueryServlet;

public final class QueryServletTestEnv {

    /**
     * A private constructor stopping this utility class from instantiation.
     */
    private QueryServletTestEnv() {
    }

    @SuppressWarnings("serial")
    public static final class TestQueryServlet extends QueryServlet<Message> {

        public TestQueryServlet() {
            this(Empty.getDefaultInstance());
        }

        public TestQueryServlet(Message expectedMessage) {
            this(new TestQueryBridge(expectedMessage));
        }

        public TestQueryServlet(QueryBridge<Message> bridge) {
            super(bridge);
        }
    }

    private static final class TestQueryBridge implements QueryBridge<Message> {

        private final Message response;

        private TestQueryBridge(Message response) {
            this.response = response;
        }

        @Override
        public Message send(Query query) {
            return response;
        }
    }
}
