/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.ir2cfg.builders

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir2cfg.graph.BasicBlock
import org.jetbrains.kotlin.ir2cfg.graph.ControlFlowGraph

interface ControlFlowGraphBuilder {

    fun add(element: IrElement)

    fun add(element: IrElement, after: IrElement?)

    fun add(block: BasicBlock) {
        for (element in block.elements) {
            add(element)
        }
    }

    fun add(block: BasicBlock, after: IrElement) {
        var last = after
        for (element in block.elements) {
            add(element, after = last)
            last = element
        }
    }

    fun jump(element: IrElement)

    fun jump(element: IrElement, after: IrElement)

    fun build(): ControlFlowGraph
}