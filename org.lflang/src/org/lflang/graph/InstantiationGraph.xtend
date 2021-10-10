/*************
Copyright (c) 2020, The University of California at Berkeley.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
***************/

package org.lflang.graph

import com.google.common.collect.HashMultimap
import java.util.List
import java.util.Set
import org.eclipse.emf.ecore.resource.Resource
import org.lflang.lf.Instantiation
import org.lflang.lf.Model
import org.lflang.lf.Reactor
import org.lflang.lf.ReactorDecl

import static extension org.lflang.ASTUtils.*

/**
 * A graph with vertices that are Reactors (not ReactorInstances) and edges that denote
 * dependencies between them. A "dependency" from reactor class A to 
 * reactor class B (A depends on B) means that A instantiates within 
 * it at least one instance of B. Note that there a potentially 
 * confusing and subtle distinction here between an "instantiation" 
 * and an "instance". They are not the same thing at all. An
 * "instantiation" is an AST node representing a statement like
 * `a = new A();`. This can result in many instances of reactor 
 * class A (if the containing reactor class is instantiated multiple times).
 * 
 * In addition to the graph, this class keeps track of the instantiations
 * that induce the dependencies. These can be retrieved using the method
 * `getInstantiations(Reactor)`.
 * 
 * @author{Marten Lohstroh <marten@berkeley.edu>}
 */
class InstantiationGraph extends PrecedenceGraph<Reactor> {

    /**
     * A mapping from reactors to the sites of their instantiation.
     */
    protected val reactorToInstantiation = HashMultimap.<Reactor, Instantiation>create
    
    /**
     * A mapping from reactor classes to their declarations.
     */
    protected val reactorToDecl = HashMultimap.<Reactor, ReactorDecl>create
    
    protected val flaggedReactors = newHashSet
    
    /**
     * Return the instantiations that point to a given reactor definition.
     * If none are known, returns an empty set. * The returned set may be
     * unmodifiable.
     */
    def Set<Instantiation> getInstantiations(Reactor definition) {
        return this.reactorToInstantiation.get(definition) ?: emptySet
    }
    
    /**
     * Return the declarations that point to a given reactor definition.
     * A declaration is either a reactor definition or an import statement.
     */
    def Set<ReactorDecl> getDeclarations(Reactor definition) {
        return this.reactorToDecl.get(definition)
    }
    
    /**
     * Return the reactor definitions referenced by instantiations in this graph
     * ordered topologically. Each reactor in the returned list is preceded by
     * any reactors that it may instantiate.
     */
    def List<Reactor> getReactors() {
        return this.nodesInTopologicalOrder
    }
    
    /**
     * Construct an instantiation graph based on the given AST and, if the
     * detectCycles argument is true, run Tarjan's algorithm to detect cyclic
     * dependencies between instantiations.
     * @param resource The resource associated with the AST.
     * @param detectCycles Whether or not to detect cycles.
     */
    new (Resource resource, boolean detectCycles) {
        val instantiations = resource.allContents.toIterable.filter(
            Instantiation)
        val main = resource.allContents.toIterable.filter(Reactor).findFirst[it.isMain || it.isFederated]
        if (main !== null) {
            this.addNode(main)
        }
        for (i : instantiations) {
            i.buildGraph(newHashSet)
        }
        detectCycles? this.detectCycles()
    }
    
    /**
     * Construct an instantiation graph based on the given AST and, if the
     * detectCycles argument is true, run Tarjan's algorithm to detect cyclic
     * dependencies between instantiations.
     * @param model The root of the AST.
     * @param detectCycles Whether or not to detect cycles.
     */
     new (Model model, boolean detectCycles) {
        for (r : model.reactors) {
            for (i : r.instantiations) {
                i.buildGraph(newHashSet)
            }
        }
        detectCycles? this.detectCycles()
    }

    /**
     * Traverse the AST and build this precedence graph relating the
     * encountered instantiations. Also map each reactor to all
     * declarations associated with it and each reactor to the sites of
     * its instantiations.
     * @param instantiation
     * @param graph
     */
    private def void buildGraph(Instantiation instantiation,
        Set<Instantiation> visited) {
        val decl = instantiation.reactorClass
        val reactor = decl.toDefinition
        val container = instantiation.eContainer as Reactor

        if (visited.add(instantiation)) {
            this.reactorToInstantiation.put(reactor, instantiation)
            this.reactorToDecl.put(reactor, decl)

            if (container !== null) {
                this.addEdge(container, reactor)
            } else {
                this.addNode(reactor)
            }
            for (inst : reactor.instantiations) {
                inst.buildGraph(visited)
            }
        }
    }
}
