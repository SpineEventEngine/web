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

package io.spine.internal.gradle.js.task.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.spine.internal.gradle.base.assemble
import io.spine.internal.gradle.base.check
import io.spine.internal.gradle.base.clean
import io.spine.internal.gradle.java.test
import io.spine.internal.gradle.js.task.buildJs
import io.spine.internal.gradle.js.task.compileProtoToJs
import io.spine.internal.gradle.js.task.installNodePackages
import io.spine.internal.gradle.js.task.JsTaskRegistering
import io.spine.internal.gradle.js.task.updatePackageVersion

/**
 * Registers group of tasks for building a JavaScript module.
 *
 * List of tasks to be created:
 *
 *  1. [buildJs][io.spine.internal.gradle.js.task.buildJs];
 *  1. [cleanJs][io.spine.internal.gradle.js.task.cleanJs];
 *  1. [testJs][io.spine.internal.gradle.js.task.testJs];
 *  1. [updatePackageVersion][io.spine.internal.gradle.js.task.updatePackageVersion];
 *  1. [installNodePackages][io.spine.internal.gradle.js.task.installNodePackages];
 *  1. [auditNodePackages][io.spine.internal.gradle.js.task.auditNodePackages];
 *  1. [compileProtoToJs][io.spine.internal.gradle.js.task.compileProtoToJs];
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
fun JsTaskRegistering.build() {

    compileProtoToJs()
    installNodePackages()
    updatePackageVersion()

    check.dependsOn(
        auditNodePackages(),
        testJs(),
    )
    assemble.dependsOn(
        buildJs()
    )
    clean.dependsOn(
        cleanJs()
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

private fun JsTaskRegistering.auditNodePackages() =
    create("auditNodePackages") {

        description = "Audits the module's Node dependencies."
        group = jsBuildTask

        inputs.dir(nodeModules)

        doLast {

            // Sets `critical` as the minimum level of vulnerability for `npm audit` to exit
            // with a non-zero exit code.

            npm("set", "audit-level", "critical")

            try {
                npm("audit")
            } catch (ignored: Exception) {
                npm("audit", "--registry", "https://registry.npmjs.eu")
            }
        }

        dependsOn(installNodePackages)
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

private fun JsTaskRegistering.buildJs() =
    create("buildJs") {

        description = "Assembles the JavaScript sources."
        group = jsBuildTask

        dependsOn(
            updatePackageVersion,
            installNodePackages,
            compileProtoToJs
        )
    }

private fun JsTaskRegistering.cleanJs() =
    create("cleanJs") {

        description = "Cleans output of `buildJs` task and output of its dependants."
        group = jsBuildTask

        doLast {
            project.delete(
                buildJs.outputs,
                compileProtoToJs.outputs,
                installNodePackages.outputs
            )
        }
    }

private fun JsTaskRegistering.testJs() =
    create("testJs") {

        description = "Runs the JavaScript tests."
        group = jsBuildTask

        dependsOn(
            installNodePackages,
            compileProtoToJs
        )

        mustRunAfter(test)
    }
