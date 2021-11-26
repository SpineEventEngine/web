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

import io.spine.internal.gradle.base.check
import io.spine.internal.gradle.java.test
import io.spine.internal.gradle.javascript.isWindows
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer

/**
 * Registers tasks for verifying a JavaScript module.
 *
 * List of tasks to be created:
 *
 *  1. [checkJs][checkJs];
 *  2. [auditNodePackages][auditNodePackages];
 *  3. [testJs][testJs];
 *  4. [coverageJs][coverageJs].
 *
 * An example of how to apply those tasks in `build.gradle.kts`:
 *
 * ```
 * import io.spine.internal.gradle.js.javascript
 * import io.spine.internal.gradle.js.task.impl.check
 *
 * // ...
 *
 * js {
 *     tasks {
 *         register {
 *             check()
 *         }
 *     }
 * }
 * ```
 */
fun JsTaskRegistering.check(configuration: JsTaskRegistering.() -> Unit = {}) {

    check.dependsOn(
        checkJs()
    )

    configuration()
}



internal val TaskContainer.checkJs: Task
    get() = getByName("checkJs")

private fun JsTaskRegistering.checkJs() =
    create("checkJs") {

        description = "Runs tests, audits NPM modules and creates a test-coverage report."
        group = jsBuildTask

        dependsOn(
            auditNodePackages(),
            coverageJs(),
            testJs(),
        )
    }


/**
 * Locates `auditNodePackages` task in this [TaskContainer].
 *
 * The task audits the module dependencies using the `npm audit` command.
 *
 * The `audit` command submits a description of the dependencies configured in the module
 * to the registry and asks for a report of known vulnerabilities. If any are found,
 * then the impact and appropriate remediation will be calculated.
 *
 * @see <a href="https://docs.npmjs.com/cli/v7/commands/npm-audit">npm-audit | npm Docs</a>
 */
val TaskContainer.auditNodePackages: Task
    get() = getByName("auditNodePackages")

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


/**
 * Locates `coverageJs` task in this [TaskContainer].
 *
 * The task runs the JavaScript tests and collects the code coverage.
 */
val TaskContainer.coverageJs: Task
    get() = getByName("coverageJs")

private fun JsTaskRegistering.coverageJs() =
    create("coverageJs") {

        description = "Runs the JavaScript tests and collects the code coverage info."
        group = jsAnyTask

        outputs.dir(nycOutput)

        // The statement below is not the best practice.
        // But creating a dedicated task is not much better.
        // This task is the only one in this group producing cleanable output.

        cleanGenerated.doLast {
            project.delete(outputs)
        }

        doLast {
            npm("run", if (isWindows()) "coverage:win" else "coverage:unix")
        }

        dependsOn(assembleJs)
    }


/**
 * Locates `testJs` task in this [TaskContainer].
 *
 * The task runs the JavaScript tests.
 */
val TaskContainer.testJs: Task
    get() = getByName("testJs")

private fun JsTaskRegistering.testJs() =
    create("testJs") {

        description = "Runs the JavaScript tests."
        group = jsBuildTask

        doLast {
            npm("run", "test")
        }

        dependsOn(assembleJs)
        mustRunAfter(test)
    }
