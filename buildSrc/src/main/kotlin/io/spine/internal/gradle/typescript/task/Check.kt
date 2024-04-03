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
import io.spine.internal.gradle.base.check
import io.spine.internal.gradle.java.test
import io.spine.internal.gradle.named
import io.spine.internal.gradle.register
import io.spine.internal.gradle.typescript.isWindows
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

/**
 * Registers tasks for verifying a TypeScript module.
 *
 * Please note, this task group depends on [assemble] tasks. Therefore, assembling tasks should
 * be applied in the first place.
 *
 * List of tasks to be created:
 *
 *  1. [TaskContainer.checkTs].
 *  2. [TaskContainer.auditNodePackages].
 *  3. [TaskContainer.testTs].
 *  4. [TaskContainer.coverageTs].
 *
 * Here's an example of how to apply it in `build.gradle.kts`:
 *
 * ```
 * import io.spine.internal.gradle.typescript.typescript
 * import io.spine.internal.gradle.typescript.task.assemble
 * import io.spine.internal.gradle.typescript.task.check
 *
 * // ...
 *
 * typescript {
 *     tasks {
 *         assemble()
 *         check()
 *     }
 * }
 * ```
 *
 * @param configuration any additional configuration related to the module's verification.
 */
fun TsTasks.check(configuration: TsTasks.() -> Unit = {}) {

    auditNodePackages()
    coverageTs()
    testTs()

    checkTs().also {
        check.configure {
            dependsOn(it)
        }
    }

    configuration()
}

private val checkTsName = TaskName.of("checkTs")

/**
 * Locates `checkTs` task in this [TaskContainer].
 *
 * The task runs tests, audits NPM modules and creates a test-coverage report.
 */
val TaskContainer.checkTs: TaskProvider<Task>
    get() = named(checkTsName)

private fun TsTasks.checkTs() =
    register(checkTsName) {

        description =
            "Runs tests against TypeScript sources," +
                " audits NPM modules," +
                " and creates a test-coverage report."

        group = TsTasks.Group.check

        dependsOn(
            auditNodePackages,
            coverageTs,
            testTs,
        )
    }

private val auditNodePackagesName = TaskName.of("auditTsNodePackages")

/**
 * Locates `auditNodePackages` task in this [TaskContainer].
 *
 * The task audits the module dependencies using the `npm audit` command.
 *
 * The `audit` command submits a description of the dependencies configured in the module
 * to a public registry and asks for a report of known vulnerabilities. If any are found,
 * then the impact and appropriate remediation will be calculated.
 *
 * @see <a href="https://docs.npmjs.com/cli/v7/commands/npm-audit">npm-audit | npm Docs</a>
 */
val TaskContainer.auditNodePackages: TaskProvider<Task>
    get() = named(auditNodePackagesName)

private fun TsTasks.auditNodePackages() =
    register(auditNodePackagesName) {

        description = "Audits the TypeScript module's Node dependencies."
        group = TsTasks.Group.check

        inputs.dir(nodeModules)

        doLast {

            // `critical` level is set as the minimum level of vulnerability for `npm audit`
            // to exit with a non-zero code.

            npm("set", "audit-level", "critical")

            try {
                npm("audit")
            } catch (ignored: Exception) {
                npm("audit", "--registry", "https://registry.npmjs.eu")
            }
        }

        dependsOn(installNodePackages)
    }

private val coverageTsName = TaskName.of("coverageTs")

/**
 * Locates `coverageTs` task in this [TaskContainer].
 *
 * The task runs the TypeScript tests and collects the code coverage.
 */
val TaskContainer.coverageTs: TaskProvider<Task>
    get() = named(coverageTsName)

private fun TsTasks.coverageTs() =
    register(coverageTsName) {

        description = "Runs the TypeScript tests and collects the code coverage."
        group = TsTasks.Group.check

        outputs.dir(nycOutput)

        doLast {
            npm("run", if (isWindows()) "coverage:win" else "coverage:unix")
        }

        dependsOn(assembleTs)
    }

private val testTsName = TaskName.of("testTs")

/**
 * Locates `testTs` task in this [TaskContainer].
 *
 * The task runs TypeScript tests.
 */
val TaskContainer.testTs: TaskProvider<Task>
    get() = named(testTsName)

private fun TsTasks.testTs() =
    register(testTsName) {

        description = "Runs TypeScript tests."
        group = TsTasks.Group.check

        doLast {
            npm("run", "test")
        }

        dependsOn(assembleTs)
        mustRunAfter(test)
    }
