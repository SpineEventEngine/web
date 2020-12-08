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
import com.google.common.net.MediaType;
import com.google.common.testing.NullPointerTester;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

@DisplayName("`KnownRequest` should")
class KnownRequestTest {

    @Test
    @DisplayName("not tolerate `null`s")
    void notTolerateNull() {
        NullPointerTester tester = new NullPointerTester();
        tester.testAllPublicStaticMethods(KnownRequest.class);
    }

    @Test
    @DisplayName("return set values")
    @SuppressWarnings("JdkObsolete") // we're force to follow the contract
    void returnSetValues() throws IOException {
        String text = "some text";
        MediaType type = MediaType.PLAIN_TEXT_UTF_8;
        String headerName = "custom";
        String headerValue = "header";
        ImmutableMap<String, String> headers = ImmutableMap.of(headerName, headerValue);
        KnownRequest request = KnownRequest.create(text, type, headers);
        assertThat(request.getContentLength())
                .isEqualTo(text.length());
        assertThat(request.getContentLengthLong())
                .isEqualTo(text.length());
        assertThat(request.getHeader(headerName))
                .isEqualTo(headerValue);
        assertThat(request.getHeaders(headerName)
                          .nextElement())
                .isEqualTo(headerValue);
        assertThat(request.getHeaders("non-existing")
                          .hasMoreElements())
                .isFalse();
        assertThat(request.getContentType())
                .isEqualTo(type.toString());
        assertThat(request.getReader()
                          .readLine())
                .isEqualTo(text);
    }
}
