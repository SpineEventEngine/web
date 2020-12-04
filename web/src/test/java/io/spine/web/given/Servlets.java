/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.common.net.MediaType;
import com.google.protobuf.Message;
import io.spine.json.Json;

import javax.servlet.http.HttpServletResponse;
import java.io.StringWriter;

/**
 * The factory of mock servlet API objects, such as requests and responses.
 */
public final class Servlets {

    /**
     * Prevents the utility class instantiation.
     */
    private Servlets() {
    }

    /**
     * Creates a new request with the supplied {@code content}.
     */
    public static FixedContentRequest request(Message content) {
        String json = Json.toJson(content);
        return FixedContentRequest.create(json, MediaType.JSON_UTF_8);
    }

    /**
     * Creates a new response with the supplied {@code writer}.
     */
    public static HttpServletResponse response(StringWriter writer) {
        return FixedContentResponse.create(writer);
    }
}
