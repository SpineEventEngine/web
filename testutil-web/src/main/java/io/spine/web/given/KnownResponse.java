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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.PrintWriter;
import java.io.Writer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A mocked servlet response with pre-defined {@code writer}, {@code status} and {@code headers}.
 *
 * @implNote The response is effectively immutable and does not pay attention to any
 *         modification attempts. Such a mocked implementation may be used for tests where
 *         one do not care if anything may be adjusted in the response while the response
 *         is being created.
 *         See {@linkplain MemoizingResponse memoizing response} if mutability is required.
 */
public final class KnownResponse implements MockedResponse {

    private final ImmutableMap<String, String> headers;
    private final int status;
    private final Writer writer;

    private KnownResponse(ImmutableMap<String, String> headers, Writer writer, int status) {
        this.headers = headers;
        this.writer = writer;
        this.status = status;
    }

    /**
     * Creates a new mocked response with specified {@code writer} and default
     * {@linkplain #SC_OK OK} status.
     */
    public static KnownResponse create(Writer writer) {
        checkNotNull(writer);
        return create(writer, SC_OK);
    }

    /**
     * Creates a new mocked response with specified {@code writer} and {@code status}.
     */
    public static KnownResponse create(Writer writer, int status) {
        checkNotNull(writer);
        return create(writer, status, ImmutableMap.of());
    }

    /**
     * Creates a new mocked response with specified {@code writer}, {@code status}
     * and {@code headers}.
     */
    public static KnownResponse create(Writer writer,
                                       int status,
                                       ImmutableMap<String, String> headers) {
        checkNotNull(writer);
        checkNotNull(headers);
        return new KnownResponse(headers, writer, status);
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public @Nullable String getHeader(String name) {
        return headers.get(name);
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
    public ImmutableSet<String> getHeaderNames() {
        return headers.keySet();
    }

    @Override
    public PrintWriter getWriter() {
        return new PrintWriter(writer);
    }
}
