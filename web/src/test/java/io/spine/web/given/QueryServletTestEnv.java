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

package io.spine.web.given;

import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.json.Json;
import io.spine.web.QueryBridge;
import io.spine.web.QueryProcessingResult;
import io.spine.web.QueryServlet;
import io.spine.web.WebQuery;

import javax.annotation.Nonnull;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * @author Dmytro Dashenkov
 */
public final class QueryServletTestEnv {

    // This duplication verifies that the parameter was not changed in production code by accident.
    @SuppressWarnings("DuplicateStringLiteralInspection") 
    public static final String TRANSACTIONAL_PARAMETER = "transactional";

    /**
     * A private constructor stopping this utility class from instantiation.
     */
    private QueryServletTestEnv() {
    }

    @SuppressWarnings("serial")
    public static final class TestQueryServlet extends QueryServlet {

        public TestQueryServlet() {
            this(Empty.getDefaultInstance());
        }

        public TestQueryServlet(Message expectedMessage) {
            this(new TestQueryBridge(expectedMessage));
        }

        public TestQueryServlet(QueryBridge bridge) {
            super(bridge);
        }
    }

    private static final class TestQueryBridge implements QueryBridge {

        private final Message response;

        private TestQueryBridge(Message response) {
            this.response = response;
        }

        @Override
        public QueryProcessingResult send(@Nonnull WebQuery query) {
            return new TestQueryProcessingResult(response);
        }
    }

    private static final class TestQueryProcessingResult implements QueryProcessingResult {

        private final Message message;

        private TestQueryProcessingResult(Message message) {
            this.message = message;
        }

        @Override
        public void writeTo(@Nonnull ServletResponse response) throws IOException {
            response.getWriter()
                    .append(Json.toJson(message));
        }
    }
}
