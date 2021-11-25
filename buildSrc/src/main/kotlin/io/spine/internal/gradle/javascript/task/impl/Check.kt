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

import io.spine.internal.gradle.base.check
import io.spine.internal.gradle.java.test
import io.spine.internal.gradle.javascript.isWindows
import io.spine.internal.gradle.javascript.task.JsTaskRegistering
import io.spine.internal.gradle.javascript.task.assembleJs
import io.spine.internal.gradle.javascript.task.installNodePackages

/**
 * Registers tasks for verifying a JavaScript module.
 *
 * List of tasks to be created:
 *
 *  1. [publishJs][io.spine.internal.gradle.javascript.task.publishJs];
 *  2. [publishJsLocally][io.spine.internal.gradle.javascript.task.publishJsLocally];
 *  3. [prepareJsPublication][io.spine.internal.gradle.javascript.task.prepareJsPublication].
 *
 * Usage example:
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
fun JsTaskRegistering.check() = check.dependsOn(
    auditNodePackages(),
    testJs(),
    coverageJs(),
)

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


private fun JsTaskRegistering.testJs() =
    create("testJs") {

        description = "Runs the JavaScript tests."
        group = jsBuildTask

        dependsOn(assembleJs)
        mustRunAfter(test)
    }

private fun JsTaskRegistering.coverageJs() =
    create("coverageJs") {

        description = "Runs the JavaScript tests and collects the code coverage info."
        group = jsAnyTask

        outputs.dir(nycOutput)

        doLast {
            npm("run", if(isWindows()) "coverage:win" else "coverage:unix")
        }

        dependsOn(assembleJs)
    }
