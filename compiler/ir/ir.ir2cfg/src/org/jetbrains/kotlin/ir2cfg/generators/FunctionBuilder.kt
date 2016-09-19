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

package org.jetbrains.kotlin.ir2cfg.generators

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir2cfg.builders.BasicBlockBuilder
import org.jetbrains.kotlin.ir2cfg.builders.BlockConnectorBuilder
import org.jetbrains.kotlin.ir2cfg.builders.ControlFlowGraphBuilder
import org.jetbrains.kotlin.ir2cfg.graph.BasicBlock
import org.jetbrains.kotlin.ir2cfg.graph.BlockConnector
import org.jetbrains.kotlin.ir2cfg.graph.ControlFlowGraph

class FunctionBuilder(val function: IrFunction)  : ControlFlowGraphBuilder {

    private val blockBuilderMap = mutableMapOf<IrElement, BasicBlockBuilder>()

    private var currentBlockBuilder: BasicBlockBuilder? = null

    private val blocks = mutableListOf<BasicBlock>()

    private val connectorBuilderMap = mutableMapOf<IrElement, BlockConnectorBuilder>()

    private val blockAfterConnectorMap = mutableMapOf<BasicBlockBuilder, BlockConnectorBuilder>()

    private fun createBlockBuilder(after: BlockConnectorBuilder?): BasicBlockBuilder {
        val result = GeneralBlockBuilder()
        if (after != null) {
            blockAfterConnectorMap[result] = after
        }
        currentBlockBuilder = result
        return result
    }

    private fun BasicBlockBuilder.shiftTo(element: IrElement) {
        blockBuilderMap.remove(last())
        add(element)
        blockBuilderMap[element] = this
    }

    override fun add(element: IrElement) {
        val blockBuilder = currentBlockBuilder ?: createBlockBuilder(connectorBuilderMap[element])
        blockBuilder.shiftTo(element)
    }

    override fun add(element: IrElement, after: IrElement?) {
        if (after == null) add(element)
        val blockBuilder = blockBuilderMap[after]
                           ?: connectorBuilderMap[after]?.let { createBlockBuilder(it) }
                           ?: throw AssertionError("Function generator may add an element only to the end of a block or to connector")
        currentBlockBuilder = blockBuilder
        blockBuilder.shiftTo(element)
    }

    override fun jump(element: IrElement) {
        val blockBuilder = currentBlockBuilder ?: throw AssertionError("Function generator: no default block builder at connector")
        val block = blockBuilder.build()
        blocks.add(block)
        blockBuilderMap.values.remove(blockBuilder)
        currentBlockBuilder = null
        val nextConnectorBuilder = connectorBuilderMap[element] ?: GeneralConnectorBuilder(element)
        nextConnectorBuilder.addPrevious(block)
        val previousConnectorBuilder = blockAfterConnectorMap[blockBuilder]
        previousConnectorBuilder?.addNext(block)
        connectorBuilderMap[element] = nextConnectorBuilder
    }

    override fun jump(element: IrElement, after: IrElement) {
        currentBlockBuilder = blockBuilderMap[after]
                              ?: throw AssertionError("Function generator cannot add an element not at the end of a block")
        jump(element)
    }

    override fun build(): ControlFlowGraph {
        for (blockBuilder in blockBuilderMap.values) {
            if (currentBlockBuilder == blockBuilder) {
                currentBlockBuilder = null
            }
            val block = blockBuilder.build()
            blocks.add(block)
            val connectorBuilder = blockAfterConnectorMap[blockBuilder] ?: continue
            connectorBuilder.addNext(block)
        }
        val connectors = mutableListOf<BlockConnector>()
        for (connectorBuilder in connectorBuilderMap.values) {
            connectors.add(connectorBuilder.build())
        }
        return ControlFlowGraphImpl(function, blocks, connectors)
    }
}
