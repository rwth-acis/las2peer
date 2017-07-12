package i5.las2peer.classLoaders.policies;

import static org.junit.Assert.*;

import org.junit.Test;

public class ClassLoaderPolicyTest {
	
	class TestPolicy extends ClassLoaderPolicy {
		TestPolicy() {
			allow("package");
			allow("package2.sub");
			allow("package3");
			deny("package3.sub");
		}
	}
	
	class TestPolicy2 extends ClassLoaderPolicy {
		TestPolicy2() {
			allow("");
			deny("package");
		}
	}
	
	@Test
	public void test() {
		ClassLoaderPolicy policy = new TestPolicy();
		
		assertFalse(policy.canLoad("notallowed"));
		assertTrue(policy.canLoad("package"));
		assertTrue(policy.canLoad("package.sub"));
		assertFalse(policy.canLoad("package2"));
		assertTrue(policy.canLoad("package2.sub"));
		assertTrue(policy.canLoad("package3.sub1"));
		assertTrue(policy.canLoad("package3.sub1.sub"));
		assertFalse(policy.canLoad("package3.sub"));
		assertFalse(policy.canLoad("package3.sub.sub"));
		
		ClassLoaderPolicy policy2 = new TestPolicy2();
		
		assertTrue(policy2.canLoad("package2"));
		assertFalse(policy2.canLoad("package"));
	}
}
