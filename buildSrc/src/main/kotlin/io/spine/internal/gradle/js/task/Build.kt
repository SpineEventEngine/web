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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.File
import org.gradle.api.Task

/**
 * Registers tasks for building JavaScript projects.
 *
 * ...
 */
fun JsTaskRegistering.build(packageVersion: String) {

    val compileProtoToJs = compileProtoToJs()
    val updatePackageVersion = updatePackageVersion(packageVersion)

    val installNodePackages = installNodePackages().also {
        val auditNodePackages = auditNodePackages()
        auditNodePackages.dependsOn(it)
        getByName("check").dependsOn(auditNodePackages)
    }

    val buildJs = buildJs(updatePackageVersion, installNodePackages, compileProtoToJs).also {
        getByName("assemble").dependsOn(it)
    }

    cleanJs(buildJs, compileProtoToJs, installNodePackages).also {
        getByName("clean").dependsOn(it)
    }

    testJs(installNodePackages, compileProtoToJs).also {
        getByName("check").dependsOn(it)
        it.mustRunAfter(getByName("test"))
    }
}

/**
 * Compiles Protobuf sources into JavaScript.
 *
 * This is a lifecycle task. It performs no action itself but is used to trigger other tasks
 * which perform the compilation.
 */
private fun JsTaskRegistering.compileProtoToJs() =
    create("compilerProtoToJs1") {

        description = "Compiles Protobuf sources to JavaScript."
        group = jsBuildTask
    }

/**
 * Installs the module dependencies using the `npm install` command.
 *
 * The `npm install` command is executed with the vulnerability check disabled since
 * it cannot fail the task execution despite on vulnerabilities found.
 *
 * To check installed Node packages for vulnerabilities execute `auditNodePackages` task.
 *
 * @see <a href="https://docs.npmjs.com/cli/v7/commands/npm-audit">npm-audit | npm Docs</a>
 */
private fun JsTaskRegistering.installNodePackages() =
    create("installNodePackages1") {

        description = "Installs the module`s Node dependencies."
        group = jsBuildTask

        inputs.file(packageJsonFile)
        outputs.dir(nodeModulesDir)

        doLast {
            npm("set", "audit", "false")
            npm("install")
        }
    }

/**
 * Audits the module dependencies using the `npm audit` command.
 *
 * The audit command submits a description of the dependencies configured in the module
 * to the registry and asks for a report of known vulnerabilities. If any are found,
 * then the impact and appropriate remediation will be calculated.
 *
 * @see <a href="https://docs.npmjs.com/cli/v7/commands/npm-audit">npm-audit | npm Docs</a>
 */
private fun JsTaskRegistering.auditNodePackages() =
    create("auditNodePackages1") {

        description = "Audits the module's Node dependencies."
        group = jsBuildTask

        inputs.dir(nodeModulesDir)

        doLast {

            // Sets `critical` as the minimum level of vulnerability for `npm audit` to exit
            // with a non-zero exit code.

            npm("set", "audit-level", "critical")

            try {
                npm("audit")
            } catch (ignored: Exception) {
                npm("audit", "--registry", "http://registry.npmjs.eu")
            }
        }
    }

/**
 * Sets the module's version in `package.json` to the specified one.
 */
private fun JsTaskRegistering.updatePackageVersion(newVersion: String) =
    create("updatePackageVersion1") {

        description = "Updates the version in `package.json`."
        group = jsBuildTask

        doLast {
            val packageJson = File(packageJsonFile)

            val objectNode = ObjectMapper()
                .readValue(packageJson, ObjectNode::class.java)
                .put("version", newVersion)

            packageJson.writeText(

                // We are going to stick to JSON formatting used by `npm` itself.
                // So that modifying the line with the version would ONLY affect a single line
                // when comparing two files i.e. in Git.

                (objectNode.toPrettyString() + '\n')
                    .replace("\" : ", "\": ")
            )
        }
    }

/**
 * Assembles the JS sources.
 *
 * This task in an analog of JavaPlugin's `build` for JS.
 *
 * This is a lifecycle task. It performs no action itself but is used to trigger other tasks
 * which perform the building.
 */
private fun JsTaskRegistering.buildJs(
    updatePackageVersion: Task,
    installNodePackages: Task,
    compileProtoToJs: Task,

) = create("buildJs") {

    description = "Assembles the JS sources."
    group = jsBuildTask

    dependsOn(
        updatePackageVersion,
        installNodePackages,
        compileProtoToJs
    )
}

/**
 * Cleans output of `buildJs` task and its dependants.
 */
private fun JsTaskRegistering.cleanJs(
    buildJs: Task,
    compileProtoToJs: Task,
    installNodePackages: Task

) = create("cleanJs") {

    description = "Cleans the output of JavaScript build."
    group = jsBuildTask

    doLast {
        project.delete(
            buildJs.outputs,
            compileProtoToJs.outputs,
            installNodePackages.outputs
        )
    }
}

/**
 * Tests the JS sources.
 */
private fun JsTaskRegistering.testJs(
    installNodePackages: Task,
    compileProtoToJs: Task

) = create("testJs") {

        description = "Runs the JavaScript tests."
        group = jsBuildTask

        dependsOn(
            installNodePackages,
            compileProtoToJs
        )
    }
