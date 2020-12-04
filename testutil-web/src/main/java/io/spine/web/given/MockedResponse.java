/*
 * Copyright (c) 2000-2020 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

package io.spine.web.given;

import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

/**
 * A mock servlet request that returns pre-defined request URI.
 *
 * @apiNote Some of the methods are marked as {@linkplain Deprecated deprecated} to avoid
 *         the deprecation warnings, as their {@code super} methods are marked as such.
 */
public class MockedResponse implements HttpServletResponse {

    private final int status;

    private MockedResponse(int status) {
        this.status = status;
    }

    /**
     * Creates a mock response that always returns the given status.
     */
    public static MockedResponse create(int status) {
        return new MockedResponse(status);
    }

    @Override
    public int getStatus() {
        return status;
    }

    // All methods below are intentionally no-op.

    @Override
    public void addCookie(Cookie cookie) {
    }

    @Override
    public boolean containsHeader(String name) {
        return false;
    }

    @Override
    public @Nullable String encodeURL(String url) {
        return null;
    }

    @Override
    public @Nullable String encodeRedirectURL(String url) {
        return null;
    }

    @Deprecated
    @Override
    public @Nullable String encodeUrl(String url) {
        return null;
    }

    @Deprecated
    @Override
    public @Nullable String encodeRedirectUrl(String url) {
        return null;
    }

    @Override
    public void sendError(int sc, String msg) {
    }

    @Override
    public void sendError(int sc) {
    }

    @Override
    public void sendRedirect(String location) {
    }

    @Override
    public void setDateHeader(String name, long date) {
    }

    @Override
    public void addDateHeader(String name, long date) {
    }

    @Override
    public void setHeader(String name, String value) {
    }

    @Override
    public void addHeader(String name, String value) {
    }

    @Override
    public void setIntHeader(String name, int value) {
    }

    @Override
    public void addIntHeader(String name, int value) {
    }

    @Override
    public void setStatus(int sc) {
    }

    @Deprecated
    @Override
    public void setStatus(int sc, String sm) {
    }

    @Override
    public @Nullable String getHeader(String name) {
        return null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return ImmutableList.of();
    }

    @Override
    public Collection<String> getHeaderNames() {
        return ImmutableList.of();
    }

    @Override
    public @Nullable String getCharacterEncoding() {
        return null;
    }

    @Override
    public @Nullable String getContentType() {
        return null;
    }

    @Override
    public @Nullable ServletOutputStream getOutputStream() {
        return null;
    }

    @Override
    public @Nullable PrintWriter getWriter() {
        return null;
    }

    @Override
    public void setCharacterEncoding(String charset) {
    }

    @Override
    public void setContentLength(int len) {
    }

    @Override
    public void setContentLengthLong(long len) {
    }

    @Override
    public void setContentType(String type) {
    }

    @Override
    public void setBufferSize(int size) {
    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public void flushBuffer() {
    }

    @Override
    public void resetBuffer() {
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void setLocale(Locale loc) {
    }

    @Override
    public @Nullable Locale getLocale() {
        return null;
    }
}
