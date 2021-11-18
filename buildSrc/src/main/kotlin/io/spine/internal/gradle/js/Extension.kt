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

package io.spine.internal.gradle.js

import io.spine.internal.gradle.js.task.JsTasks
import java.io.File
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.findByType
import org.gradle.plugins.ide.idea.model.IdeaModel

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
 * ...
 */
open class JsExtension(internal val project: Project) {

    private val defaultEnvironment = object : JsEnvironment {
        override val projectDir = project.projectDir
        override val moduleVersion = project.extra["versionToPublishJs"].toString()
    }

    private val environment = ConfigurableJsEnvironment(defaultEnvironment)
    private val tasks = JsTasks(environment, project)

    init {

        // `node_modules` directory contains installed module's dependencies.
        // No one needs it as a part of the module.

        project.configure<IdeaModel> {

            module {

                // It is safe to use `environment.nodeModulesDir` as this can not be
                // changed to the custom value.

                excludeDirs.add(File(environment.nodeModulesDir))
            }
        }
    }

    /**
     * Overrides default values of [JsEnvironment].
     *
     * Please note, environment should be set up firstly to have the effect on the parts
     * of the extension that depend on it.
     */
    fun environment(overridings: ConfigurableJsEnvironment.() -> Unit) =
        environment.run(overridings)

    /**
     * Configures [JS-related tasks][JsTasks].
     */
    fun tasks(configurations: JsTasks.() -> Unit) =
        tasks.run(configurations)
}
