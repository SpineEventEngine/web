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

package io.spine.internal.gradle.javascript.task.impl

import io.spine.internal.gradle.java.publish.publish
import io.spine.internal.gradle.javascript.task.buildJs
import io.spine.internal.gradle.javascript.task.prepareJsPublication
import io.spine.internal.gradle.javascript.task.transpileSources
import io.spine.internal.gradle.javascript.task.JsTaskRegistering
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.create

/**
 * Registers tasks for publishing a JavaScript module as a package.
 *
 * List of tasks to be created:
 *
 *  1. [publishJs][io.spine.internal.gradle.javascript.task.publishJs];
 *  2. [publishJsLocally][io.spine.internal.gradle.javascript.task.publishJsLocally];
 *  3. [prepareJsPublication][io.spine.internal.gradle.javascript.task.prepareJsPublication].
 *
 * Usage example:
 *
 * ```
 * import io.spine.internal.gradle.js.javascript
 * import io.spine.internal.gradle.js.task.impl.publish
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

    transpileSources()
    prepareJsPublication()
    publishJsLocally()

    publish.dependsOn(
        publishJs()
    )
}

private fun JsTaskRegistering.transpileSources() =
    create("transpileSources") {

        description = "Transpiles sources before publishing."
        group = jsAnyTask

        doLast {
            npm("run", "transpile-before-publishing")
        }
    }

private fun JsTaskRegistering.prepareJsPublication() =
    create<Copy>("prepareJsPublication") {

        description = "Prepares the NPM package for publishing."
        group = jsPublishTask

        from(
            packageJson,
            npmrc
        )

        into(publicationDir)

        dependsOn(

            // TODO:2019-02-05:dmytro.grankin: Temporarily don't publish a bundle.
            // See: https://github.com/SpineEventEngine/web/issues/61

            //copyBundledJs,

            buildJs,
            transpileSources
        )
    }

private fun JsTaskRegistering.publishJsLocally() =
    create("publishJsLocally") {

        description = "Publishes the NPM package locally with `npm link`."
        group = jsPublishTask

        doLast {
            publicationDir.npm("link")
        }

        dependsOn(prepareJsPublication)
    }

private fun JsTaskRegistering.publishJs() =
    create("publishJs") {

        description = "Publishes the NPM package with `npm publish`."
        group = jsPublishTask

        doLast {
            publicationDir.npm("publish")
        }

        dependsOn(prepareJsPublication)
    }
