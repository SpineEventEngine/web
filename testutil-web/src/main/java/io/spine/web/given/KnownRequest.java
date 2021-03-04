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
import com.google.common.collect.ImmutableSet;
import com.google.common.net.MediaType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterators.asEnumeration;
import static com.google.common.collect.Iterators.singletonIterator;
import static java.util.Collections.emptyIterator;

/**
 * A mocked servlet request with pre-defined {@code content}, {@code type} and {@code headers}.
 *
 * @implNote The request is effectively immutable and does not pay attention to any
 *         modification attempts. Such a mocked implementation may be used for tests where
 *         one do not care if anything may be adjusted in the request while the request
 *         is being processed.
 */
@SuppressWarnings("UnstableApiUsage") // we're OK using Guava's beta APIs
public final class KnownRequest implements MockedRequest {

    private static final String CONTENT_TYPE = "Content-Type";

    private final ImmutableMap<String, String> headers;
    private final byte[] content;
    private final MediaType type;
    private final String uri;

    private KnownRequest(Builder builder) {
        this.headers = builder.headers;
        this.content = builder.content;
        this.type = builder.type;
        this.uri = builder.uri;
    }

    /**
     * Creates a new mocked request with specified {@code content} and default
     * {@linkplain MediaType#ANY_TYPE any} type.
     */
    public static KnownRequest create(String content) {
        checkNotNull(content);
        return create(content, MediaType.ANY_TYPE);
    }

    /**
     * Creates a new mocked request with specified {@code content} and {@code type}.
     */
    public static KnownRequest create(String content, MediaType type) {
        checkNotNull(content);
        checkNotNull(type);
        return newBuilder()
                .withContent(content)
                .withType(type)
                .withHeaders(contentTypeHeader(type))
                .build();
    }

    private static ImmutableMap<String, String> contentTypeHeader(MediaType type) {
        return ImmutableMap.of(CONTENT_TYPE, type.toString());
    }

    @Override
    public @Nullable String getHeader(String name) {
        return headers.get(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        String result = headers.get(name);
        if (result == null) {
            return asEnumeration(emptyIterator());
        }
        return asEnumeration(singletonIterator(result));
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        ImmutableSet<String> names = headers.keySet();
        return asEnumeration(names.iterator());
    }

    @Override
    public int getContentLength() {
        return content.length;
    }

    @Override
    public long getContentLengthLong() {
        return content.length;
    }

    @Override
    public @NonNull String getContentType() {
        return type.toString();
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(
                new InputStreamReader(
                        new ByteArrayInputStream(content), StandardCharsets.UTF_8
                )
        );
    }

    @Override
    public @NonNull String getRequestURI() {
        return uri;
    }

    /**
     * Creates an empty request.
     */
    public static KnownRequest empty() {
        return newBuilder().build();
    }

    /**
     * Creates a new request builder.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * The request builder.
     */
    public static class Builder {

        private ImmutableMap<String, String> headers = ImmutableMap.of();
        private MediaType type = MediaType.ANY_TYPE;
        private byte[] content = "".getBytes(StandardCharsets.UTF_8);
        private String uri = "";

        /**
         * Prevents instantiation outside of the class.
         *
         * @see #newBuilder()
         */
        private Builder() {
        }

        /**
         * Sets the request content.
         */
        public Builder withContent(String content) {
            checkNotNull(content);
            this.content = content.getBytes(StandardCharsets.UTF_8);
            return this;
        }

        /**
         * Sets the request content bytes.
         */
        public Builder withBinaryContent(byte[] content) {
            checkNotNull(content);
            this.content = content.clone();
            return this;
        }

        /**
         * Sets the request headers.
         */
        public Builder withHeaders(Map<String, String> headers) {
            checkNotNull(headers);
            this.headers = ImmutableMap.copyOf(headers);
            return this;
        }

        /**
         * Sets the request media type.
         */
        public Builder withType(MediaType type) {
            this.type = checkNotNull(type);
            return this;
        }

        /**
         * Sets the request URI.
         */
        public Builder withUri(String uri) {
            this.uri = checkNotNull(uri);
            return this;
        }

        /**
         * Creates a request out of this builder.
         */
        public KnownRequest build() {
            return new KnownRequest(this);
        }
    }
}
