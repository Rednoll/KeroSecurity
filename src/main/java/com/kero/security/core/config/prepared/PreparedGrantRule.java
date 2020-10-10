package com.kero.security.core.config.prepared;

import java.lang.reflect.Method;
import java.util.Collection;

import com.kero.security.core.agent.KeroAccessAgent;
import com.kero.security.core.role.Role;
import com.kero.security.core.scheme.AccessScheme;

public class PreparedGrantRule extends PreparedActionBase implements PreparedAction {
	
	private Collection<Role> propagatedRoles;
	
	public PreparedGrantRule(AccessScheme scheme, Collection<Role> propogatedRoles) {
		super(scheme);
	
		this.propagatedRoles = propogatedRoles;
	}

	@Override
	public Object process(Method method, Object original, Object[] args) {
		
		try {
		
			Object methodResult = method.invoke(original, args);
			
			KeroAccessAgent agent = this.scheme.getAgent();
			
			methodResult = agent.protectWithoutCast(methodResult, this.propagatedRoles);

			return methodResult;
		}
		catch(Exception e) {
			
			throw new RuntimeException(e);
		}
	}
}
