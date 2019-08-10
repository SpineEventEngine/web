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

package io.spine.web.firebase.rest;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
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
class HttpClient {

    /**
     * The header which enables correct parsing of query parameters in request.
     *
     * <p>See Firebase REST API
     * <a href="https://firebase.google.com/docs/reference/rest/database/#section-api-usage">
     * reference</a>.
     */
    private static final String FIREBASE_DECODING_HEADER = "X-Firebase-Decoding";

    private final HttpRequestFactory requestFactory;

    private HttpClient(HttpRequestFactory requestFactory) {
        this.requestFactory = requestFactory;
    }

    /**
     * Creates a new {@code HttpClient} which will use the specified HTTP request factory.
     *
     * @param requestFactory
     *         the {@code HttpRequestFactory} to use for HTTP requests execution
     * @return the new instance of {@code HttpClient}
     */
    static HttpClient using(HttpRequestFactory requestFactory) {
        checkNotNull(requestFactory);
        return new HttpClient(requestFactory);
    }

    /**
     * Prepares and executes a GET request.
     *
     * @param url
     *         the target URL
     * @return the {@code String} containing response body
     * @throws RequestToFirebaseFailedException
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
     * @throws RequestToFirebaseFailedException
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
     * @throws RequestToFirebaseFailedException
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

    /**
     * Prepares and executes a DELETE request.
     *
     * @param url
     *         the target URL
     * @return the {@code String} containing response body
     * @throws RequestToFirebaseFailedException
     *         if the request couldn't be performed normally
     */
    @CanIgnoreReturnValue
    String delete(GenericUrl url) {
        checkNotNull(url);

        try {
            return doDelete(url);
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

    private String doDelete(GenericUrl url) throws IOException {
        HttpRequest request = requestFactory.buildDeleteRequest(url);
        return executeAndGetResponse(request);
    }

    private static String executeAndGetResponse(HttpRequest request) throws IOException {
        setFirebaseDecodingHeader(request);
        HttpResponse httpResponse = request.execute();
        String response = httpResponse.parseAsString();
        httpResponse.disconnect();
        return response;
    }

    /**
     * Sets the "X-Firebase-Decoding" header which allows query parameters in URL to be parsed
     * correctly and be RFC-compliant.
     *
     * <p>See REST API
     * <a href="https://firebase.google.com/docs/reference/rest/database/#section-api-usage">
     * reference.</a>
     */
    private static void setFirebaseDecodingHeader(HttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        headers.put(FIREBASE_DECODING_HEADER, 1);
    }
}
