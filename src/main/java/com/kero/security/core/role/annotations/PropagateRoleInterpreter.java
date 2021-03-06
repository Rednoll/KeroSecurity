package com.kero.security.core.role.annotations;

import com.kero.security.core.agent.KeroAccessAgent;
import com.kero.security.core.annotations.PropertyAnnotationInterpreterBase;
import com.kero.security.core.property.configurator.SinglePropertyConfigurator;

public class PropagateRoleInterpreter extends PropertyAnnotationInterpreterBase<PropagateRole> {

	public PropagateRoleInterpreter(KeroAccessAgent agent) {
		super(agent);
	
	}

	@Override
	public void interpret(SinglePropertyConfigurator configurator, PropagateRole annotation) {
	
		configurator
			.propagateRole(annotation.from(), annotation.to());
	}
}
