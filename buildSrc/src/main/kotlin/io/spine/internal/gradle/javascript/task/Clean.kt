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

import io.spine.internal.gradle.base.clean
import org.gradle.api.Task
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName

/**
 * Registers tasks for deleting output of JavaScript build tasks.
 *
 * List of tasks to be created:
 *
 *  1. [TaskContainer.cleanJs];
 *  2. [TaskContainer.cleanGenerated].
 *
 *  @see JsTasks
 */
fun JsTasks.clean() =
    clean.dependsOn(
        cleanJs()
    )


/**
 * Locates `cleanJs` task in this [TaskContainer].
 *
 * The task deletes output of `assembleJs` task and output of its dependants.
 */
val TaskContainer.cleanJs: Task
    get() = getByName("cleanJs")

private fun JsTasks.cleanJs() =
    create<Delete>("cleanJs") {

        description = "Cleans output of `assembleJs` task and output of its dependants."
        group = jsCleanTask

        delete(
            assembleJs.outputs,
            compileProtoToJs.outputs,
            installNodePackages.outputs,
        )

        dependsOn(
            cleanGenerated()
        )
    }


/**
 * Locates `cleanGenerated` task in this [TaskContainer].
 *
 * The task deletes directories with generated code and reports.
 */
internal val TaskContainer.cleanGenerated: Delete
    get() = getByName<Delete>("cleanGenerated")

private fun JsTasks.cleanGenerated() =
    create<Delete>("cleanGenerated") {

        description = "Cleans generated code and reports."
        group = jsCleanTask

        delete(
            genProtoMain,
            genProtoTest,
            nycOutput,
        )
    }
