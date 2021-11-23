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

import org.gradle.kotlin.dsl.configure
import org.gradle.plugins.ide.idea.model.IdeaModel

/**
 * Applies and configures this [IDEA module][org.gradle.plugins.ide.idea.model.IdeaModule] in accordance with
 * the current [JsEnvironment][io.spine.internal.gradle.js.JsEnvironment].
 *
 * In particular, this method:
 *
 *  1. Specifies `sourceDirs` and `testSourceDirs`;
 *  2. Excludes directories with generated code or build cache.
 */
fun JsPlugins.ideaModule() {

    plugins.apply("org.gradle.idea")

    extensions.configure<IdeaModel> {
        module {
            sourceDirs.add(srcDir)
            testSourceDirs.add(testSrcDir)

            excludeDirs.addAll(
                listOf(
                    nodeModules,
                    nycOutput,
                    genProtoMain,
                    genProtoTest
                )
            )
        }
    }
}
