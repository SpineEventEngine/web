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
import com.google.common.collect.ImmutableSet;
import com.google.common.net.MediaType;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterators.asEnumeration;
import static com.google.common.collect.Iterators.singletonIterator;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Collections.emptyIterator;

/**
 * A mocked servlet request with pre-defined {@code content}, {@code type} and {@code headers}.
 *
 * <p>In most cases this implementation should be sufficient enough for local tests.
 */
@SuppressWarnings("UnstableApiUsage") // we're OK using Guava's beta APIs
public final class FixedContentRequest implements MockedRequest {

    private static final String CONTENT_TYPE = "Content-Type";

    private final ImmutableMap<String, String> headers;
    private final byte[] content;
    private final MediaType type;

    private FixedContentRequest(ImmutableMap<String, String> headers,
                                byte[] content,
                                MediaType type) {
        this.headers = headers;
        this.content = content;
        this.type = type;
    }

    /**
     * Creates a new mocked request with specified {@code content} and default
     * {@linkplain MediaType#ANY_TYPE any} type.
     */
    public static FixedContentRequest create(String content) {
        checkNotNull(content);
        return create(content, MediaType.ANY_TYPE);
    }

    /**
     * Creates a new mocked request with specified {@code content} and {@code type}.
     */
    public static FixedContentRequest create(String content, MediaType type) {
        checkNotNull(content);
        checkNotNull(type);
        return create(content, type, contentTypeHeader(type));
    }

    /**
     * Creates a new mocked request with specified {@code content}, {@code type}
     * and {@code headers}.
     */
    public static FixedContentRequest create(String content,
                                             MediaType type,
                                             ImmutableMap<String, String> headers) {
        checkNotNull(content);
        checkNotNull(type);
        checkNotNull(headers);
        return create(content.getBytes(defaultCharset()), type, headers);
    }

    /**
     * Creates a new mocked request with specified {@code content}, {@code type}
     * and {@code headers}.
     */
    public static FixedContentRequest create(byte[] content,
                                             MediaType type,
                                             ImmutableMap<String, String> headers) {
        checkNotNull(content);
        checkNotNull(type);
        checkNotNull(headers);
        return new FixedContentRequest(headers, content, type);
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
    public @Nullable String getContentType() {
        return type.toString();
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content)));
    }
}
