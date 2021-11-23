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

package io.spine.internal.gradle.js.plugins

import io.spine.internal.gradle.js.task.buildJs
import io.spine.internal.gradle.js.task.compileProtoToJs
import io.spine.internal.gradle.js.task.testJs
import org.gradle.api.Task
import org.gradle.kotlin.dsl.withGroovyBuilder

/**
 * Applies and configures `MsJsPlugin` in accordance with
 * the current [JsEnvironment][io.spine.internal.gradle.js.JsEnvironment].
 *
 * In particular, this method:
 *
 *  1. Specifies directories for generated code;
 *  2. Binds `generateParsersTask` to [buildJs] execution. The task generates JSON-parsing
 *     code for the JavaScript messages compiled from Protobuf.
 *
 * @see JsPlugins
 */
fun JsPlugins.mcJs() {

    plugins.apply("io.spine.mc-js")

    // Currently, it is done with the help of GroovyInterop.
    // due to impossibility of putting this plugin on `buildSrc` classpath.
    // See issue: https://github.com/SpineEventEngine/config/issues/298

    project.withGroovyBuilder {

        "protoJs" {

            setProperty("generatedMainDir", genProtoMain)
            setProperty("generatedTestDir", genProtoTest)

            val parsersTask = "generateParsersTask"() as Task

            parsersTask.dependsOn(compileProtoToJs)
            buildJs.dependsOn(parsersTask)
            testJs.dependsOn(buildJs)
        }
    }
}
