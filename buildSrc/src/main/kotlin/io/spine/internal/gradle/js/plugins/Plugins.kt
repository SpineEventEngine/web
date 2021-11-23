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

import io.spine.internal.gradle.js.JsContext
import io.spine.internal.gradle.js.JsEnvironment
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer

/**
 * A scope for applying and configuring of JavaScript-related plugins.
 *
 * The scope extends [JsContext] and provides access to the project's [TaskContainer].
 *
 * Supposing, one needs to apply and configure `FooBar` plugin. To achieve that,
 * several steps should be performed:
 *
 *  1. Define the corresponding extension function upon this scope;
 *  2. Apply and configure the plugin inside that function;
 *  3. Call that function in your `build.gradle.kts` file.
 *
 * Here's an example of `js/plugins/FooBar.kt`:
 *
 * ```
 * fun JsPlugins.fooBar() {
 *     plugins.apply("com.fooBar")
 *     extensions.configure<FooBarExtension> {
 *         // ...
 *     }
 * }
 * ```
 *
 * And how to apply this in `build.gradle.kts`:
 *
 *  ```
 * import io.spine.internal.gradle.js.javascript
 * import io.spine.internal.gradle.js.plugins.fooBar
 *
 * // ...
 *
 * javascript {
 *     plugins {
 *         fooBar()
 *     }
 * }
 *  ```
 */
class JsPlugins(jsEnv: JsEnvironment, internal val project: Project)
    : JsContext(jsEnv, project), TaskContainer by project.tasks
{
    internal val plugins = project.plugins
    internal val extensions = project.extensions
}
