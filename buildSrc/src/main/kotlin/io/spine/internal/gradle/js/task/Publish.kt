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

package io.spine.internal.gradle.js.task

import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.create

/**
 * ...
 */
fun JsTaskRegistering.publish() {

    prepareJsPublication()
    link()
    publishJs().also {
        publish.dependsOn(it)
    }
}

/**
 * Prepares JS artifacts for publication.
 *
 * Does nothing by default, so a user should configure this task to copy all
 * required files to the `publicationDirectory`.
 */
private fun JsTaskRegistering.prepareJsPublication() =
    create<Copy>("prepareJsPublication1") {

        description = "Prepares the NPM package for publishing."
        group = jsPublishTask

        dependsOn("buildJs1")
    }

private fun JsTaskRegistering.link() =
    create("link1") {

        description = "Publishes the NPM package locally with `npm link`"
        group = jsPublishTask

        doLast {
            publicationDirectory.npm("link")
        }

        dependsOn("prepareJsPublication1")
    }

private fun JsTaskRegistering.publishJs() =
    create("publishJs1") {

        description = "Publishes the NPM package with `npm publish`."
        group = jsPublishTask

        doLast {
            publicationDirectory.npm("publish")
        }

        dependsOn("prepareJsPublication1")
    }
