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

import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.getByName

/**
 * Locates `deleteCompiled` task in this [TaskContainer].
 *
 * The task cleans old module dependencies and build outputs.
 */
internal val TaskContainer.deleteCompiled: Delete
    get() = getByName<Delete>("deleteCompiled")

/**
 * Locates `coverageJs` task in this [TaskContainer].
 *
 * The task runs the JavaScript tests and collects the code coverage.
 */
internal val TaskContainer.coverageJs: Task
    get() = getByName("coverageJs")

/**
 * Locates `copyBundledJs` task in this [TaskContainer].
 *
 * The task copies JavaScript sources to the temporary NPM publication directory.
 */
internal val TaskContainer.copyBundledJs: Copy
    get() = getByName<Copy>("copyBundledJs")

/**
 * Locates `transpileSources` task in this [TaskContainer].
 *
 * The task transpiles JavaScript sources before publishing them to NPM.
 */
internal val TaskContainer.transpileSources: Task
    get() = getByName("transpileSources")

/**
 * Locates `npmLicenseReport` task in this [TaskContainer].
 *
 * The task generates the report on NPM dependencies and their licenses.
 */
internal val TaskContainer.npmLicenseReport: Task
    get() = getByName("npmLicenseReport")
