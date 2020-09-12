package com.kero.security.lang.nodes;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kero.security.core.agent.KeroAccessAgent;
import com.kero.security.core.scheme.AccessScheme;

public class SchemeNode extends KsdlNodeBase implements KsdlRootNode {

	private static Logger LOGGER = LoggerFactory.getLogger("KeroSecurity");
	
	public static final SchemeNode EMPTY = new Empty();
	
	private String typeAliase;
	
	private DefaultRuleNode defaultRule;
	
	private List<PropertyNode> properties;
	
	public SchemeNode(String typeAliase, DefaultRuleNode defaultRule, List<PropertyNode> properties) {
		
		this.typeAliase = typeAliase;
		this.defaultRule = defaultRule;
		this.properties = properties;
	}
	
	public void interpret(KeroAccessAgent manager) {
		
		AccessScheme scheme = manager.getSchemeByAlise(this.typeAliase);
	
		this.interpret(scheme);
	}
	
	public void interpret(AccessScheme scheme) {
		
		LOGGER.debug("Interpret scheme node to: "+scheme.getClass().getCanonicalName());
		
		defaultRule.interpret(scheme.getAgent(), scheme);
		
		properties.forEach((prop)-> prop.interpret(scheme));
	}
	
	public String getTypeAlise() {
		
		return this.typeAliase;
	}
	
	private static class Empty extends SchemeNode {

		public Empty() {
			super(null, null, null);
		
		}
		
		public void interpret(KeroAccessAgent manager) {}
		public void interpret(AccessScheme scheme) {}
	}
}