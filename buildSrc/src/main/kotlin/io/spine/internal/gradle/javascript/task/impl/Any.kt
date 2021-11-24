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

import io.spine.internal.gradle.base.check
import io.spine.internal.gradle.javascript.isWindows
import io.spine.internal.gradle.javascript.task.JsTaskConfiguring
import io.spine.internal.gradle.javascript.task.JsTaskRegistering
import io.spine.internal.gradle.javascript.task.buildJs
import io.spine.internal.gradle.javascript.task.coverageJs
import io.spine.internal.gradle.javascript.task.prepareJsPublication
import io.spine.internal.gradle.javascript.task.testJs
import io.spine.internal.gradle.javascript.task.transpileSources
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.create

fun JsTaskRegistering.other() {

    check.dependsOn(
        coverageJs()
    )

    deleteCompiled()
    copyBundledJs()
    transpileSources()

    // TODO:2021-09-22:alexander.yevsyukov: Resolve this dependency.
    // generateLicenseReport.finalizedBy npmLicenseReport
    npmLicenseReport()
}

fun JsTaskConfiguring.webPack() {

    // one should have created dedicated tasks for this

    /**
     * Customizes the task already defined in `config` module by running Webpack build.
     */
    buildJs.apply {

        outputs.dir(webPackOutput)

        doLast {
            npm("run", "build")
            npm("run", "build-dev")
        }
    }

    /**
     * Customizes the task already defined in `config` module by running
     * the JavaScript tests.
     */
    testJs.apply {

        doLast {
            npm("run", "test")
        }
    }
}

/**
 * Defines files to copy by the task.
 */
fun JsTaskConfiguring.publish() = prepareJsPublication.apply {

    from(
        packageJson,
        npmrcFile
    )

    into(publicationDir)

    dependsOn(

        // TODO:2019-02-05:dmytro.grankin: Temporarily don't publish a bundle.
        // See: https://github.com/SpineEventEngine/web/issues/61

        //copyBundledJs,

        transpileSources,
    )
}

private fun JsTaskRegistering.coverageJs() =
    create("coverageJs") {

        description = "Runs the JavaScript tests and collects the code coverage info."
        group = jsAnyTask

        outputs.dir(nycOutput)

        doLast {
            npm("run", if(isWindows()) "coverage:win" else "coverage:unix")
        }

        dependsOn(buildJs)
    }

private fun JsTaskRegistering.deleteCompiled() =
    create<Delete>("deleteCompiled") {

        description = "Cleans old module dependencies and build outputs."
        group = jsAnyTask

        delete(
            genProtoMain,
            genProtoTest,
            coverageJs.outputs
        )
    }

private fun JsTaskRegistering.copyBundledJs() =
    create<Copy>("copyBundledJs") {

        description = "Copies bundled JavaScript sources to the NPM publication directory."
        group = jsAnyTask

        from(buildJs.outputs)
        into(webPackPublicationDir)
    }

private fun JsTaskRegistering.transpileSources() =
    create("transpileSources") {

        description = "Transpiles sources before publishing."
        group = jsAnyTask

        doLast {
            npm("run", "transpile-before-publishing")
        }
    }

private fun JsTaskRegistering.npmLicenseReport() =
    create("npmLicenseReport") {

        description = "Generates the report on NPM dependencies and their licenses."
        group = jsAnyTask

        doLast {
            npm("run", "license-report")
        }
    }
