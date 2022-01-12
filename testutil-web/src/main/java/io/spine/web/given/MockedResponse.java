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

import org.checkerframework.checker.nullness.qual.Nullable;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

/**
 * A mocked no-op servlet response.
 *
 * @apiNote Some of the methods are marked as {@linkplain Deprecated deprecated} to avoid
 *         the deprecation warnings, as their {@code super} methods are marked as such.
 */
@SuppressWarnings("PMD.UncommentedEmptyMethodBody") // default implementations are empty on purpose.
public interface MockedResponse extends HttpServletResponse {

    @Override
    default int getStatus() {
        return 0;
    }

    @Override
    default void addCookie(Cookie cookie) {
    }

    @Override
    default boolean containsHeader(String name) {
        return false;
    }

    @Override
    default @Nullable String encodeURL(String url) {
        return null;
    }

    @Override
    default @Nullable String encodeRedirectURL(String url) {
        return null;
    }

    @Deprecated
    @Override
    default @Nullable String encodeUrl(String url) {
        return null;
    }

    @Deprecated
    @Override
    default @Nullable String encodeRedirectUrl(String url) {
        return null;
    }

    @Override
    default void sendError(int sc, String msg) {
    }

    @Override
    default void sendError(int sc) {
    }

    @Override
    default void sendRedirect(String location) {
    }

    @Override
    default void setDateHeader(String name, long date) {
    }

    @Override
    default void addDateHeader(String name, long date) {
    }

    @Override
    default void setHeader(String name, String value) {
    }

    @Override
    default void addHeader(String name, String value) {
    }

    @Override
    default void setIntHeader(String name, int value) {
    }

    @Override
    default void addIntHeader(String name, int value) {
    }

    @Override
    default void setStatus(int sc) {
    }

    @Deprecated
    @Override
    default void setStatus(int sc, String sm) {
    }

    @Override
    default @Nullable String getHeader(String name) {
        return null;
    }

    @Override
    default @Nullable Collection<String> getHeaders(String name) {
        return null;
    }

    @Override
    default @Nullable Collection<String> getHeaderNames() {
        return null;
    }

    @Override
    default @Nullable String getCharacterEncoding() {
        return null;
    }

    @Override
    default @Nullable String getContentType() {
        return null;
    }

    @Override
    default @Nullable ServletOutputStream getOutputStream() {
        return null;
    }

    @Override
    default @Nullable PrintWriter getWriter() {
        return null;
    }

    @Override
    default void setCharacterEncoding(String charset) {
    }

    @Override
    default void setContentLength(int len) {
    }

    @Override
    default void setContentLengthLong(long len) {
    }

    @Override
    default void setContentType(String type) {
    }

    @Override
    default void setBufferSize(int size) {
    }

    @Override
    default int getBufferSize() {
        return 0;
    }

    @Override
    default void flushBuffer() {
    }

    @Override
    default void resetBuffer() {
    }

    @Override
    default boolean isCommitted() {
        return false;
    }

    @Override
    default void reset() {
    }

    @Override
    default void setLocale(Locale loc) {
    }

    @Override
    default @Nullable Locale getLocale() {
        return null;
    }
}
