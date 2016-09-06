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

package org.jetbrains.kotlin.ir2cfg.util

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir2cfg.graph.BasicBlock
import org.jetbrains.kotlin.ir2cfg.graph.BlockConnector
import org.jetbrains.kotlin.ir2cfg.graph.ControlFlowGraph
import org.jetbrains.kotlin.ir2cfg.nodes.IrCfgSink
import java.util.*

private fun IrElement.cfgDump() = if (this is IrCfgSink) "$this" else dump()

fun BasicBlock.dump(builder: StringBuilder = StringBuilder(), indent: String = ""): String {
    for ((index, element) in elements.withIndex()) {
        builder.append(indent)
        builder.append(String.format("%3d ", index + 1))
        val dump = element.cfgDump()
        builder.appendln(dump.lines().first())
    }
    return builder.toString()
}

fun BlockConnector.dump(builder: StringBuilder = StringBuilder(), indent: String = ""): String {
    builder.append(indent)
    val dump = element.cfgDump()
    builder.appendln(dump.lines().first())
    return builder.toString()
}

fun ControlFlowGraph.incomingConnectors(basicBlock: BasicBlock) = connectors.filter { basicBlock in it.nextBlocks }

fun ControlFlowGraph.outgoingConnectors(basicBlock: BasicBlock) = connectors.filter { basicBlock in it.previousBlocks }

fun ControlFlowGraph.dump(): String {
    val builder = StringBuilder()
    val blockQueue = LinkedList<BasicBlock>()
    val visitedBlocks = hashSetOf<BasicBlock>()
    blockQueue.add(enterBlock)
    var index = 0
    while (blockQueue.isNotEmpty()) {
        val block = blockQueue.poll()
        if (block in visitedBlocks) continue
        visitedBlocks.add(block)
        builder.appendln("BB ${index++}")
        val incoming = incomingConnectors(block)
        if (incoming.isNotEmpty()) {
            builder.appendln("INCOMING")
            for (connector in incoming) {
                connector.dump(builder, "        ")
            }
        }
        builder.appendln("CONTENT")
        block.dump(builder, "    ")
        val outgoing = outgoingConnectors(block)
        if (outgoing.isNotEmpty()) {
            builder.appendln("OUTGOING")
            for (connector in outgoing) {
                connector.dump(builder, "        ")
                for (next in connector.nextBlocks) {
                    blockQueue.add(next)
                }
            }
        }
    }
    return builder.toString()
}