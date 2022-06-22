/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import com.google.common.collect.ImmutableSet;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A mocked response that attempts to save all the actions done to it.
 *
 * @implNote The response mutates its state and holds the latest changes. Such an
 *         implementation may be useful to verify that e.g. a particular error code was set
 *         to the response or a particular content was written.
 *         See {@linkplain KnownResponse known response} if an immutable implementation is required.
 */
public final class MemoizingResponse implements MockedResponse {

    private final StringWriter writer = new StringWriter();
    private final Map<String, String> headers = new HashMap<>();
    private String contentType = "";
    private int status = -1;

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public @Nullable String getHeader(String name) {
        return headers.get(name);
    }

    @Override
    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    @Override
    public void sendError(int sc) {
        status = sc;
    }

    @Override
    public void sendError(int sc, String msg) {
        sendError(sc);
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public @Nullable String getContentType() {
        return contentType;
    }

    @Override
    public ImmutableSet<String> getHeaders(String name) {
        String result = headers.get(name);
        if (result == null) {
            return ImmutableSet.of();
        }
        return ImmutableSet.of(result);
    }

    @Override
    public Set<String> getHeaderNames() {
        return headers.keySet();
    }

    @Override
    public PrintWriter getWriter() {
        return new PrintWriter(writer);
    }

    /**
     * Returns the content of the associated response writer.
     */
    public String writerContent() {
        return writer.toString();
    }
}
