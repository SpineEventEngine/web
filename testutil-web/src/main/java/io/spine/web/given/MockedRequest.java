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
 * A mocked no-op servlet request.
 *
 * @apiNote Some of the methods are marked as {@linkplain Deprecated deprecated} to avoid
 *         the deprecation warnings, as their {@code super} methods are marked as such.
 */
@SuppressWarnings("PMD.UncommentedEmptyMethodBody") // default implementations are empty on purpose.
public interface MockedRequest extends HttpServletRequest {

    @Override
    default @Nullable String getRequestURI() {
        return null;
    }

    @Override
    default @Nullable String getAuthType() {
        return null;
    }

    @Override
    @SuppressWarnings("ReturnOfNull") // we're explicitly returning `null`.
    default @Nullable Cookie[] getCookies() {
        return null;
    }

    @Override
    default long getDateHeader(String name) {
        return 0;
    }

    @Override
    default @Nullable String getHeader(String name) {
        return null;
    }

    @Override
    default @Nullable Enumeration<String> getHeaders(String name) {
        return null;
    }

    @Override
    default @Nullable Enumeration<String> getHeaderNames() {
        return null;
    }

    @Override
    default int getIntHeader(String name) {
        return 0;
    }

    @Override
    default @Nullable String getMethod() {
        return null;
    }

    @Override
    default @Nullable String getPathInfo() {
        return null;
    }

    @Override
    default @Nullable String getPathTranslated() {
        return null;
    }

    @Override
    default @Nullable String getContextPath() {
        return null;
    }

    @Override
    default @Nullable String getQueryString() {
        return null;
    }

    @Override
    default @Nullable String getRemoteUser() {
        return null;
    }

    @Override
    default boolean isUserInRole(String role) {
        return false;
    }

    @Override
    default @Nullable Principal getUserPrincipal() {
        return null;
    }

    @Override
    default @Nullable String getRequestedSessionId() {
        return null;
    }

    @Override
    default @Nullable StringBuffer getRequestURL() {
        return null;
    }

    @Override
    default @Nullable String getServletPath() {
        return null;
    }

    @Override
    default @Nullable HttpSession getSession(boolean create) {
        return null;
    }

    @Override
    default @Nullable HttpSession getSession() {
        return null;
    }

    @Override
    default @Nullable String changeSessionId() {
        return null;
    }

    @Override
    default boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    default boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    @Override
    default boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Deprecated
    @Override
    default boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    @Override
    default boolean authenticate(HttpServletResponse response) {
        return false;
    }

    @Override
    default void login(String username, String password) {
    }

    @Override
    default void logout() {
    }

    @Override
    default @Nullable Collection<Part> getParts() {
        return null;
    }

    @Override
    default @Nullable Part getPart(String name) {
        return null;
    }

    @Override
    default @Nullable <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) {
        return null;
    }

    @Override
    default @Nullable Object getAttribute(String name) {
        return null;
    }

    @Override
    default @Nullable Enumeration<String> getAttributeNames() {
        return null;
    }

    @Override
    default @Nullable String getCharacterEncoding() {
        return null;
    }

    @Override
    default void setCharacterEncoding(String env) {
    }

    @Override
    default int getContentLength() {
        return 0;
    }

    @Override
    default long getContentLengthLong() {
        return 0;
    }

    @Override
    default @Nullable String getContentType() {
        return null;
    }

    @Override
    default @Nullable ServletInputStream getInputStream() {
        return null;
    }

    @Override
    default @Nullable String getParameter(String name) {
        return null;
    }

    @Override
    default @Nullable Enumeration<String> getParameterNames() {
        return null;
    }

    @Override
    @SuppressWarnings("ReturnOfNull") // we're explicitly returning `null`.
    default @Nullable String[] getParameterValues(String name) {
        return null;
    }

    @Override
    default @Nullable Map<String, String[]> getParameterMap() {
        return null;
    }

    @Override
    default @Nullable String getProtocol() {
        return null;
    }

    @Override
    default @Nullable String getScheme() {
        return null;
    }

    @Override
    default @Nullable String getServerName() {
        return null;
    }

    @Override
    default int getServerPort() {
        return 0;
    }

    @Override
    default @Nullable BufferedReader getReader() {
        return null;
    }

    @Override
    default @Nullable String getRemoteAddr() {
        return null;
    }

    @Override
    default @Nullable String getRemoteHost() {
        return null;
    }

    @Override
    default void setAttribute(String name, Object o) {
    }

    @Override
    default void removeAttribute(String name) {
    }

    @Override
    default @Nullable Locale getLocale() {
        return null;
    }

    @Override
    default @Nullable Enumeration<Locale> getLocales() {
        return null;
    }

    @Override
    default boolean isSecure() {
        return false;
    }

    @Override
    default @Nullable RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    @Deprecated
    @Override
    default @Nullable String getRealPath(String path) {
        return null;
    }

    @Override
    default int getRemotePort() {
        return 0;
    }

    @Override
    default @Nullable String getLocalName() {
        return null;
    }

    @Override
    default @Nullable String getLocalAddr() {
        return null;
    }

    @Override
    default int getLocalPort() {
        return 0;
    }

    @Override
    default @Nullable ServletContext getServletContext() {
        return null;
    }

    @Override
    default @Nullable AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    @Override
    default @Nullable AsyncContext
    startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IllegalStateException {
        return null;
    }

    @Override
    default boolean isAsyncStarted() {
        return false;
    }

    @Override
    default boolean isAsyncSupported() {
        return false;
    }

    @Override
    default @Nullable AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    default @Nullable DispatcherType getDispatcherType() {
        return null;
    }
}
