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

import io.spine.internal.gradle.js.task.compileProtoToJs
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskCollection
import org.gradle.kotlin.dsl.withGroovyBuilder

/**
 * Configures `Protobuf` plugin.
 *
 * In particular, this method:
 *
 *  1. Specifies `protoc` compiler;
 *  2. Tunes `GenerateProtoTask` tasks for JavaScript code generation;
 *  3. Binds those tasks to [compileProtoToJs] task execution.
 */
fun JsPlugins.protobuf() = project.withGroovyBuilder {

    // TODO - Create an issue to `config` repository describing why `GroovyBuilder` is used.

    "protobuf" {

        setProperty("generatedFilesBaseDir", projectDir.path)

        "protoc" {
            setProperty("artifact", io.spine.internal.dependency.Protobuf.compiler)
        }

        "generateProtoTasks" {

            ("all"() as TaskCollection<*>).forEach { task ->

                task.withGroovyBuilder {
                    "builtins" {

                        // We don't need java builtin output in this project.

                        "remove"("name" to "java")

                        // For information on JavaScript code generation please see
                        // https://github.com/google/protobuf/blob/master/js/README.md

                        "create"("js") {
                            invokeMethod("option", "import_style=commonjs")
                            setProperty("outputSubDir", "proto")
                        }
                    }

                    val sourceSetName = (getProperty("sourceSet") as SourceSet).name
                    val testClassifier = if (sourceSetName == "test") "_test" else "test"
                    val projectDesc = "${project.group}_${project.name}_${project.version}"
                    val descriptorName = "$projectDesc$testClassifier.desc"

                    setProperty("generateDescriptorSet", true)
                    getProperty("descriptorSetOptions").withGroovyBuilder {
                        setProperty(
                            "path",
                            "${projectDir}/build/descriptors/${sourceSetName}/${descriptorName}"
                        )
                    }
                }

                compileProtoToJs.dependsOn(task)
            }
        }
    }
}
