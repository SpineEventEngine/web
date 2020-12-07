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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.NullPointerTester;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static com.google.common.truth.Truth.assertThat;
import static javax.servlet.http.HttpServletResponse.SC_ACCEPTED;

@DisplayName("`FixedContentResponse` should")
class FixedContentResponseTest {

    @Test
    @DisplayName("not tolerate `null`s")
    void notTolerateNull() {
        NullPointerTester tester = new NullPointerTester();
        tester.testAllPublicStaticMethods(FixedContentResponse.class);
    }

    @Test
    @DisplayName("return set values")
    @SuppressWarnings("JdkObsolete") // we're force to follow the contract
    void returnSetValues() {
        String headerName = "custom";
        String headerValue = "header";
        ImmutableMap<String, String> headers = ImmutableMap.of(headerName, headerValue);
        StringWriter writer = new StringWriter();
        FixedContentResponse response = FixedContentResponse.create(writer, SC_ACCEPTED, headers);
        assertThat(response.getStatus())
                .isEqualTo(SC_ACCEPTED);
        assertThat(response.getHeader(headerName))
                .isEqualTo(headerValue);
        assertThat(response.getHeaders(headerName))
                .isEqualTo(ImmutableSet.of(headerValue));
        assertThat(response.getHeaderNames())
                .isEqualTo(ImmutableSet.of(headerName));
        PrintWriter printWriter = response.getWriter();
        printWriter.print("Unbelievable string!");
        assertThat(writer.toString())
                .isEqualTo("Unbelievable string!");
    }
}