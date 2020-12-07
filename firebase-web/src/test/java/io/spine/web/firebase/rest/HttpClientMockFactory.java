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

package io.spine.web.firebase.rest;

import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;

import java.io.IOException;

/**
 * Creates mocked {@link HttpClient}s.
 */
final class HttpClientMockFactory {

    /**
     * Prevents instantiation of this utility class.
     */
    private HttpClientMockFactory() {
    }

    /**
     * Creates a new no-operation {@code HttpClient}.
     */
    static HttpClient noOpClient() {
        MockHttpTransport transport = new MockHttpTransport.Builder().build();
        return HttpClient.using(transport.createRequestFactory());
    }

    /**
     * Creates an {@code HttpClient} mock which returns the specified {@code content}
     * on every request.
     */
    static HttpClient mockHttpClient(String content) {
        return mockHttpClient(content, HttpClientMockFactory::noOpObserver);
    }

    /**
     * Creates an {@code HttpClient} mock which returns the specified {@code content}
     * on every request and uses supplied {@code observer} while building requests.
     */
    static HttpClient mockHttpClient(String content, RequestObserver observer) {
        return HttpClient.using(mockRequestFactory(content, observer));
    }

    private static HttpRequestFactory
    mockRequestFactory(String content, RequestObserver observer) {
        HttpTransport transportMock = mockHttpTransport(content, observer);
        HttpRequestFactory requestFactoryMock = transportMock.createRequestFactory();
        return requestFactoryMock;
    }

    private static HttpTransport
    mockHttpTransport(String content, RequestObserver observer) {
        final MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
        response.setContent(content);
        return new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest
            buildRequest(String method, String url) {
                MockLowLevelHttpRequest request = new MockLowLevelHttpRequest(url);
                request.setResponse(response);
                observer.onRequest(method, url);
                return request;
            }
        };
    }

    /**
     * Creates an {@code HttpClient} mock which throws {@link java.io.IOException} on every
     * request.
     */
    static HttpClient throwingClient() {
        return HttpClient.using(throwingRequestFactory());
    }

    private static HttpRequestFactory throwingRequestFactory() {
        HttpRequestFactory result = throwingHttpTransport().createRequestFactory();
        return result;
    }

    private static MockHttpTransport throwingHttpTransport() {
        return new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest
            buildRequest(String method, String url) throws IOException {
                throw new IOException("Intended exception");
            }
        };
    }

    private static void noOpObserver(String method, String url) {
    }

    /**
     * Observes built HTTP requests.
     */
    interface RequestObserver {

        /**
         * A callback that is executed whenever an HTTP request is built.
         *
         * @param method
         *         the HTTP method being used
         * @param url
         *         the request URL
         */
        void onRequest(String method, String url);
    }
}
