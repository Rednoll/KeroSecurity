package com.kero.security.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kero.security.core.role.Role;
import com.kero.security.core.role.RoleImpl;
import com.kero.security.core.rules.AccessRule;
import com.kero.security.core.rules.AccessRuleImpl;
import com.kero.security.core.scheme.AccessScheme;
import com.kero.security.core.scheme.ClassAccessScheme;
import com.kero.security.core.scheme.InterfaceAccessScheme;
import com.kero.security.core.scheme.configuration.KeroAccessConfigurator;
import com.kero.security.core.scheme.configuration.auto.AccessSchemeAutoConfigurator;
import com.kero.security.core.scheme.configuration.auto.AnnotationAccessSchemeConfigurator;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

public class KeroAccessManagerImpl implements KeroAccessManager {
	
	protected static Logger LOGGER = LoggerFactory.getLogger("KeroSecurity");
	
	protected Map<Class, AccessScheme> schemes = new HashMap<>();
	
	protected Map<String, Role> roles = new HashMap<>();
	
	protected AccessRule defaultRule = AccessRuleImpl.GRANT_ALL;
	
	protected ClassLoader proxiesClassLoader = ClassLoader.getSystemClassLoader();
	
	protected Set<Class> ignoreList = new HashSet<>();
	
	protected String basePackage = "com.kero";
	protected boolean scaned = false;
	
	protected Map<String, Class<?>> aliasedTypes = new HashMap<>();
	
	protected KeroAccessConfigurator configurator = new KeroAccessConfigurator(this);
	
	protected Set<AccessSchemeAutoConfigurator> autoConfigurators = new HashSet<>();
	
	public KeroAccessManagerImpl() {
		
		ignoreType(String.class);
		
		ignoreType(Integer.class);
		ignoreType(int.class);
		
		ignoreType(Long.class);
		ignoreType(long.class);
		
		ignoreType(Float.class);
		ignoreType(float.class);
		
		ignoreType(Double.class);
		ignoreType(double.class);
		
		ignoreType(Character.class);
		ignoreType(char.class);
		
		ignoreType(Boolean.class);
		ignoreType(boolean.class);
		
		autoConfigurators.add(new AnnotationAccessSchemeConfigurator(this));
	}
	
	public void addTypeAliase(String aliase, Class<?> type) {
		
		this.aliasedTypes.put(aliase, type);
	}
	
	public void setBasePackage(String basePackage) {
		
		this.basePackage = basePackage;
	}
	
	public void ignoreType(Class<?> type) {
		
		ignoreList.add(type);
	}
	
	@Override
	public Role createRole(String name) {
		
		Role role = new RoleImpl(name);
		
		roles.put(name, role);
		
		return role;
	}
	
	public Role getRole(String name) {
		
		return roles.get(name);
	}
	
	public Role getOrCreateRole(String name) {
		
		if(hasRole(name)) {
			
			return getRole(name);
		}
		else {

			return createRole(name);
		}
	}
	
	public boolean hasRole(String name) {
		
		return this.roles.containsKey(name);
	}
	
	@Override
	public boolean hasScheme(Class<?> rawType) {
		
		return schemes.containsKey(rawType);
	}

	@Override
	public AccessScheme getScheme(Class<?> rawType) {
		
		return schemes.get(rawType);
	}
	
	@Override
	public Class<?> getTypeByAliase(String aliase) {
		
		if(aliasedTypes.containsKey(aliase)) return aliasedTypes.get(aliase);
		
		if(!scaned) {
			
			LOGGER.debug("Begin scan base package: "+basePackage);
			
			ScanResult scanResult = new ClassGraph().verbose().enableAllInfo().acceptPackages(basePackage).scan();
			
			ClassInfoList classInfoList = scanResult.getAllClasses();
			
			for(ClassInfo typeInfo : classInfoList) {
				
				String typeAliase = typeInfo.getSimpleName();
				
				Class<?> type = typeInfo.loadClass();
				
				LOGGER.debug("Registered type: "+typeAliase);
				
				aliasedTypes.put(typeAliase, type);
			}
			
			scaned = true;
		}
		
		return aliasedTypes.get(aliase);
	}
	
	public AccessScheme getOrCreateScheme(Class<?> rawType){
		
		return hasScheme(rawType) ? getScheme(rawType) : createScheme(rawType);
	}
	
	public AccessScheme createScheme(Class<?> rawType) {
		
		AccessScheme scheme = null;
		
		if(rawType.isInterface()) {
			
			LOGGER.debug("Creating access scheme for interface: "+rawType.getCanonicalName());
			scheme = new InterfaceAccessScheme(this, rawType);
		}
		else {
			
			LOGGER.debug("Creating access scheme for class: "+rawType.getCanonicalName());
			scheme = new ClassAccessScheme(this, rawType);
		}
		
		for(AccessSchemeAutoConfigurator ac : autoConfigurators) {
			
			ac.configure(scheme);
		}
		
		schemes.put(rawType, scheme);
		
		return scheme;
	}

	@Override
	public <T> T protect(T object, Set<Role> roles) {
		
		if(object == null) return null;
		
		if(this.ignoreList.contains(object.getClass())) return object;

		try {
			
			ClassAccessScheme scheme = (ClassAccessScheme) getOrCreateScheme(object.getClass());
				
			return scheme.protect(object, roles);
		}
		catch(Exception e) {
			
			throw new RuntimeException(e);
		}
	}
	
	public String extractName(String rawName) {
		
		if(rawName.startsWith("get")) {
			
			rawName = rawName.replaceFirst("get", "");
		}
		
		rawName = rawName.toLowerCase();
	
		return rawName;
	}
	
	public AccessRule getDefaultRule() {
		
		return this.defaultRule;
	}

	@Override
	public ClassLoader getClassLoader() {
		
		return this.proxiesClassLoader;
	}
	
	public KeroAccessConfigurator getConfigurator() {
		
		return this.configurator;
	}
}