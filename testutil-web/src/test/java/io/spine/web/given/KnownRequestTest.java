/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.google.common.io.CharStreams;
import com.google.common.net.MediaType;
import com.google.common.testing.NullPointerTester;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

@DisplayName("`KnownRequest` should")
final class KnownRequestTest {

    @Test
    @DisplayName("not tolerate `null`s")
    void notTolerateNull() {
        var tester = new NullPointerTester();
        tester.testAllPublicStaticMethods(KnownRequest.class);
        tester.testAllPublicInstanceMethods(KnownRequest.newBuilder());
    }

    @Test
    @DisplayName("return set values")
    @SuppressWarnings("JdkObsolete")
        // we're force to follow the contract
    void returnSetValues() throws IOException {
        var text = "some text";
        var uri = "/perform/action";
        var type = MediaType.PLAIN_TEXT_UTF_8;
        var headerName = "custom";
        var headerValue = "header";
        var headers = ImmutableMap.of(headerName, headerValue);
        var request = KnownRequest
                .newBuilder()
                .withContent(text)
                .withType(type)
                .withHeaders(headers)
                .withUri(uri)
                .build();
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
        assertThat(request.getRequestURI())
                .isEqualTo(uri);
    }

    @Test
    @DisplayName("create empty request")
    void empty() throws IOException {
        var request = KnownRequest.empty();
        assertThat(request.getContentLength())
                .isEqualTo(0);
        assertThat(request.getContentLengthLong())
                .isEqualTo(0);
        assertThat(request.getContentType())
                .isEqualTo(MediaType.ANY_TYPE.toString());
        assertThat(CharStreams.toString(request.getReader()))
                .isEmpty();
        assertThat(request.getRequestURI())
                .isEmpty();
    }
}
