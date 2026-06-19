/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import com.google.protobuf.gradle.protobuf
import io.spine.dependency.lib.HttpClient
import io.spine.dependency.lib.JavaX
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.CoreJvm
import io.spine.gradle.publish.IncrementGuard
import io.spine.gradle.checkstyle.CheckStyleConfig

plugins {
    id("io.spine.core-jvm")
}

configurations.excludeProtobufLite()

apply<IncrementGuard>()

dependencies {
    api(JavaX.servletApi)
    api(CoreJvm.server)
    api(HttpClient.google)

    implementation(HttpClient.apache)

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
    // TODO: Restore Protobuf-to-JavaScript generation once the JS toolchain is
    //  compatible with the new `config`. The `js` output is no longer a `protoc`
    //  built-in (it requires the external `protoc-gen-js` plugin), and `spine-mc-js`
    //  is not published against the current Spine artifacts. See `client-js` and the
    //  commented-out JS modules in `settings.gradle.kts`.
    generateProtoTasks {
        all().forEach { task ->
            compileProtoToJs {
                dependsOn(task)
            }
        }
    }
}
