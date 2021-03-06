package com.kero.security.core.agent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.kero.security.core.access.Access;
import com.kero.security.core.configurator.KeroAccessConfigurator;
import com.kero.security.core.role.Role;
import com.kero.security.core.role.storage.RoleStorage;
import com.kero.security.core.scheme.AccessScheme;
import com.kero.security.core.scheme.configurator.AccessSchemeConfigurator;
import com.kero.security.core.scheme.definition.configurator.AccessSchemeDefinitionConfigurator;
import com.kero.security.core.scheme.storage.AccessSchemeStorage;
import com.kero.security.core.scheme.strategy.AccessSchemeNamingStrategy;

public interface KeroAccessAgent {
	
	public void ignoreType(Class<?> type);

	public void setSchemeNamingStrategy(AccessSchemeNamingStrategy strategy);
	
	public void addConfigurator(AccessSchemeConfigurator configurator);
	public void addDefinitionConfigurator(AccessSchemeDefinitionConfigurator definitionConfigurator);
	
	public void setTypeName(String name, Class<?> type);

	public Access getDefaultAccess();
	
	public String extractPropertyName(String rawName);

	public Role createRole(String name);
	public Role getRole(String name);
	public Role hasRole(String name);
	public Role getOrCreateRole(String name);
	public Set<Role> getOrCreateRole(Collection<String> names);
	public Set<Role> getOrCreateRole(String[] names);
	
	public AccessScheme getOrCreateScheme(Class<?> rawType);
	public boolean hasScheme(Class<?> rawType);
	public AccessScheme getSchemeByName(String name);
	public AccessScheme getScheme(Class<?> rawType);
	
	public default <T> T protect(T object) {
		
		return (T) protect(object, Collections.EMPTY_SET);
	}
	
	public default <T> T protect(T object, String... roleNames) {
		
		Set<Role> roles = this.getOrCreateRole(roleNames);
		
		return protect(object, roles);
	}
	
	public default <T> T protect(T object, Role... roles) {
		
		return protect(object, new HashSet<>(Arrays.asList(roles)));
	}
	
	public <T> T protect(T object, Collection<Role> roles);
	
	public AccessSchemeStorage getSchemeStorage();
	public RoleStorage getRoleStorage();
	public KeroAccessConfigurator getKeroAccessConfigurator();
}
