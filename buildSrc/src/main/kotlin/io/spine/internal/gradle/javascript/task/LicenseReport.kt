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

import io.spine.internal.gradle.report.license.generateLicenseReport
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer

/**
 * Registers a single [task][npmLicenseReport] for including NPM dependencies into license reports.
 *
 * @see [JsTasks]
 */
fun JsTaskRegistering.licenseReport() =
    generateLicenseReport.finalizedBy(
        npmLicenseReport()
    )


/**
 * Locates `npmLicenseReport` task in this [TaskContainer].
 *
 * The task generates the report on NPM dependencies and their licenses.
 */
internal val TaskContainer.npmLicenseReport: Task
    get() = getByName("npmLicenseReport")

private fun JsTaskRegistering.npmLicenseReport() =
    create("npmLicenseReport") {

        description = "Generates the report on NPM dependencies and their licenses."
        group = jsBuildTask

        doLast {

            // The script below generates license report for NPM dependencies and appends it
            // to the report for Java dependencies generated by `generateLicenseReport` task.

            npm("run", "license-report")
        }
    }
