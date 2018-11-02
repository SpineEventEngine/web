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

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A tool to create and execute HTTP requests.
 *
 * <p>All responses are returned in a {@code String} form.
 *
 * <p>The class is effectively {@code final} and is left non-{@code final} to enable testing mocks.
 */
class HttpRequestExecutor {

    private final HttpRequestFactory requestFactory;

    private HttpRequestExecutor(HttpRequestFactory requestFactory) {
        this.requestFactory = requestFactory;
    }

    /**
     * Creates a new {@code HttpRequestExecutor} which will use the specified HTTP transport.
     *
     * @param transport
     *         the underlying {@code HttpTransport} to use
     * @return the new instance of {@code HttpRequestExecutor}
     */
    static HttpRequestExecutor using(HttpTransport transport) {
        checkNotNull(transport);
        HttpRequestFactory requestFactory = transport.createRequestFactory();
        return new HttpRequestExecutor(requestFactory);
    }

    /**
     * Prepares and executes a GET request.
     *
     * @param url
     *         the target URL
     * @return the new instance of {@code HttpRequestExecutor}
     * @throws java.lang.IllegalStateException
     *         if the request couldn't be performed normally
     */
    String get(GenericUrl url) {
        checkNotNull(url);
        try {
            return doGet(url);
        } catch (IOException e) {
            throw new RequestToFirebaseFailedException(e.getMessage(), e);
        }
    }

    /**
     * Prepares and executes a PUT request.
     *
     * @param url
     *         the target URL
     * @param content
     *         the body of the request
     * @return the {@code String} containing response body
     * @throws java.lang.IllegalStateException
     *         if the request couldn't be performed normally
     */
    @CanIgnoreReturnValue
    String put(GenericUrl url, HttpContent content) {
        checkNotNull(url);
        checkNotNull(content);
        try {
            return doPut(url, content);
        } catch (IOException e) {
            throw new RequestToFirebaseFailedException(e.getMessage(), e);
        }
    }

    /**
     * Prepares and executes a PATCH request.
     *
     * @param url
     *         the target URL
     * @param content
     *         the body of the request
     * @return the {@code String} containing response body
     * @throws java.lang.IllegalStateException
     *         if the request couldn't be performed normally
     */
    @CanIgnoreReturnValue
    String patch(GenericUrl url, HttpContent content) {
        checkNotNull(url);
        checkNotNull(content);
        try {
            return doPatch(url, content);
        } catch (IOException e) {
            throw new RequestToFirebaseFailedException(e.getMessage(), e);
        }
    }

    private String doGet(GenericUrl url) throws IOException {
        HttpRequest request = requestFactory.buildGetRequest(url);
        return executeAndGetResponse(request);
    }

    private String doPut(GenericUrl url, HttpContent content) throws IOException {
        HttpRequest request = requestFactory.buildPutRequest(url, content);
        return executeAndGetResponse(request);
    }

    private String doPatch(GenericUrl url, HttpContent content) throws IOException {
        HttpRequest request = requestFactory.buildPatchRequest(url, content);
        return executeAndGetResponse(request);
    }

    private static String executeAndGetResponse(HttpRequest request) throws IOException {
        HttpResponse httpResponse = request.execute();
        String response = httpResponse.parseAsString();
        httpResponse.disconnect();
        return response;
    }
}
