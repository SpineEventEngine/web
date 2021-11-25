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

/**
 * Locates `assembleJs` task in this [TaskContainer].
 *
 * It is an aggregate task that assembles the JavaScript sources.
 *
 * The next tasks are to be executed:
 *
 *  1. [updatePackageVersion][io.spine.internal.gradle.javascript.task.updatePackageVersion];
 *  2. [installNodePackages][io.spine.internal.gradle.javascript.task.installNodePackages];
 *  3. [compileProtoToJs][io.spine.internal.gradle.javascript.task.compileProtoToJs].
 */
val TaskContainer.assembleJs: Task
    get() = getByName("assembleJs")

/**
 * Locates `compileProtoToJs` task in this [TaskContainer].
 *
 * The task compiles Protobuf messages into JavaScript. This is a lifecycle task that performs
 * no action itself. It is used to aggregate other tasks which perform the compilation.
 */
val TaskContainer.compileProtoToJs: Task
    get() = getByName("compileProtoToJs")

/**
 * Locates `installNodePackages` task in this [TaskContainer].
 *
 * The task installs a package and any packages that it depends on using the `npm install` command.
 *
 * The `npm install` command is executed with the vulnerability check disabled since
 * it cannot fail the task execution despite on vulnerabilities found.
 *
 * To check installed Node packages for vulnerabilities execute
 * [auditNodePackages][io.spine.internal.gradle.javascript.task.auditNodePackages] task.
 *
 * @see <a href="https://docs.npmjs.com/cli/v8/commands/npm-install">npm-install | npm Docs</a>
 */
val TaskContainer.installNodePackages: Task
    get() = getByName("installNodePackages")

/**
 * Locates `updatePackageVersion` task in this [TaskContainer].
 *
 * The task sets the module's version in `package.json` to the value of
 * [moduleVersion][io.spine.internal.gradle.javascript.JsEnvironment.moduleVersion]
 * specified in the current `JsEnvironment`.
 */
val TaskContainer.updatePackageVersion: Task
    get() = getByName("updatePackageVersion")
