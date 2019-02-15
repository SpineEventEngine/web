/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import org.apache.commons.validator.routines.UrlValidator;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A Firebase database URL.
 */
public class DatabaseUrls {

    private DatabaseUrls() {
    }

    /**
     * Creates a {@code DatabaseUrls} instance from the given string.
     *
     * <p>The given string should be a valid URL by the Apache
     * {@link org.apache.commons.validator.routines.UrlValidator} standards.
     *
     * @param url
     *         a {@code String} containing database URL
     * @return a new instance of {@code DatabaseUrls}
     */
    public static DatabaseUrl from(String url) {
        validate(url);
        return DatabaseUrlVBuilder
                .newBuilder()
                .setUrl(Urls.create(url))
                .build();
    }

    public static void checkSpec(DatabaseUrl url) {
        String spec = url.getUrl()
                         .getSpec();
        validate(spec);
    }

    private static void validate(String url) {
        UrlValidator urlValidator = UrlValidator.getInstance();
        checkArgument(urlValidator.isValid(url), "The specified database URL %s is invalid");
    }
}
