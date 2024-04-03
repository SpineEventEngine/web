/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.internal.gradle.typescript.task

import io.spine.internal.gradle.publish.publish
import io.spine.internal.gradle.named
import io.spine.internal.gradle.register
import io.spine.internal.gradle.TaskName
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

/**
 * Registers tasks for publishing a TypeScript module.
 *
 * Please note, this task group depends on [assemble] tasks. Therefore, assembling tasks should
 * be applied in the first place.
 *
 * List of tasks to be created:
 *
 *  1. [TaskContainer.publishTs].
 *  2. [TaskContainer.publishTsLocally].
 *  3. [TaskContainer.prepareTsPublication].
 *
 * Here's an example of how to apply it in `build.gradle.kts`:
 *
 * ```
 * import io.spine.internal.gradle.typescript.typescript
 * import io.spine.internal.gradle.typescript.task.assemble
 * import io.spine.internal.gradle.typescript.task.publish
 *
 * // ...
 *
 * typescript {
 *     tasks {
 *         assemble()
 *         publish()
 *     }
 * }
 * ```
 */
fun TsTasks.publish() {

    // TODO:2024-02-28:alex.tymchenko: do we need transpiling altogether?
    transpileTsSources()
    prepareTsPublication()
    publishTsLocally()

    publishTs().also {
        publish.configure {
            dependsOn(it)
        }
    }
}

private val transpileTsSourcesName = TaskName.of("transpileTsSources")

/**
 * Locates `transpileTsSources` task in this [TaskContainer].
 *
 * The task transpiles TypeScript sources using Babel before their publishing.
 */
val TaskContainer.transpileTsSources: TaskProvider<Task>
    get() = named(transpileTsSourcesName)

private fun TsTasks.transpileTsSources() =
    register(transpileTsSourcesName) {

        description = "Transpiles TypeScript sources using Babel before their publishing."
        group = TsTasks.Group.publish

        doLast {
            npm("run", "transpile-before-publish")
        }
    }

private val prepareTsPublicationName = TaskName.of("prepareTsPublication")

/**
 * Locates `prepareTsPublication` task in this [TaskContainer].
 *
 * This is a lifecycle task that prepares the NPM package in
 * [publicationDirectory][io.spine.internal.gradle.typescript.TsEnvironment.publicationDir]
 * of the current `TsEnvironment`.
 */
val TaskContainer.prepareTsPublication: TaskProvider<Task>
    get() = named(prepareTsPublicationName)

private fun TsTasks.prepareTsPublication() =
    register(prepareTsPublicationName) {

        description = "Prepares the NPM package for TypeScript publishing."
        group = TsTasks.Group.publish

        // We need to copy two files into a destination directory without overwriting its content.
        // Default `Copy` task is not used since it overwrites the content of a destination
        // when copying there.
        // See issue: https://github.com/gradle/gradle/issues/1012

        doLast {
            project.copy {
                from(
                    packageJson,
                    npmrc
                )

                into(publicationDir)
            }
        }

        dependsOn(
            assembleTs,
            transpileTsSources
        )
    }

private val publishTsLocallyName = TaskName.of("publishTsLocally")

/**
 * Locates `publishTsLocally` task in this [TaskContainer].
 *
 * The task publishes the prepared NPM package locally using `npm link`.
 *
 *  @see <a href="https://docs.npmjs.com/cli/v8/commands/npm-link">npm-link | npm Docs</a>
 */
val TaskContainer.publishTsLocally: TaskProvider<Task>
    get() = named(publishTsLocallyName)

private fun TsTasks.publishTsLocally() =
    register(publishTsLocallyName) {

        description = "Publishes the TypeScript-based NPM package locally with `npm link`."
        group = TsTasks.Group.publish

        doLast {
            publicationDir.npm("link")
        }

        dependsOn(prepareTsPublication)
    }

private val publishTsName = TaskName.of("publishTs")

/**
 * Locates `publishTs` task in this [TaskContainer].
 *
 * The task publishes the prepared NPM package from
 * [publicationDirectory][io.spine.internal.gradle.typescript.TsEnvironment.publicationDir]
 * using `npm publish`.
 *
 * Please note, in order to publish an NMP package, a valid
 * [npmAuthToken][io.spine.internal.gradle.typescript.TsEnvironment.npmAuthToken] should be
 * set. If no token is set, a default dummy value is quite enough for the local development.
 *
 * @see <a href="https://docs.npmjs.com/cli/v7/commands/npm-publish">npm-publish | npm Docs</a>
 */
val TaskContainer.publishTs: TaskProvider<Task>
    get() = named(publishTsName)

private fun TsTasks.publishTs() =
    register(publishTsName) {

        description = "Publishes the TypeScript-based NPM package with `npm publish`."
        group = TsTasks.Group.publish

        doLast {
            publicationDir.npm("publish")
        }

        dependsOn(prepareTsPublication)
    }
