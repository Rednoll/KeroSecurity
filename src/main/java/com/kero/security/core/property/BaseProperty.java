package com.kero.security.core.property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.kero.security.core.access.Access;
import com.kero.security.core.config.action.Action;
import com.kero.security.core.config.action.ActionDeny;
import com.kero.security.core.config.action.ActionGrant;
import com.kero.security.core.interceptor.DenyInterceptor;
import com.kero.security.core.property.exceptions.PropertyPrepareException;
import com.kero.security.core.property.exceptions.RoleCollisionException;
import com.kero.security.core.role.Role;
import com.kero.security.core.scheme.AccessScheme;

public class BaseProperty implements Property {

	private String name;
	
	private AccessScheme scheme;
	
	private Access defaultAccess = Access.UNKNOWN;
	
	private Set<Role> grantRoles = new HashSet<>();
	private Set<Role> denyRoles = new HashSet<>();
	
	private DenyInterceptor defaultInterceptor;
	private List<DenyInterceptor> interceptors = new LinkedList<>();
	
	private Map<Role, Role> rolesPropagation = new HashMap<>();
	
	public BaseProperty(AccessScheme scheme, String name) {
		
		this.scheme = scheme;
		this.name = name;
	}
	
	@Override
	public Access accessible(Collection<Role> rolesArg) {
	
		if(rolesArg.isEmpty()) return Access.UNKNOWN;
		
		Set<Role> roles = new HashSet<>(rolesArg);
			roles.removeAll(this.denyRoles);
		
		if(roles.isEmpty()) return Access.DENY;
		
		if(!Collections.disjoint(roles, this.grantRoles)) {
			
			return Access.GRANT;
		}

		if(!this.scheme.isInherit()) return Access.UNKNOWN;
	
		return getParent().accessible(roles);
	}
	
	@Override
	public Action prepare(Collection<Role> roles) {
		
		Access accessible = accessible(roles);
		
		if(accessible == Access.UNKNOWN) {
			
			accessible = determineDefaultAccess();
		}
		
		if(accessible == Access.GRANT) {
			
			return new ActionGrant(this.scheme, propagateRoles(roles));
		}
		else if(accessible == Access.DENY) {
		
			DenyInterceptor interceptor = determineInterceptor(roles);
			
			if(interceptor != null) {
				
				return interceptor.prepare(roles);
			}
			else {
				
				return new ActionDeny(this.scheme);
			}
		}
		else {
		
			throw new PropertyPrepareException("Can't prepare property: \""+this.name+"\". Your Kero-Security configuration is bad, if you see this exception.");
		}
	}
	
	protected Access determineDefaultAccess() {
		
		Access defaultAccess = getDefaultAccess();
	
		if(defaultAccess == Access.UNKNOWN) {
			
			defaultAccess = this.scheme.determineDefaultAccess();
		}
		
		return defaultAccess;
	}
	
	@Override
	public DenyInterceptor determineInterceptor(Collection<Role> roles) {
		
		int maxOverlap = 0;
		int minTrash = Integer.MAX_VALUE;
		DenyInterceptor result = null;
		
		List<DenyInterceptor> interceptors = getInterceptors();
		
		for(DenyInterceptor interceptor : interceptors) {
			
			Set<Role> interceptorRoles = interceptor.getRoles();
			
			int overlap = 0;
			int trash = 0;
			
			for(Role interceptorRole : interceptorRoles) {
				
				if(roles.contains(interceptorRole)) {
					
					overlap++;
				}
				else {
					
					trash++;
				}
			}
			
			if(overlap > maxOverlap) {
				
				maxOverlap = overlap;
				minTrash = trash;
				result = interceptor;
			}
			else if(overlap == maxOverlap && trash < minTrash) {
				
				maxOverlap = overlap;
				minTrash = trash;
				result = interceptor;
			}
		}
	
		if(maxOverlap == 0 || result == null) return getDefaultInterceptor();
		
		return result;
	}
	
	@Override
	public Role propagateRole(Role role) {
		
		Set<Role> data = new HashSet<>();
			data.add(role);
			
		return propagateRoles(data).iterator().next();
	}
	
