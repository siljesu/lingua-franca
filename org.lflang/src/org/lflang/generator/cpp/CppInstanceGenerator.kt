/*************
 * Copyright (c) 2021, TU Dresden.

 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:

 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.

 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ***************/

package org.lflang.generator.cpp

import org.lflang.isOfTimeType
import org.lflang.lf.Instantiation
import org.lflang.lf.Parameter
import org.lflang.lf.Reactor

/** A code genarator for reactor instances */
class CppInstanceGenerator(
    private val reactor: Reactor,
    private val parameterGenerator: CppParameterGenerator,
    private val fileConfig: CppFileConfig
) {

    private val Instantiation.type: String
        get() {
            return if (this.reactor.isGeneric)
                """${this.reactor.name}<${this.typeParms.joinToString(", ")}>}"""
            else
                this.reactor.name
        }

    private fun generateDeclaration(inst: Instantiation): String {
        return if (inst.isBank)
            "std::array<std::unique_ptr<${inst.type}>, ${inst.width}> ${inst.name};"
        else
            "std::unique_ptr<${inst.type}> ${inst.name};"
    }

    private fun Instantiation.getParameterValue(param: Parameter, instanceId: Int? = null): String {
        val assignment = this.parameters.firstOrNull { it.lhs === param }

        return if (instanceId != null && param.name == "instance") {
            // If we are in a bank instantiation (instanceId != null), then assign the instanceId
            // to the parameter named "instance"
            instanceId.toString()
        } else if (assignment == null) {
            // If no assignment was found, then the parameter is not overwritten and we assign the
            // default value
            with(parameterGenerator) { param.defaultValue }
        } else {
            // Otherwise, we use the assigned value.
            val initializers = assignment.rhs.map { if (param.isOfTimeType) it.toTime() else it.toCode() }
            with(parameterGenerator) { param.generateInstance(initializers) }
        }
    }

    private fun generateInitializer(inst: Instantiation): String {
        val parameters = inst.reactor.parameters
        return if (inst.isBank) {
            val initializations = if (parameters.isEmpty()) {
                (0 until inst.width).joinToString(", ") {
                    """std::make_unique<${inst.type}>("${inst.name}_$it", this)"""
                }
            } else {
                (0 until inst.width).joinToString(", ") {
                    val params = parameters.joinToString(", ") { param -> inst.getParameterValue(param, it) }
                    """, ${inst.name}(std::make_unique<${inst.type}>("${inst.name}", this, $params))"""
                }
            }
            """, ${inst.name}{{$initializations}}"""
        } else {
            if (parameters.isEmpty())
                """, ${inst.name}(std::make_unique<${inst.type}>("${inst.name}", this))"""
            else {
                val params = parameters.joinToString(", ") { inst.getParameterValue(it) }
                """, ${inst.name}(std::make_unique<${inst.type}>("${inst.name}", this, $params))"""
            }
        }
    }

    /** Generate C++ include statements for each reactor that is instantiated */
    fun generateIncludes(): String =
        reactor.instantiations.map { fileConfig.getReactorHeaderPath(it.reactor) }
            .distinct()
            .joinToString(separator = "\n") { """#include "${it.toUnixString()}" """ }

    /** Generate declaration statements for all reactor instantiations */
    fun generateDeclarations(): String {
        // FIXME: Does not support parameter values for widths.
        return reactor.instantiations.joinToString(
            prefix = "// reactor instances\n",
            separator = "\n"
        ) { generateDeclaration(it) }
    }

    /** Generate constructor initializers for all reactor instantiations */
    // FIXME: Does not support parameter values for widths.
    fun generateInitializers(): String =
        reactor.instantiations.joinToString(prefix = "//reactor instances\n", separator = "\n") { generateInitializer(it) }
}
