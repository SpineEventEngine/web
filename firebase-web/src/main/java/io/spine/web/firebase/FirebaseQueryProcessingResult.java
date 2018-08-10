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

package io.spine.web.firebase;

import io.spine.web.QueryProcessingResult;

import javax.servlet.ServletResponse;
import java.io.IOException;

import static java.lang.String.format;

/**
 * A result of a query processed by a {@link FirebaseQueryBridge}.
 *
 * <p>This result represents a database path to the requested data.
 * See {@link FirebaseQueryBridge} for more details.
 *
 * @author Dmytro Dashenkov
 */
final class FirebaseQueryProcessingResult implements QueryProcessingResult {

    @SuppressWarnings("DuplicateStringLiteralInspection") // The duplication is a coincidence.
    private static final String MIME_TYPE = "application/json";

    private final FirebaseDatabasePath path;
    private final long count;

    FirebaseQueryProcessingResult(FirebaseDatabasePath path, long count) {
        this.path = path;
        this.count = count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeTo(ServletResponse response) throws IOException {
        final String databaseUrl = path.toString();
        response.getWriter().append(format("{\"path\": \"%s\", \"count\": %s}",
                                           databaseUrl, count));
        response.setContentType(MIME_TYPE);
    }
}
