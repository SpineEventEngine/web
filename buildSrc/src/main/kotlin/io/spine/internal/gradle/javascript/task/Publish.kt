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

package io.spine.internal.gradle.javascript.task

import io.spine.internal.gradle.java.publish.publish
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.getByName

/**
 * Registers tasks for publishing a JavaScript module as a package.
 *
 * List of tasks to be created:
 *
 *  1. [publishJs][publishJs];
 *  2. [publishJsLocally][publishJsLocally];
 *  3. [prepareJsPublication][prepareJsPublication].
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

    prepareJsPublication()
    publishJsLocally()

    publish.dependsOn(
        publishJs()
    )
}


/**
 * Locates `transpileSources` task in this [TaskContainer].
 *
 * The task transpiles JavaScript sources before publishing them to NPM.
 */
internal val TaskContainer.transpileSources: Task
    get() = getByName("transpileSources")

private fun JsTaskRegistering.transpileSources() =
    create("transpileSources") {

        description = "Transpiles sources before publishing."
        group = jsAnyTask

        doLast {
            val parserJsPresent = publicationDir
                .resolve("client")
                .resolve("parser")
                .resolve("object-parser.js")

            println("HELL_HELL_HELL | before transpile in pubDir object-parser.js: ${parserJsPresent.exists()}")

            npm("run", "transpile-before-publish")

            println("HELL_HELL_HELL | after transpile in pubDir object-parser.js: ${parserJsPresent.exists()}")
        }
    }


/**
 * Locates `prepareJsPublication` task in this [TaskContainer].
 *
 * This is a lifecycle task that prepares the NPM package for publishing in
 * [publicationDirectory][io.spine.internal.gradle.javascript.JsEnvironment.publicationDir]
 * of the current `JsEnvironment`.
 *
 * Does nothing by default, so a user should configure this task to copy all
 * required files to that directory.
 */
internal val TaskContainer.prepareJsPublication: Task
    get() = getByName("prepareJsPublication")

private fun JsTaskRegistering.prepareJsPublication() =
    create("prepareJsPublication") {

        description = "Prepares the NPM package for publishing."
        group = jsPublishTask

        project.copy {
            from(
                packageJson,
                npmrc
            )

            into(publicationDir)
        }

        dependsOn(
            assembleJs,
            transpileSources(),
        )
    }


/**
 * Locates `publishJsLocally` task in this [TaskContainer].
 *
 * The task publishes locally the prepared NPM package
 * from [publicationDirectory][io.spine.internal.gradle.javascript.JsEnvironment.publicationDir]
 * with `npm link`.
 *
 * Depends on [prepareJsPublication][prepareJsPublication].
 *
 *  @see <a href="https://docs.npmjs.com/cli/v8/commands/npm-link">npm-link | npm Docs</a>
 */
internal val TaskContainer.publishJsLocally: Task
    get() = getByName("publishJsLocally")

private fun JsTaskRegistering.publishJsLocally() =
    create("publishJsLocally") {

        description = "Publishes the NPM package locally with `npm link`."
        group = jsPublishTask

        doLast {
            val parserJsPresent = publicationDir
                .resolve("client")
                .resolve("parser")
                .resolve("object-parser.js")

            println("HELL_HELL_HELL | before link in pubDir object-parser.js: ${parserJsPresent.exists()}")

            publicationDir.npm("link")

            println("HELL_HELL_HELL | after link in pubDir object-parser.js: ${parserJsPresent.exists()}")

        }

        dependsOn(prepareJsPublication)
    }


/**
 * Locates `publishJs` task in this [TaskContainer].
 *
 * The task publishes the prepared NPM package from
 * [publicationDirectory][io.spine.internal.gradle.javascript.JsEnvironment.publicationDir]
 * with `npm publish`.
 *
 * Please note, in order to publish the NMP module, a valid
 * [npmAuthToken][io.spine.internal.gradle.javascript.JsEnvironment.npmAuthToken] should be
 * set. If no token is set, a default dummy value is quite enough for the local development.
 *
 * Depends on [prepareJsPublication][prepareJsPublication].
 *
 * @see <a href="https://docs.npmjs.com/cli/v7/commands/npm-publish">npm-publish | npm Docs</a>
 */
internal val TaskContainer.publishJs: Task
    get() = getByName("publishJs")

private fun JsTaskRegistering.publishJs() =
    create("publishJs") {

        description = "Publishes the NPM package with `npm publish`."
        group = jsPublishTask

        doLast {
            publicationDir.npm("publish")
        }

        dependsOn(prepareJsPublication)
    }
