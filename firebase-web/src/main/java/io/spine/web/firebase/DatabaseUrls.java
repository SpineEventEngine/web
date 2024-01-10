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

package io.spine.web.firebase;

import io.spine.net.Urls;

import static io.spine.util.Preconditions2.checkNotEmptyOrBlank;

/**
 * Utilities and static factories dealing with {@link DatabaseUrl}.
 */
public final class DatabaseUrls {

    /**
     * Prevents instantiation of this utility class.
     */
    private DatabaseUrls() {
    }

    /**
     * Creates a {@code DatabaseUrl} instance from the given string.
     *
     * @param dbUrl
     *         a {@code String} containing database URL
     * @return a new instance of {@code DatabaseUrl}
     * @see com.google.firebase.database.util.EmulatorHelper
     */
    public static DatabaseUrl from(String dbUrl) {
        checkNotEmptyOrBlank(dbUrl);
        var namespace = "";
        var url = dbUrl;
        var namespaceQuery = "?ns=";
        var queryIndex = dbUrl.indexOf(namespaceQuery);
        if (queryIndex > 0) {
            namespace = dbUrl.substring(queryIndex + namespaceQuery.length());
            url = dbUrl.substring(0, queryIndex);
        }
        return DatabaseUrl
                .newBuilder()
                .setUrl(Urls.create(url))
                .setNamespace(namespace)
                .build();
    }
}
