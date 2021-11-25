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

package io.spine.internal.gradle.javascript.task.impl

import io.spine.internal.gradle.base.clean
import io.spine.internal.gradle.javascript.task.JsTaskRegistering
import io.spine.internal.gradle.javascript.task.assembleJs
import io.spine.internal.gradle.javascript.task.compileProtoToJs
import io.spine.internal.gradle.javascript.task.coverageJs
import io.spine.internal.gradle.javascript.task.installNodePackages
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.create

fun JsTaskRegistering.clean() =
    clean.dependsOn(
        cleanJs()
    )

private fun JsTaskRegistering.cleanJs() =
    create<Delete>("cleanJs") {

        description = "Cleans output of `assembleJs` task and output of its dependants."
        group = jsAnyTask

        delete(
            assembleJs.outputs,
            compileProtoToJs.outputs,
            installNodePackages.outputs,
        )

        dependsOn(
            cleanGenerated()
        )
    }

private fun JsTaskRegistering.cleanGenerated() =
    create<Delete>("cleanGenerated") {

        description = "Cleans generated code and reports."
        group = jsAnyTask

        delete(
            genProtoMain,
            genProtoTest,
            coverageJs.outputs,
            nycOutput,
        )
    }