	@Override
	public Set<Role> propagateRoles(Collection<Role> rolesArg) {
		
		Set<Role> roles = new HashSet<>(rolesArg);
		
		Set<Role> result = new HashSet<>();
		Set<Role> propagated = new HashSet<>();

		for(Role fromRole : roles) {
			
			if(hasPropagationFor(fromRole)) {
				
				result.add(this.rolesPropagation.get(fromRole));
				propagated.add(fromRole);
			}
		}
		
		roles.removeAll(propagated);
		
		if(this.scheme.isInherit()) {
		
			result.addAll(this.getParent().propagateRoles(roles));
		}
		
		return result;
	}
	
	@Override
	public boolean hasPropagationFor(Role target) {
		
		return rolesPropagation.containsKey(target);
	}
	
	@Override
	public void addRolePropagation(Role from, Role to) {
		
		this.rolesPropagation.put(from, to);
	}
	
	@Override
	public Map<Role, Role> getLocalRolesPropagation() {
		
		return this.rolesPropagation;
	}
	
	@Override
	public void addInterceptor(DenyInterceptor interceptor) {
		
		this.interceptors.add(interceptor);
	}
	
	@Override
	public List<DenyInterceptor> getInterceptors() {
	
		List<DenyInterceptor> interceptors = new ArrayList<>(this.interceptors);
		
		if(this.scheme.isInherit()) {
			
			interceptors.addAll(this.getParent().getInterceptors());
		}
		
		return interceptors;
	}
	
	public void grantRoles(Collection<Role> roles) {
		
		for(Role role : roles) {
			
			grantRole(role);
		}
	}
	
	public void grantRole(Role role) {
		
		if(this.denyRoles.contains(role)) throw new RoleCollisionException("Detected roles collision: "+role);
		
		this.grantRoles.add(role);
	}
	
	public void denyRoles(Collection<Role> roles) {
		
		for(Role role : roles) {
			
			denyRole(role);
		}
	}

	public void denyRole(Role role) {
		
		if(this.grantRoles.contains(role)) throw new RoleCollisionException("Detected roles collision: "+role);
		
		this.denyRoles.add(role);
	}

	@Override
	public Set<Role> getLocalGrantRoles() {
		
		return this.grantRoles;
	}
	
	@Override
	public Set<Role> getLocalDenyRoles() {
		
		return this.denyRoles;
	}

	@Override
	public void setDefaultAccess(Access access) {
		
		this.defaultAccess = access;
	}
	
	@Override
	public boolean hasLocalDefaultAccess() {
		
		return this.getLocalDefaultAccess() != Access.UNKNOWN;
	}
	
	@Override
	public Access getLocalDefaultAccess() {
		
		return this.defaultAccess;
	}
	
	@Override
	public boolean hasDefaultAccess() {
		
		return this.getDefaultAccess() != Access.UNKNOWN;
	}
	
	@Override
	public Access getDefaultAccess() {
		
		if(hasLocalDefaultAccess()) return this.getLocalDefaultAccess();

		if(!this.scheme.isInherit()) return Access.UNKNOWN;
		
		return getParent().getDefaultAccess();
	}

	@Override
	public String getName() {
		
		return this.name;
	}

	@Override
	public void setDefaultInterceptor(DenyInterceptor interceptor) {
		
		this.defaultInterceptor = interceptor;
	}

	@Override
	public boolean hasDefaultInterceptor() {
		
		return this.defaultInterceptor != null;
	}

	@Override
	public DenyInterceptor getDefaultInterceptor() {
		
		if(hasDefaultInterceptor()) return this.defaultInterceptor;
		
		if(!this.scheme.isInherit()) return null;
		
		return this.getParent().getDefaultInterceptor();
	}
	
	@Override
	public Property getParent() {
		
		return this.scheme.getParentProperty(this.name);
	}

	@Override
	public List<DenyInterceptor> getLocalInterceptors() {
		
		return this.interceptors;
	}

	@Override
	public boolean hasLocalDefaultInterceptor() {
		
		return this.defaultInterceptor != null;
	}

	@Override
	public DenyInterceptor getLocalDefaultInterceptor() {
		
		return this.defaultInterceptor;
	}
}
