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

import io.spine.internal.gradle.TaskName
import io.spine.internal.gradle.base.clean
import io.spine.internal.gradle.named
import io.spine.internal.gradle.register
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

/**
 * Registers tasks for deleting output of TypeScript builds.
 *
 * Please note, this task group depends on [assemble] tasks. Therefore, assembling tasks should
 * be applied in the first place.
 *
 * List of tasks to be created:
 *
 *  1. [TaskContainer.cleanTs].
 *  2. [TaskContainer.cleanGeneratedTs].
 *
 * Here's an example of how to apply it in `build.gradle.kts`:
 *
 * ```
 * import io.spine.internal.gradle.typescript.typescript
 * import io.spine.internal.gradle.typescript.task.assemble
 * import io.spine.internal.gradle.typescript.task.clean
 *
 * // ...
 *
 * typescript {
 *     tasks {
 *         assemble()
 *         clean()
 *     }
 * }
 * ```
 */
fun TsTasks.clean() {

    cleanGeneratedTs()

    cleanTs().also {
        clean.configure {
            dependsOn(it)
        }
    }
}

private val cleanTsName = TaskName.of("cleanTs", Delete::class)

/**
 * Locates `cleanTs` task in this [TaskContainer].
 *
 * The task deletes output of `assembleTs` task and output of its dependants.
 */
val TaskContainer.cleanTs: TaskProvider<Delete>
    get() = named(cleanTsName)

private fun TsTasks.cleanTs() =
    register(cleanTsName) {

        description = "Cleans output of `assembleTs` task and output of its dependants."
        group = TsTasks.Group.clean

        System.err.println("---- Registering `cleanTs`... ----")

        delete(
            assembleTs.map { it.outputs },
            compileProtoToTs.map { it.outputs },
            installNodePackages.map { it.outputs },
        )

        dependsOn(
            cleanGeneratedTs
        )
    }

private val cleanGeneratedTsName = TaskName.of("cleanGeneratedTs", Delete::class)

/**
 * Locates `cleanGeneratedTs` task in this [TaskContainer].
 *
 * The task deletes directories with the generated TypeScript code and reports.
 */
val TaskContainer.cleanGeneratedTs: TaskProvider<Delete>
    get() = named(cleanGeneratedTsName)

private fun TsTasks.cleanGeneratedTs() =
    register(cleanGeneratedTsName) {

        description = "Cleans generated code and reports."
        group = TsTasks.Group.clean

        delete(
            genProtoMain,
            genProtoTest,
            nycOutput,
        )
    }
