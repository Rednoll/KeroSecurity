package com.kero.security.core.interceptor.annotations;

import java.util.Set;

import com.kero.security.core.agent.KeroAccessAgent;
import com.kero.security.core.annotations.PropertyAnnotationInterpreterBase;
import com.kero.security.core.interceptor.DenyInterceptor;
import com.kero.security.core.interceptor.exceptions.DenyInterceptorConstructException;
import com.kero.security.core.property.configurator.SinglePropertyConfigurator;
import com.kero.security.core.role.Role;

public class DenyWithInterceptorInterpreter extends PropertyAnnotationInterpreterBase<DenyWithInterceptor> {

	public DenyWithInterceptorInterpreter(KeroAccessAgent agent) {
		super(agent);
	
	}

	@Override
	public void interpret(SinglePropertyConfigurator configurator, DenyWithInterceptor annotation) {
		
		Set<Role> roles = this.agent.getOrCreateRole(annotation.roles());
		
		Class<? extends DenyInterceptor> interceptorClass = annotation.value();
		
		DenyInterceptor interceptor;
		
		try {
			
			interceptor = interceptorClass.getConstructor().newInstance();
		}
		catch(Exception e) {
			
			throw new DenyInterceptorConstructException(interceptorClass.getCanonicalName());
		}
		
		interceptor.setRoles(roles);
		
		configurator
			.denyWithInterceptor(interceptor);
	}
}
