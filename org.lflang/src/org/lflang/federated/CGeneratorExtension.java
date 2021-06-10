/*************
 * Copyright (c) 2021, The University of California at Berkeley.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ***************/

package org.lflang.federated;

import org.lflang.ASTUtils;
import org.lflang.generator.CGenerator;
import org.lflang.generator.FederateInstance;
import org.lflang.generator.ReactorInstance;
import org.lflang.lf.Input;
import org.lflang.lf.Port;
import org.lflang.lf.Reactor;
import org.lflang.lf.ReactorDecl;
import org.lflang.lf.VarRef;

/**
 * An extension class to the CGenerator that enables certain federated
 * functionalities. Currently, this class offers the following features:
 * 
 * - Allocating and initializing C structures for federated communication -
 * Creating status field for network input ports that help the receiver logic in
 * federate.c communicate the status of a network input port with network input
 * control reactions.
 * 
 * @author Soroush Bateni {soroush@utdallas.edu}
 *
 */
public class CGeneratorExtension {

    /**
     * Generate C code that allocates sufficient memory for the following two
     * critical data structures that support network control reactions: 
     *  - triggers_for_network_input_control_reactions: These are triggers that are
     *  used at runtime to insert network input control reactions into the
     *  reaction queue. 
     *  - trigger_for_network_output_control_reactions: Triggers for
     *  network output control reactions, which are unique per each output port.
     *  There could be multiple network output control reactions for each network
     *  output port if it is connected to multiple downstream federates.
     * 
     * @param federate  The top-level federate instance
     * @param generator The instance of the CGenerator passed to keep this
     *                  extension function static.
     * @return A string that allocates memory for the aforementioned three
     *         structures.
     */
    public static String allocateTriggersForFederate(FederateInstance federate,
            CGenerator generator) {

        StringBuilder builder = new StringBuilder();

        // Create the table to initialize intended tag fields to 0 between time
        // steps.
        if (generator.isFederatedAndDecentralized()
                && generator.startTimeStepIsPresentCount > 0) {
            // Allocate the initial (before mutations) array of pointers to
            // intended_tag fields.
            // There is a 1-1 map between structs containing is_present and
            // intended_tag fields,
            // thus, we reuse startTimeStepIsPresentCount as the counter.
            builder.append(
                    "// Create the array that will contain pointers to intended_tag fields to reset on each step.\n"
                            + "__intended_tag_fields_size = "
                            + generator.startTimeStepIsPresentCount + ";\n"
                            + "__intended_tag_fields = (tag_t**)malloc(__intended_tag_fields_size * sizeof(tag_t*));\n");
        }

        if (generator.isFederated) {
            if (federate.networkInputControlReactionsTriggers.size() > 0) {
                // Proliferate the network input control reaction trigger array
                builder.append(
                        "// Initialize the array of pointers to network input port triggers\n"
                                + "_fed.triggers_for_network_input_control_reactions_size = "
                                + federate.networkInputControlReactionsTriggers
                                .size()
                                + ";\n"
                                + "_fed.triggers_for_network_input_control_reactions = (trigger_t**)malloc("
                                + "_fed.triggers_for_network_input_control_reactions_size * sizeof(trigger_t*)"
                                + ");\n");

            }
        }

        return builder.toString();
    }

    /**
     * Generate C code that initializes three critical structures that support
     * network control reactions: 
     *  - triggers_for_network_input_control_reactions: These are triggers that are
     *  used at runtime to insert network input control reactions into the
     *  reaction queue. There could be multiple network input control reactions
     *  for one network input at multiple levels in the hierarchy. 
     *  - trigger_for_network_output_control_reactions: Triggers for
     *  network output control reactions, which are unique per each output port.
     *  There could be multiple network output control reactions for each network
     *  output port if it is connected to multiple downstream federates.
     * 
     * @param instance  The reactor instance that is at any level of the
     *                  hierarchy within the federate.
     * @param federate  The top-level federate
     * @param generator The instance of the CGenerator passed to keep this
     *                  extension function static.
     * @return A string that initializes the aforementioned three structures.
     */
    public static StringBuilder initializeTriggerForControlReactions(
            ReactorInstance instance, FederateInstance federate,
            CGenerator generator) {

        StringBuilder builder = new StringBuilder();

        // The network control reactions are always in the main federated
        // reactor
        if (instance != generator.main) {
            return builder;
        }

        ReactorDecl reactorClass = instance.getDefinition().getReactorClass();
        Reactor reactor = ASTUtils.toDefinition(reactorClass);
        String nameOfSelfStruct = CGenerator.selfStructName(instance);

        // Initialize triggers for network input control reactions
        for (Port trigger : federate.networkInputControlReactionsTriggers) {
            // Check if the trigger belongs to this reactor instance
            if (ASTUtils.allReactions(reactor).stream().anyMatch(r -> {
                return r.getTriggers().stream().anyMatch(t -> {
                    if (t instanceof VarRef) {
                        return ((VarRef) t).getVariable().equals(trigger);
                    } else {
                        return false;
                    }
                });
            })) {
                // Initialize the triggers_for_network_input_control_reactions for the input
                builder.append("// Add trigger " + nameOfSelfStruct + "->___"
                        + trigger.getName()
                        + " to the global list of network input ports.\n"
                        + "_fed.triggers_for_network_input_control_reactions["
                        + federate.networkInputControlReactionsTriggers
                        .indexOf(trigger)
                        + "]= &" + nameOfSelfStruct + "" + "->___"
                        + trigger.getName() + ";\n");
            }
        }

        nameOfSelfStruct = CGenerator.selfStructName(instance);

        // Initialize the trigger for network output control reactions if it doesn't exists
        if (federate.networkOutputControlReactionsTrigger != null) {
            builder.append("_fed.trigger_for_network_output_control_reactions=&"
                    + nameOfSelfStruct
                    + "->___outputControlReactionTrigger;\n");
        }

        return builder;
    }

    /**
     * Create a port status field variable for a network input port "input" in
     * the self struct of a reactor.
     * 
     * @param input     The network input port
     * @param generator The instance of the CGenerator
     * @return A string containing the appropriate variable
     */
    public static String createPortStatusFieldForInput(Input input,
            CGenerator generator) {
        StringBuilder builder = new StringBuilder();
        // Check if the port is a multiport
        if (generator.isMultiport(input)) {
            // If it is a multiport, then create an auxiliary list of port
            // triggers for each channel of
            // the multiport to keep track of the status of each channel
            // individually
            builder.append("trigger_t* ___" + input.getName()
            + "_network_port_status;\n");
        } else {
            // If it is not a multiport, then we could re-use the port trigger,
            // and nothing needs to be
            // done
        }
        return builder.toString();
    }
}
