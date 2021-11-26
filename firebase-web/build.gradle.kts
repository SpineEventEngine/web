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

import com.google.protobuf.gradle.builtins
import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import io.spine.internal.dependency.Firebase
import io.spine.internal.dependency.HttpClient
import io.spine.internal.dependency.Jackson
import io.spine.internal.dependency.Protobuf
import io.spine.internal.gradle.checkstyle.CheckStyleConfig
import io.spine.internal.gradle.excludeProtobufLite

plugins {
    id("io.spine.mc-java")
}

group = "io.spine.gcloud"

configurations.excludeProtobufLite()

val spineBaseTypesVersion: String by extra

dependencies {
    api("io.spine:spine-base-types:$spineBaseTypesVersion")
    api(project(":web"))
    api(Firebase.admin) {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "io.grpc")
    }

    implementation(Jackson.databind)
    implementation(HttpClient.apache)

    // Required by the Firebase Admin SDK.
    @Suppress("DEPRECATION", "RemoveRedundantQualifierName")
    runtimeOnly(io.spine.internal.dependency.Slf4J.lib)

    testImplementation(project(":testutil-web"))
}

val compileProtoToJs by tasks.registering {
    description = "Compiles Protobuf sources into JavaScript."
}

CheckStyleConfig.applyTo(project)

protobuf {
    protoc {
        artifact = Protobuf.compiler
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                id("js") {
                    // For information on JavaScript code generation please see
                    // https://github.com/google/protobuf/blob/master/js/README.md
                    option("import_style=commonjs")
                }
            }
            compileProtoToJs {
                dependsOn(task)
            }
        }
    }
}
