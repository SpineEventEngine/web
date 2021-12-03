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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.protobuf.gradle.GenerateProtoTask
import io.spine.internal.gradle.base.assemble
import io.spine.internal.gradle.javascript.plugin.generateJsonParsers
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.withType

/**
 * Registers tasks for assembling JavaScript artifacts.
 *
 * Please note, this task group depends on [mc-js][io.spine.internal.gradle.javascript.plugin.mcJs]
 * and [protobuf][io.spine.internal.gradle.javascript.plugin.protobuf]` plugins. Therefore,
 * these plugins should be applied in the first place.
 *
 * List of tasks to be created:
 *
 *  1. [TaskContainer.assembleJs].
 *  2. [TaskContainer.compileProtoToJs].
 *  3. [TaskContainer.installNodePackages].
 *  4. [TaskContainer.updatePackageVersion].
 *
 * An example of how to apply it in `build.gradle.kts`:
 *
 * ```
 * import io.spine.internal.gradle.javascript.javascript
 * import io.spine.internal.gradle.javascript.task.assemble
 *
 * // ...
 *
 * javascript {
 *     tasks {
 *         assemble()
 *     }
 * }
 * ```
 */
fun JsTasks.assemble() {

    installNodePackages()

    compileProtoToJs().also {
        generateJsonParsers.configure {
            dependsOn(it)
        }
    }

    updatePackageVersion()

    assembleJs().also {
        assemble.configure {
            dependsOn(it)
        }
    }
}

/**
 * Locates `assembleJs` task in this [TaskContainer].
 *
 * It is a lifecycle task that produces consumable JavaScript artifacts.
 */
val TaskContainer.assembleJs: TaskProvider<Task>
    get() = named("assembleJs")

private fun JsTasks.assembleJs() =
    register("assembleJs") {

        description = "Assembles JavaScript sources into consumable artifacts."
        group = jsAssembleTask

        dependsOn(
            installNodePackages,
            compileProtoToJs,
            updatePackageVersion,
            generateJsonParsers
        )
    }

/**
 * Locates `compileProtoToJs` task in this [TaskContainer].
 *
 * The task is responsible for compiling Protobuf messages into JavaScript. It aggregates the tasks
 * provided by `protobuf` plugin that perform actual compilation.*
 */
val TaskContainer.compileProtoToJs: TaskProvider<Task>
    get() = named("compileProtoToJs")

private fun JsTasks.compileProtoToJs() =
    register("compileProtoToJs") {

        description = "Compiles Protobuf messages into JavaScript."
        group = jsAssembleTask

        withType<GenerateProtoTask>()
            .forEach { dependsOn(it) }
    }

/**
 * Locates `installNodePackages` task in this [TaskContainer].
 *
 * The task installs Node packages which this module depends on using `npm install` command.
 *
 * The `npm install` command is executed with the vulnerability check disabled since
 * it cannot fail the task execution despite on vulnerabilities found.
 *
 * To check installed Node packages for vulnerabilities execute
 * [TaskContainer.auditNodePackages] task.
 *
 * See [npm-install | npm Docs](https://docs.npmjs.com/cli/v8/commands/npm-install).
 */
val TaskContainer.installNodePackages: TaskProvider<Task>
    get() = named("installNodePackages")

private fun JsTasks.installNodePackages() =
    register("installNodePackages") {

        description = "Installs module`s Node dependencies."
        group = jsAssembleTask

        inputs.file(packageJson)
        outputs.dir(nodeModules)

        doLast {
            npm("set", "audit", "false")
            npm("install")
        }
    }

/**
 * Locates `updatePackageVersion` task in this [TaskContainer].
 *
 * The task sets the module's version in `package.json` to the value of
 * [moduleVersion][io.spine.internal.gradle.javascript.JsEnvironment.moduleVersion]
 * specified in the current `JsEnvironment`.
 */
val TaskContainer.updatePackageVersion: TaskProvider<Task>
    get() = named("updatePackageVersion")

private fun JsTasks.updatePackageVersion() =
    register("updatePackageVersion") {

        description = "Sets a module's version in `package.json`."
        group = jsAssembleTask

        doLast {
            val objectNode = ObjectMapper()
                .readValue(packageJson, ObjectNode::class.java)
                .put("version", moduleVersion)

            packageJson.writeText(

                // We are going to stick to JSON formatting used by `npm` itself.
                // So that modifying the line with the version would ONLY affect a single line
                // when comparing two files i.e. in Git.

                (objectNode.toPrettyString() + '\n')
                    .replace("\" : ", "\": ")
            )
        }
    }
