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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.spine.internal.gradle.base.assemble
import io.spine.internal.gradle.javascript.task.JsTaskRegistering

/**
 * Registers tasks for assembling a JavaScript module.
 *
 * Full list of tasks to be created:
 *
 *  1. [assembleJs][io.spine.internal.gradle.javascript.task.assembleJs];
 *  2. [compileProtoToJs][io.spine.internal.gradle.javascript.task.compileProtoToJs];
 *  3. [installNodePackages][io.spine.internal.gradle.javascript.task.installNodePackages];
 *  4. [updatePackageVersion][io.spine.internal.gradle.javascript.task.updatePackageVersion].
 *
 * An example of how to apply those tasks in `build.gradle.kts`:
 *
 * ```
 * import io.spine.internal.gradle.js.javascript
 * import io.spine.internal.gradle.js.task.impl.assemble
 *
 * // ...
 *
 * js {
 *     tasks {
 *         register {
 *             assemble()
 *         }
 *     }
 * }
 * ```
 */
fun JsTaskRegistering.assemble() =
    assemble.dependsOn(
        assembleJs()
    )

private fun JsTaskRegistering.assembleJs() =
    create("buildJs") {

        description = "Assembles the JavaScript sources."
        group = jsBuildTask

        dependsOn(
            installNodePackages(),
            compileProtoToJs(),
            updatePackageVersion()
        )
    }

private fun JsTaskRegistering.compileProtoToJs() =
    create("compileProtoToJs") {

        description = "Compiles Protobuf messages into JavaScript."
        group = jsBuildTask
    }

private fun JsTaskRegistering.installNodePackages() =
    create("installNodePackages") {

        description = "Installs the module`s Node dependencies."
        group = jsBuildTask

        inputs.file(packageJson)
        outputs.dir(nodeModules)

        doLast {
            npm("set", "audit", "false")
            npm("install")
        }
    }

private fun JsTaskRegistering.updatePackageVersion() =
    create("updatePackageVersion") {

        description = "Sets the version in `package.json`."
        group = jsBuildTask

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
