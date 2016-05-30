/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js

import org.jetbrains.kotlin.name.FqName

enum class PredefinedAnnotation(fqName: String) {
    LIBRARY("kotlin.js.library"),
    NATIVE("kotlin.js.native"),
    NATIVE_INVOKE("kotlin.js.nativeInvoke"),
    NATIVE_GETTER("kotlin.js.nativeGetter"),
    NATIVE_SETTER("kotlin.js.nativeSetter"),
    JS_NAME("kotlin.js.JsName");

    val fqName: FqName = FqName(fqName)

    companion object {
        val WITH_CUSTOM_NAME = setOf(LIBRARY, NATIVE)
    }
}
