/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.internal.gradle.typescript.plugin

import com.google.protobuf.gradle.ProtobufExtension
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.remove
import io.spine.internal.dependency.Protobuf

/**
 * Applies and configures `protobuf` plugin to work with a TypeScript module.
 *
 * In particular, this method:
 *
 *  1. Specifies an executable for `protoc` compiler.
 *  2. Configures `GenerateProtoTask`.
 *
 * @see TsPlugins
 */
fun TsPlugins.protobuf() {

    plugins {
        apply(Protobuf.GradlePlugin.id)
    }

    val protobufExt = project.extensions.getByType(ProtobufExtension::class.java)
    protobufExt.apply {

        generatedFilesBaseDir = projectDir.path

        protoc {
            artifact = Protobuf.JsRenderingProtoc.compiler
        }

        plugins {
            // `buf-es` must match the plugin ID in `generateProtoTasks { ... plugins {} }`
            create("buf-es") {
                path  = "${project.rootDir}/node_modules/.bin/protoc-gen-es"
            }
        }

        generateProtoTasks {
            all().forEach { task ->

                task.builtins {

                    // Do not use `java` builtin in this project.
                    remove("java")
                }

                task.plugins {
                    id("buf-es") {
                    }
                }

                val sourceSet = task.sourceSet.name
                val testClassifier = if (sourceSet == "test") "_test" else ""
                val artifact = "${project.group}_${project.name}_${moduleVersion}"
                val descriptor = "$artifact$testClassifier.desc"

                task.generateDescriptorSet = true
                task.descriptorSetOptions.path =
                    "${projectDir}/build/descriptors/${sourceSet}/${descriptor}"
            }
        }
    }
}
