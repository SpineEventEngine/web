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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.protobuf.gradle.GenerateProtoTask
import io.spine.internal.gradle.base.assemble
import io.spine.internal.gradle.named
import io.spine.internal.gradle.register
import io.spine.internal.gradle.TaskName
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.withType

/**
 * Registers tasks for assembling TypeScript artifacts.
 *
 * Please note, this task group depends on
 * [protobuf][io.spine.internal.gradle.typescript.plugin.protobuf]` plugin. Therefore,
 * this plugin should be applied in the first place.
 *
 * List of tasks to be created:
 *
 *  1. [TaskContainer.assembleTs].
 *  2. [TaskContainer.compileProtoToTs].
 *  3. [TaskContainer.installNodePackages].
 *  4. [TaskContainer.updatePackageVersion].
 *
 * Here's an example of how to apply it in `build.gradle.kts`:
 *
 * ```
 * import io.spine.internal.gradle.typescript.typescript
 * import io.spine.internal.gradle.typescript.task.assemble
 *
 * // ...
 *
 * typescript {
 *     tasks {
 *         assemble()
 *     }
 * }
 * ```
 *
 * @param configuration any additional configuration related to the module's assembling.
 */
fun TsTasks.assemble(configuration: TsTasks.() -> Unit = {}) {

    installNodePackages()
    compileProtoToTs()

    // TODO: Do we need it at all?
    //.also {
//        generateJsonParsers.configure {
//            dependsOn(it)
//        }
//    }

    updatePackageVersion()

    assembleTs().also {
        assemble.configure {
            dependsOn(it)
        }
    }

    configuration()
}

private val assembleTsName = TaskName.of("assembleTs")

/**
 * Locates `assembleTs` task in this [TaskContainer].
 *
 * It is a lifecycle task that produces consumable JavaScript artifacts.
 */
val TaskContainer.assembleTs: TaskProvider<Task>
    get() = named(assembleTsName)

private fun TsTasks.assembleTs() =
    register(assembleTsName) {

        System.err.println("---- `assembleTs` registered. ----")
        description = "Assembles TypeScript sources into consumable artifacts."
        group = TsTasks.Group.assemble

        dependsOn(
            installNodePackages,
            compileProtoToTs,
            updatePackageVersion,
        )
    }

private val compileProtoToTsName = TaskName.of("compileProtoToTs")

/**
 * Locates `compileProtoToTs` task in this [TaskContainer].
 *
 * The task is responsible for compiling Protobuf messages into JavaScript. It aggregates the tasks
 * provided by `protobuf` plugin that perform actual compilation.
 */
val TaskContainer.compileProtoToTs: TaskProvider<Task>
    get() = named(compileProtoToTsName)

private fun TsTasks.compileProtoToTs() =
    register(compileProtoToTsName) {

        description = "Compiles Protobuf messages into TypeScript."
        group = TsTasks.Group.assemble

        withType<GenerateProtoTask>()
            .forEach { dependsOn(it) }
    }

private val installNodePackagesName = TaskName.of("installNodePackages")

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
    get() = named(installNodePackagesName)

private fun TsTasks.installNodePackages() =
    register(installNodePackagesName) {

        description = "Installs TypeScript module`s Node dependencies."
        group = TsTasks.Group.assemble

        inputs.file(packageJson)
        outputs.dir(nodeModules)

        doLast {
            npm("set", "audit", "false")
            npm("install")
        }
    }

private val updatePackageVersionName = TaskName.of("updatePackageVersion")

/**
 * Locates `updatePackageVersion` task in this [TaskContainer].
 *
 * The task sets the module's version in `package.json` to the value of
 * [moduleVersion][io.spine.internal.gradle.typescript.TsEnvironment.moduleVersion]
 * specified in the current `TsEnvironment`.
 */
val TaskContainer.updatePackageVersion: TaskProvider<Task>
    get() = named(updatePackageVersionName)

private fun TsTasks.updatePackageVersion() =
    register(updatePackageVersionName) {

        description = "Sets a TypeScript module's version in `package.json`."
        group = TsTasks.Group.assemble

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
