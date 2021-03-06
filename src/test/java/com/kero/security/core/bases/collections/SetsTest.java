package com.kero.security.core.bases.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.kero.security.core.TestObject;
import com.kero.security.core.agent.KeroAccessAgent;
import com.kero.security.core.agent.KeroAccessAgentFactoryImpl;
import com.kero.security.core.exception.AccessException;

public class SetsTest {

	private KeroAccessAgent manager = new KeroAccessAgentFactoryImpl().create();
	
	@BeforeEach
	public void init() {
		
		manager.getKeroAccessConfigurator()
			.scheme(TestObject.class)
				.defaultGrant()
				.property("text")
					.defaultDeny()
					.addDenyInterceptor((obj, args)-> "forbidden", "FRIEND")
					.grantFor("OWNER");
	}
	
	@Test
	public void test_hashSet() {
		
		HashSet<TestObject> set = new HashSet<>();
			set.add(new TestObject("test text"));
	
		test_Owner(set);
		test_None(set);
		test_Friend(set);
	}
	
	@Test
	public void test_linkedHashSet() {
		
		LinkedHashSet<TestObject> set = new LinkedHashSet<>();
			set.add(new TestObject("test text"));
	
		test_Owner(set);
		test_None(set);
		test_Friend(set);
	}
	
	@Test
	public void test_TreeSet() {
		
		TreeSet<TestObject> set = new TreeSet<>();
			set.add(new TestObject("test text"));
	
		test_Owner(set);
		test_None(set);
		test_Friend(set);
	}
	
	public void test_Owner(Set<TestObject> set) {
		
		Set<TestObject> setOwner = manager.protect(set, "OWNER");
		assertEquals(setOwner.iterator().next().getText(), "test text");
	}
	
	public void test_None(Set<TestObject> set) {
		
		Set<TestObject> setNone = manager.protect(set, "NONE");
		assertThrows(AccessException.class, ()-> setNone.iterator().next().getText());
	}
	
	public void test_Friend(Set<TestObject> set) {
			
		Set<TestObject> setFriend = manager.protect(set, "FRIEND");
		assertEquals(setFriend.iterator().next().getText(), "forbidden");
	}
}
