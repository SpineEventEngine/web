/*
 * Copyright (c) 2000-2020 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

package io.spine.web.given;

import com.google.common.collect.ImmutableMap;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * A mock servlet request that returns a pre-defined request URI and headers.
 *
 * @apiNote Some of the methods are marked as {@linkplain Deprecated deprecated} to avoid
 *         the deprecation warnings, as their {@code super} methods are marked as such.
 */
public class MockedRequest implements HttpServletRequest {

    private final ImmutableMap<String, String> headers;
    private final String requestUri;

    private MockedRequest(Map<String, String> headers, String uri) {
        this.headers = ImmutableMap.copyOf(headers);
        this.requestUri = uri;
    }

    /**
     * Creates a new mock request.
     */
    public static ServletRequest create(String requestUri) {
        return create(ImmutableMap.of(), requestUri);
    }

    /**
     * Creates a new mock request with headers.
     */
    public static ServletRequest create(Map<String, String> headers, String requestUri) {
        return new MockedRequest(headers, requestUri);
    }

    @Override
    public String getRequestURI() {
        return requestUri;
    }

    // All methods below are intentionally no-op.

    @Override
    public @Nullable String getAuthType() {
        return null;
    }

    @Override
    public @Nullable Cookie[] getCookies() {
        return null;
    }

    @Override
    public long getDateHeader(String name) {
        return 0;
    }

    @Override
    public @Nullable String getHeader(String name) {
        return headers.get(name);
    }

    @Override
    public @Nullable Enumeration<String> getHeaders(String name) {
        return null;
    }

    @Override
    public @Nullable Enumeration<String> getHeaderNames() {
        return null;
    }

    @Override
    public int getIntHeader(String name) {
        return 0;
    }

    @Override
    public @Nullable String getMethod() {
        return null;
    }

    @Override
    public @Nullable String getPathInfo() {
        return null;
    }

    @Override
    public @Nullable String getPathTranslated() {
        return null;
    }

    @Override
    public @Nullable String getContextPath() {
        return null;
    }

    @Override
    public @Nullable String getQueryString() {
        return null;
    }

    @Override
    public @Nullable String getRemoteUser() {
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public @Nullable Principal getUserPrincipal() {
        return null;
    }

    @Override
    public @Nullable String getRequestedSessionId() {
        return null;
    }

    @Override
    public @Nullable StringBuffer getRequestURL() {
        return null;
    }

    @Override
    public @Nullable String getServletPath() {
        return null;
    }

    @Override
    public @Nullable HttpSession getSession(boolean create) {
        return null;
    }

    @Override
    public @Nullable HttpSession getSession() {
        return null;
    }

    @Override
    public @Nullable String changeSessionId() {
        return null;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Deprecated
    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    @Override
    public boolean authenticate(HttpServletResponse response) {
        return false;
    }

    @Override
    public void login(String username, String password) {
    }

    @Override
    public void logout() {
    }

    @Override
    public @Nullable Collection<Part> getParts() {
        return null;
    }

    @Override
    public @Nullable Part getPart(String name) {
        return null;
    }

    @Override
    public @Nullable <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) {
        return null;
    }

    @Override
    public @Nullable Object getAttribute(String name) {
        return null;
    }

    @Override
    public @Nullable Enumeration<String> getAttributeNames() {
        return null;
    }

    @Override
    public @Nullable String getCharacterEncoding() {
        return null;
    }

    @Override
    public void setCharacterEncoding(String env) {
    }

    @Override
    public int getContentLength() {
        return 0;
    }

    @Override
    public long getContentLengthLong() {
        return 0;
    }

    @Override
    public @Nullable String getContentType() {
        return null;
    }

    @Override
    public @Nullable ServletInputStream getInputStream() {
        return null;
    }

    @Override
    public @Nullable String getParameter(String name) {
        return null;
    }

    @Override
    public @Nullable Enumeration<String> getParameterNames() {
        return null;
    }

    @Override
    public @Nullable String[] getParameterValues(String name) {
        return null;
    }

    @Override
    public @Nullable Map<String, String[]> getParameterMap() {
        return null;
    }

    @Override
    public @Nullable String getProtocol() {
        return null;
    }

    @Override
    public @Nullable String getScheme() {
        return null;
    }

    @Override
    public @Nullable String getServerName() {
        return null;
    }

    @Override
    public int getServerPort() {
        return 0;
    }

    @Override
    public @Nullable BufferedReader getReader() {
        return null;
    }

    @Override
    public @Nullable String getRemoteAddr() {
        return null;
    }

    @Override
    public @Nullable String getRemoteHost() {
        return null;
    }

    @Override
    public void setAttribute(String name, Object o) {
    }

    @Override
    public void removeAttribute(String name) {
    }

    @Override
    public @Nullable Locale getLocale() {
        return null;
    }

    @Override
    public @Nullable Enumeration<Locale> getLocales() {
        return null;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public @Nullable RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    @Deprecated
    @Override
    public @Nullable String getRealPath(String path) {
        return null;
    }

    @Override
    public int getRemotePort() {
        return 0;
    }

    @Override
    public @Nullable String getLocalName() {
        return null;
    }

    @Override
    public @Nullable String getLocalAddr() {
        return null;
    }

    @Override
    public int getLocalPort() {
        return 0;
    }

    @Override
    public @Nullable ServletContext getServletContext() {
        return null;
    }

    @Override
    public @Nullable AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    @Override
    public @Nullable AsyncContext
    startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IllegalStateException {
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public @Nullable AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public @Nullable DispatcherType getDispatcherType() {
        return null;
    }
}
