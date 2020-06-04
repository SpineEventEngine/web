/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.protobuf.gradle.*
import io.spine.gradle.internal.DependencyResolution
import io.spine.gradle.internal.Deps
import io.spine.gradle.internal.IncrementGuard
import io.spine.gradle.internal.servletApi

plugins {
    id("io.spine.tools.spine-model-compiler")
}

DependencyResolution.excludeProtobufLite(configurations)

apply<IncrementGuard>()
apply(from = Deps.scripts.modelCompiler(project))

val spineBaseVersion: String by extra
val spineCoreVersion: String by extra

dependencies {
    api(Deps.build.servletApi)
    api("io.spine:spine-server:$spineCoreVersion")
    api(Deps.build.googleHttpClient)

    implementation(Deps.build.googleHttpClientApache)

    testImplementation(project(":testutil-web"))
    testImplementation("io.spine.tools:spine-mute-logging:$spineBaseVersion")
}

val compileProtoToJs by tasks.registering {
    description = "Compiles Protobuf sources into JavaScript."
}

protobuf {
    protoc {
        artifact = Deps.build.protoc
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
