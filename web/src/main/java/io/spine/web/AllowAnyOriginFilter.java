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

package io.spine.web;

import com.google.common.net.HttpHeaders;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;

/**
 * An servlet filter which appends the CORS headers to HTTP responses.
 *
 * <p>Requests from any origin are allowed. The requests are allowed to contain credentials and
 * the {@code Content-Type} header.
 *
 * <p>When configuring servlets via {@code web.xml}, users can add this filter to the requested
 * servlets.
 *
 * <p>When configuring servlets via annotations, users can extend this class and mark the subtype
 * with the {@code javax.servlet.annotation.WebFilter} annotation.
 */
public class AllowAnyOriginFilter implements Filter {

    /**
     * The human readable name of this filter.
     */
    public static final String NAME = "CORS filter";

    /**
     * A URL pattern which matches all the URLs.
     */
    public static final String ANY_URL = "*";

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        appendHeaders((HttpServletResponse) response);
        chain.doFilter(request, response);
    }

    private static void appendHeaders(HttpServletResponse response) {
        for (ResponseHeader header : ResponseHeader.values()) {
            header.appendTo(response);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // NOP.
    }

    @Override
    public void destroy() {
        // NOP.
    }

    /**
     * The HTTP headers which configure the cross-origin request handling.
     */
    private enum ResponseHeader {

        ACCESS_CONTROL_ALLOW_ORIGIN(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"),
        ACCESS_CONTROL_ALLOW_CREDENTIALS(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"),
        ACCESS_CONTROL_ALLOW_HEADERS(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, CONTENT_TYPE);

        private final String name;
        private final String value;

        ResponseHeader(String name, String value) {
            this.name = name;
            this.value = value;
        }

        /**
         * Appends this header to the ginen {@link HttpServletResponse}.
         */
        private void appendTo(HttpServletResponse response) {
            response.addHeader(name, value);
        }
    }
}
