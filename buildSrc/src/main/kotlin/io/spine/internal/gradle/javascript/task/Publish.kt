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
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.getByName

/**
 * Locates `prepareJsPublication` task in this [TaskContainer].
 *
 * This is a lifecycle task that prepares the NPM package for publishing in
 * [publicationDirectory][io.spine.internal.gradle.javascript.JsEnvironment.publicationDir]
 * of the current `JsEnvironment`.
 *
 * Does nothing by default, so a user should configure this task to copy all
 * required files to that directory.
 *
 * Depends on [buildsJs][io.spine.internal.gradle.javascript.task.buildJs].
 */
internal val TaskContainer.prepareJsPublication: Copy
    get() = getByName<Copy>("prepareJsPublication")

/**
 * Locates `publishJsLocally` task in this [TaskContainer].
 *
 * The task publishes locally the prepared NPM package
 * from [publicationDirectory][io.spine.internal.gradle.javascript.JsEnvironment.publicationDir]
 * with `npm link`.
 *
 * Depends on [prepareJsPublication][io.spine.internal.gradle.javascript.task.prepareJsPublication].
 *
 *  @see <a href="https://docs.npmjs.com/cli/v8/commands/npm-link">npm-link | npm Docs</a>
 */
internal val TaskContainer.publishJsLocally: Task
    get() = getByName("publishJsLocally")

/**
 * Locates `publishJs` task in this [TaskContainer].
 *
 * The task publishes the prepared NPM package from
 * [publicationDirectory][io.spine.internal.gradle.javascript.JsEnvironment.publicationDir]
 * with `npm publish`.
 *
 * Please note, in order to publish the NMP module, a valid
 * [npmAuthToken][io.spine.internal.gradle.javascript.JsEnvironment.npmAuthToken] should be
 * set. If no token is set, a default dummy value is quite enough for the local development.
 *
 * Depends on [prepareJsPublication][io.spine.internal.gradle.javascript.task.prepareJsPublication].
 *
 * @see <a href="https://docs.npmjs.com/cli/v7/commands/npm-publish">npm-publish | npm Docs</a>
 */
internal val TaskContainer.publishJs: Task
    get() = getByName("publishJs")
