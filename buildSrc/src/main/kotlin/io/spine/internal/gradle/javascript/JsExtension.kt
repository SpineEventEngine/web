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

package io.spine.internal.gradle.javascript

import io.spine.internal.gradle.javascript.task.JsTasks
import io.spine.internal.gradle.javascript.plugins.JsPlugins
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.findByType

/**
 * Configures [JsExtension].
 */
fun Project.javascript(configuration: JsExtension.() -> Unit) {
    extensions.run {
        configuration.invoke(
            findByType() ?: create("jsExtension", project)
        )
    }
}

/**
 * Scope for performing JavaScript-related configuration.
 *
 * @see [javascript]
 */
open class JsExtension(internal val project: Project) {

    private val configurableEnvironment = ConfigurableJsEnvironment(
        object : JsEnvironment {
            override val projectDir = project.projectDir
            override val moduleVersion = project.extra["versionToPublishJs"].toString()
        }
    )

    val environment: JsEnvironment = configurableEnvironment
    val tasks: JsTasks = JsTasks(environment, project)
    val plugins: JsPlugins = JsPlugins(environment, project)

    /**
     * Overrides default values of [JsEnvironment].
     *
     * Please note, environment should be set up firstly to have the effect on the parts
     * of the extension that depend on it.
     */
    fun environment(overridings: ConfigurableJsEnvironment.() -> Unit) =
        configurableEnvironment.run(overridings)

    /**
     * Configures [Js-related tasks][JsTasks].
     */
    fun tasks(configurations: JsTasks.() -> Unit) =
        tasks.run(configurations)

    /**
     * Configures [Js-related plugins][JsPlugins].
     */
    fun plugins(configurations: JsPlugins.() -> Unit) =
        plugins.run(configurations)

}
