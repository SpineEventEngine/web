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

package io.spine.internal.gradle.base

import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer

/**
 * Enumerates and provides access to the tasks provided by `The Base Plugin`.
 *
 * @see <a href="https://docs.gradle.org/current/userguide/base_plugin.html#sec:base_tasks">
 *     Tasks | The Base Plugin</a>
 */
interface BaseTaskListing : TaskContainer {

    /**
     * Deletes the build directory and everything in it,
     * i.e. the path specified by the `Project.getBuildDir()` project property.
     */
    val clean: Task
        get() = getByName("clean")

    /**
     * Plugins and build authors should attach their verification tasks,
     * such as ones that run tests, to this lifecycle task using `check.dependsOn(task)`.
     */
    val check: Task
        get() = getByName("check")

    /**
     * Plugins and build authors should attach tasks that produce distributions and
     * other consumable artifacts to this lifecycle task.
     */
    val assemble: Task
        get() = getByName("assemble")

    /**
     * Intended to build everything, including running all tests, producing the production artifacts
     * and generating documentation. You will probably rarely attach concrete tasks directly
     * to `build` as `assemble` and `check` are typically more appropriate.
     */
    val build: Task
        get() = getByName("build")
}
