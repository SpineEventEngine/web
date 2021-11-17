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

import io.spine.internal.gradle.java.publish.publish
import io.spine.internal.gradle.js.JsEnvironment
import io.spine.internal.gradle.js.buildJs
import io.spine.internal.gradle.js.prepareJsPublication
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.create

/**
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

    // TODO("Re-consider visibility and kdoc.")

    prepareJsPublication()
    publishJsLocally()

    publish.dependsOn(
        publishJs()
    )
}

private fun JsTaskRegistering.prepareJsPublication() =
    create<Copy>("prepareJsPublication") {

        description = "Prepares the NPM package for publishing."
        group = jsPublishTask

        dependsOn(buildJs)
    }

private fun JsTaskRegistering.publishJsLocally() =
    create("publishJsLocally") {

        description = "Publishes the NPM package locally with `npm link`."
        group = jsPublishTask

        doLast {
            publicationDirectory.npm("link")
        }

        dependsOn(prepareJsPublication)
    }

private fun JsTaskRegistering.publishJs() =
    create("publishJs") {

        description = "Publishes the NPM package with `npm publish`."
        group = jsPublishTask

        doLast {
            publicationDirectory.npm("publish")
        }

        dependsOn(prepareJsPublication)
    }
