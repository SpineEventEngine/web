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

package io.spine.web.firebase.query;

import io.spine.web.firebase.NodePath;
import io.spine.web.query.QueryProcessingResult;

import javax.servlet.ServletResponse;
import java.io.IOException;

import static com.google.common.net.MediaType.JSON_UTF_8;
import static io.spine.json.Json.toCompactJson;

/**
 * A result of a query processed by a {@link FirebaseQueryBridge}.
 *
 * <p>This result represents a database path to the requested data.
 * See {@link FirebaseQueryBridge} for more details.
 */
final class QueryResult implements QueryProcessingResult {

    private static final String JSON_MIME_TYPE = JSON_UTF_8.toString();

    private final NodePath path;
    private final long count;

    QueryResult(NodePath path, long count) {
        this.path = path;
        this.count = count;
    }

    @Override
    public void writeTo(ServletResponse response) throws IOException {
        FirebaseQueryResponse queryResponse = FirebaseQueryResponse
                .newBuilder()
                .setPath(path.getValue())
                .setCount(count)
                .vBuild();
        response.getWriter()
                .append(toCompactJson(queryResponse));
        response.setContentType(JSON_MIME_TYPE);
    }
}
