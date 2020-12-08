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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;

import static com.google.common.truth.Truth.assertThat;
import static javax.servlet.http.HttpServletResponse.SC_BAD_GATEWAY;

@DisplayName("`MemoizingResponse` should")
class MemoizingResponseTest {

    @Test
    @DisplayName("return set values")
    @SuppressWarnings("JdkObsolete") // we're force to follow the contract
    void returnSetValues() {
        String headerName = "custom";
        String headerValue = "header";
        MemoizingResponse response = new MemoizingResponse();
        response.sendError(SC_BAD_GATEWAY);
        assertThat(response.getStatus())
                .isEqualTo(SC_BAD_GATEWAY);
        response.addHeader(headerName, headerValue);
        assertThat(response.getHeader(headerName))
                .isEqualTo(headerValue);
        PrintWriter printWriter = response.getWriter();
        printWriter.print("I'm writing you!");
        assertThat(response.writerContent())
                .isEqualTo("I'm writing you!");
    }
}
