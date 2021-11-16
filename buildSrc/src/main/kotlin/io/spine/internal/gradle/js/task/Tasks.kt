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
import org.gradle.api.tasks.TaskContainer

/**
 * Shortcuts for accessing the tasks
 * provided by [JsExtension][io.spine.internal.gradle.js.JsExtension].
 */
interface JsTaskListing : JsBuildTaskListing, JsPublishTaskListing

/**
 * Shortcuts for accessing the tasks for building a JavaScript module.
 *
 * Tasks are to be provided by [JsExtension][io.spine.internal.gradle.js.JsExtension].
 */
interface JsBuildTaskListing : TaskContainer {

    /**
     * Compiles Protobuf messages into JavaScript.
     */
    val compileProtoToJs: Task
        get() = getByName("compileProtoToJs")

    /**
     * Installs the module`s Node dependencies.
     */
    val installNodePackages: Task
        get() = getByName("installNodePackages")

    /**
     * Audits the module's Node dependencies.
     */
    val auditNodePackages: Task
        get() = getByName("auditNodePackages")

    /**
     * Sets the module's version in `package.json` to the specified one.
     */
    val updatePackageVersion: Task
        get() = getByName("updatePackageVersion")

    /**
     * Assembles the JavaScript sources.
     */
    val buildJs: Task
        get() = getByName("buildJs")

    /**
     * Cleans output of `buildJs` task and output of its dependants.
     */
    val cleanJs: Task
        get() = getByName("cleanJs")

    /**
     * Runs the JavaScript tests.
     */
    val testJs: Task
        get() = getByName("testJs")
}

/**
 * Shortcuts for accessing the tasks for publishing a JavaScript module.
 *
 * Tasks are to be provided by [JsExtension][io.spine.internal.gradle.js.JsExtension].
 */
interface JsPublishTaskListing : TaskContainer {

    /**
     * Prepares the NPM package for publishing.
     */
    val prepareJsPublication: Task
        get() = getByName("prepareJsPublication")

    /**
     * Publishes the NPM package locally with `npm link`.
     */
    val publishJsLocally: Task
        get() = getByName("publishJsLocally")

    /**
     * Publishes the NPM package with `npm publish`.
     */
    val publishJs: Task
        get() = getByName("publishJs")
}
