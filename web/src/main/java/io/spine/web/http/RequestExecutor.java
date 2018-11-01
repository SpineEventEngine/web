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

package io.spine.web.http;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.io.IOException;

import static io.spine.util.Exceptions.newIllegalStateException;

public final class RequestExecutor {

    private final HttpRequestFactory requestFactory;

    private RequestExecutor(HttpRequestFactory requestFactory) {
        this.requestFactory = requestFactory;
    }

    public static RequestExecutor using(HttpTransport transport) {
        HttpRequestFactory requestFactory = transport.createRequestFactory();
        return new RequestExecutor(requestFactory);
    }

    public String get(GenericUrl url) {
        try {
            return doGet(url);
        } catch (IOException e) {
            throw newIllegalStateException(e, e.getMessage());
        }
    }

    @CanIgnoreReturnValue
    public String put(GenericUrl url, HttpContent content) {
        try {
            return doPut(url, content);
        } catch (IOException e) {
            throw newIllegalStateException(e, e.getMessage());
        }
    }

    @CanIgnoreReturnValue
    public String patch(GenericUrl url, HttpContent content) {
        try {
            return doPatch(url, content);
        } catch (IOException e) {
            throw newIllegalStateException(e, e.getMessage());
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
