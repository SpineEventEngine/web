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

import io.spine.internal.gradle.js.JsEnvironment
import io.spine.internal.gradle.js.JsContext
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer

open class JsTaskContext(jsEnv: JsEnvironment, project: Project)
    : JsContext(jsEnv, project), TaskContainer by project.tasks
{
    internal val jsBuildTask = "JavaScript/Build"
    internal val jsAnyTask = "JavaScript/Any"
    internal val jsPublishTask = "JavaScript/Publish"
}

open class JsTasks(jsEnv: JsEnvironment, project: Project)
    : JsTaskContext(jsEnv, project)
{
    private val registering = JsTaskRegistering(jsEnv, project)
    private val configuring = JsTaskConfiguring(jsEnv, project)

    /**
     * Registers new tasks.
     */
    fun register(registrations: JsTaskRegistering.() -> Unit) =
        registering.run(registrations)

    /**
     * Configures already registered tasks.
     */
    fun configure(configurations: JsTaskConfiguring.() -> Unit) =
        configuring.run(configurations)

}

/**
 * A logical scope for registering new tasks inside [JsTaskContext].
 */
class JsTaskRegistering(jsEnv: JsEnvironment, project: Project)
    : JsTaskContext(jsEnv, project)

/**
 * A logical scope for configuring present tasks inside [JsTaskContext].
 */
class JsTaskConfiguring(jsEnv: JsEnvironment, project: Project)
    : JsTaskContext(jsEnv, project)
