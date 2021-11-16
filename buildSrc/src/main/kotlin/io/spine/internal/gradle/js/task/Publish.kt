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

import io.spine.internal.gradle.js.JsEnvironment
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.create

/**
 * Registers [tasks][JsPublishTaskListing] for publishing JavaScript projects.
 *
 * In order to publish the NPM module, it is required that the `NPM_TOKEN` environment
 * variable is set to a valid [JsEnvironment.npmAuthToken]. If the token is not set,
 * a dummy value is quite enough for the local development.
 *
 * Usage example:
 *
 * ```
 * import io.spine.internal.gradle.js.javascript
 *
 * // ...
 *
 * js {
 *     tasks {
 *         register {
 *             publish()
 *         }
 *     }
 * }
 * ```
 */
fun JsTaskRegistering.publish() {

    prepareJsPublication()
    publishJsLocally()
    publishJs().also {
        publish.dependsOn(it)
    }
}

/**
 * Prepares the NPM package for publishing.
 *
 * Does nothing by default, so a user should configure this task to copy all
 * required files to the `publicationDirectory`.
 */
private fun JsTaskRegistering.prepareJsPublication() =
    create<Copy>("prepareJsPublication") {

        description = "Prepares the NPM package for publishing."
        group = jsPublishTask

        dependsOn(buildJs)
    }

/**
 * Publishes the NPM package locally with `npm link`.
 *
 * Package linking is a two-step process:
 *
 *  1. Create a global symlink for a dependency with `npm link`. A symlink, short for symbolic link,
 *     is a shortcut that points to another directory or file on your system.
 *  2. Tell the application to use the global symlink with `npm link some-dep`.
 *
 *  @see <a href="https://docs.npmjs.com/cli/v8/commands/npm-link">npm-link | npm Docs</a>
 */
private fun JsTaskRegistering.publishJsLocally() =
    create("publishJsLocally") {

        description = "Publishes the NPM package locally with `npm link`."
        group = jsPublishTask

        doLast {
            publicationDirectory.npm("link")
        }

        dependsOn(prepareJsPublication)
    }

private fun JsTaskRegistering.publishJs() = create("publishJs") {

    description = "Publishes the NPM package with `npm publish`."
    group = jsPublishTask

    doLast {
        publicationDirectory.npm("publish")
    }

    dependsOn(prepareJsPublication)
}
