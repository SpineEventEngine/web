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

import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer

internal val TaskContainer.checkJs: Task
    get() = getByName("checkJs")

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

/**
 * Locates `coverageJs` task in this [TaskContainer].
 *
 * The task runs the JavaScript tests and collects the code coverage.
 */
internal val TaskContainer.coverageJs: Task
    get() = getByName("coverageJs")

/**
 * Locates `testJs` task in this [TaskContainer].
 *
 * The task runs the JavaScript tests.
 */
val TaskContainer.testJs: Task
    get() = getByName("testJs")
