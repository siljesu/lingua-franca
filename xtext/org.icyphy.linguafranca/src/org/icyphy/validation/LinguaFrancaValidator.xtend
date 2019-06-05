/*
 * generated by Xtext 2.17.0
 */
package org.icyphy.validation

import org.eclipse.xtext.validation.Check
import org.icyphy.generator.GeneratorBase
import org.icyphy.linguaFranca.Action
import org.icyphy.linguaFranca.Assignment
import org.icyphy.linguaFranca.Input
import org.icyphy.linguaFranca.Instance
import org.icyphy.linguaFranca.LinguaFrancaPackage.Literals
import org.icyphy.linguaFranca.Model
import org.icyphy.linguaFranca.Output
import org.icyphy.linguaFranca.Param
import org.icyphy.linguaFranca.Produces
import org.icyphy.linguaFranca.Reaction
import org.icyphy.linguaFranca.Reactor
import org.icyphy.linguaFranca.Target
import org.icyphy.linguaFranca.Time
import org.icyphy.linguaFranca.Timer
import org.icyphy.linguaFranca.Uses

/**
 * This class contains custom validation rules. 
 *
 * See https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#validation
 */
class LinguaFrancaValidator extends AbstractLinguaFrancaValidator {
	
	public static val KNOWN_TARGETS = #{'Accessor', 'Accessors', 'C'}
	
	var parameters = newHashSet()
	var inputs = newHashSet()
	var outputs = newHashSet()
	var timers = newHashSet()
	var actions = newHashSet()
	var allNames = newHashSet()
	var containedNames = newHashSet() // Names of contained reactors.
	
	////////////////////////////////////////////////////
	//// Functions to set up data structures for performing checks.
	
	// FAST ensures that these checks run whenever a file is modified.
	// Alternatives are NORMAL (when saving) and EXPENSIVE (only when right-click, validate).
	@Check(FAST)
	def resetSets(Reactor reactor) {
		parameters.clear()
		inputs.clear()
		outputs.clear()
		timers.clear()
		actions.clear()
		allNames.clear()
		containedNames.clear()
	}
	
	@Check(FAST)
	def recordParameter(Param param) {
		parameters.add(param.name)
		allNames.add(param.name)
	}
	
	////////////////////////////////////////////////////
	//// The following checks are in alphabetical order.
		
	@Check(FAST)
	def checkAction(Action action) {
		if (allNames.contains(action.name)) {
			error("Names of parameters, inputs, timers, and actions must be unique: " 
				+ action.name,
				Literals.ACTION__NAME
			)
		}
		actions.add(action.name);
		allNames.add(action.name)
	}

	@Check(FAST)
	def checkAssignment(Assignment assignment) {
		if (assignment.unit !== null
				&& GeneratorBase.timeUnitsToNs.get(assignment.unit) === null) {
			error("Invalid time units: " + assignment.unit
					+ ". Should be one of "
					+ GeneratorBase.timeUnitsToNs.keySet,
					Literals.ASSIGNMENT__UNIT)
		}
	}

	@Check(FAST)
	def checkGets(Uses uses) {
		for (get: uses.uses) {
			if (!inputs.contains(get)) {
					error("Reaction declares that it reads something that is not an input: "
					+ get,
					Literals.USES__USES
				)
			}
		}
	}

	@Check(FAST)
	def checkInput(Input input) {
		if (allNames.contains(input.name)) {
			error("Names of parameters, inputs, timers, and actions must be unique: " 
				+ input.name,
				Literals.INPUT__NAME
			)
		}
		inputs.add(input.name);
		allNames.add(input.name)
	}
	
	@Check(FAST)
	def checkInstance(Instance instance) {
		if (containedNames.contains(instance.name)) {
			error("Names of instances must be unique: " 
				+ instance.name,
				Literals.INSTANCE__NAME
			)
		}
		containedNames.add(instance.name)
	}
	
	@Check(FAST)
	def checkModel(Model model) {
		for (reactor: model.reactors) {
			if (reactor.name.equalsIgnoreCase("main")) {
				return
			}
		}
		warning("No Main reactor.", Literals.MODEL__REACTORS)
	}
	
	@Check(FAST)
	def checkOutput(Output output) {
		if (allNames.contains(output.name)) {
			error("Names of parameters, inputs, timers, and actions must be unique: " 
				+ output.name,
				Literals.OUTPUT__NAME
			)
		}
		outputs.add(output.name);
		allNames.add(output.name)
	}
		
	@Check(FAST)
	def checkReaction(Reaction reaction) {
		for (trigger: reaction.triggers) {
			if (!inputs.contains(trigger)
				&& !timers.contains(trigger)
				&& !actions.contains(trigger)
			) {
				error("Reaction trigger is not an input, timer, or action: "
					+ trigger,
					Literals.REACTION__TRIGGERS
				)
			}
		}
	}

	@Check(FAST)
	def checkSets(Produces produces) {
		for (port: produces.produces) {
			// If the port has the form of name.name, then skip the check.
			// We don't have enough information here to check it.
			if (port.split('\\.').length != 2 && !outputs.contains(port) && !actions.contains(port)) {
					error("Reaction declares that it produces something that is not an output,"
						+ " an action, nor an input port: "
					+ port,
					Literals.PRODUCES__PRODUCES
				)
			}
		}
	}
	
	@Check(FAST)
	def checkTarget(Target target) {
		if (!KNOWN_TARGETS.contains(target.name)) {
			warning("Unrecognized target: "
					+ target.name,
					Literals.TARGET__NAME)
		}
	}

	@Check(FAST)
	def checkTime(Time time) {
		if (time.time !== null) {
			if (time.unit === null) {
				// No time unit is given.
				if (time.time.startsWith('-')) {
					error("Time cannot be negative", Literals.TIME__TIME)
				} else if (time.time.matches('[0123456789]')) {
					// Time is a literal number (only checked the first character)
					if (!time.time.equals('0')) {
						error("Missing time units. Should be one of "
								+ GeneratorBase.timeUnitsToNs.keySet,
								Literals.TIME__TIME)
					}
				}
			} else if (GeneratorBase.timeUnitsToNs.get(time.unit) === null) {
				error("Invalid time units: " + time.unit
						+ ". Should be one of "
						+ GeneratorBase.timeUnitsToNs.keySet,
						Literals.TIME__UNIT)
			}
		}
	}
	
	@Check(FAST)
	def checkTimer(Timer timer) {
		if (allNames.contains(timer.name)) {
			error("Names of parameters, inputs, timers, and actions must be unique: " 
				+ timer.name,
				Literals.TIMER__NAME
			)
		}
		timers.add(timer.name);
		allNames.add(timer.name)
	}
}
