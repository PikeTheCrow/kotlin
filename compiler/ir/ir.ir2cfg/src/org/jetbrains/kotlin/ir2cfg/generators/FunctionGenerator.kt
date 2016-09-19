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
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir2cfg.graph.ControlFlowGraph
import org.jetbrains.kotlin.ir2cfg.nodes.IrCfgSink

class FunctionGenerator(val function: IrFunction) {

    val builder = FunctionBuilder(function)

    val exit = IrCfgSink(function, "Function exit")

    fun generate(): ControlFlowGraph {
        val visitor = FunctionVisitor()
        function.accept(visitor, true)
        return builder.build()
    }

    inner class FunctionVisitor : IrElementVisitor<IrElement?, Boolean> {

        inline fun <reified IE : IrElement> IE.process(includeSelf: Boolean = true) = this.accept(this@FunctionVisitor, includeSelf)

        override fun visitFunction(declaration: IrFunction, data: Boolean): IrElement? {
            if (data) {
                builder.add(declaration)
            }
            val result = declaration.body?.process()
            if (result != null && result !is IrReturn) {
                builder.jump(exit, after = result)
            }
            return result
        }

        private fun IrStatementContainer.process(): IrElement? {
            var result: IrElement? = null
            for (statement in statements) {
                result = statement.process()
            }
            return result
        }

        override fun visitBlockBody(body: IrBlockBody, data: Boolean): IrElement? {
            if (data) {
                builder.add(body)
            }
            return body.process() ?: body
        }

        override fun visitVariable(declaration: IrVariable, data: Boolean): IrElement? {
            declaration.initializer?.process()
            return if (data) {
                builder.add(declaration)
                declaration
            }
            else null
        }

        override fun visitReturn(expression: IrReturn, data: Boolean): IrElement? {
            val result = expression.value?.process()
            return if (data) {
                builder.add(expression, result)
                builder.jump(exit)
                expression
            }
            else null
        }

        override fun visitExpressionBody(body: IrExpressionBody, data: Boolean): IrElement? {
            if (data) {
                builder.add(body)
            }
            return body.expression.process()
        }

        override fun visitWhen(expression: IrWhen, data: Boolean): IrElement? {
            if (data) {
                builder.add(expression)
            }
            val sink = IrCfgSink(expression, "When exit")
            var previousCondition: IrExpression? = null
            for (branch in 0..expression.branchesCount - 1) {
                val condition = expression.getNthCondition(branch)
                                ?: throw AssertionError("No branch condition #$branch in when: $expression")
                condition.process(includeSelf = false)
                if (previousCondition != null) {
                    builder.jump(condition, previousCondition)
                }
                else {
                    builder.jump(condition)
                }
                previousCondition = condition
                val result = expression.getNthResult(branch)
                             ?: throw AssertionError("No branch result #$branch in when: $expression")
                builder.add(result, after = previousCondition)
                result.process(includeSelf = false)
                builder.jump(sink)
            }
            val elseBranch = expression.elseBranch
            if (elseBranch != null) {
                if (previousCondition != null) {
                    builder.add(elseBranch, after = previousCondition)
                }
                else {
                    builder.add(elseBranch)
                }
                elseBranch.process(includeSelf = false)
                builder.jump(sink)
            }
            return sink
        }

        override fun visitExpression(expression: IrExpression, data: Boolean): IrElement? {
            if (data) {
                builder.add(expression)
            }
            return expression
        }

        override fun visitElement(element: IrElement, data: Boolean): IrElement? {
            TODO("not implemented")
        }

    }
}