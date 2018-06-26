package i5.las2peer.p2p;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.p2p.ServiceVersion;
import i5.las2peer.api.persistency.EnvelopeException;
import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.security.InternalSecurityException;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.tools.CryptoException;

public class ServiceAliasManagerTest {

	@Test
	public void testDuplication() throws CryptoException, InternalSecurityException, AliasConflictException,
			AliasNotFoundException, AgentAccessDeniedException, AgentOperationFailedException {
		LocalNode node = new LocalNodeManager().launchNode();
		ServiceAgentImpl agentA = ServiceAgentImpl.createServiceAgent(ServiceNameVersion.fromString("serviceA@1.0"),
				"asdf");
		agentA.unlock("asdf");
		ServiceAgentImpl agentB = ServiceAgentImpl.createServiceAgent(ServiceNameVersion.fromString("serviceB@1.0"),
				"asdf");
		agentB.unlock("asdf");
		ServiceAgentImpl agentA2 = ServiceAgentImpl.createServiceAgent(ServiceNameVersion.fromString("serviceA@1.0"),
				"asdf");
		agentA2.unlock("asdf");
		ServiceAgentImpl agentC = ServiceAgentImpl.createServiceAgent(ServiceNameVersion.fromString("serviceC@1.0"),
				"asdf");
		agentC.unlock("asdf");

		// regular creation
		node.getServiceAliasManager().registerServiceAlias(agentA, "aliasA");
		assertTrue(node.getServiceAliasManager().resolvePathToServiceNames("aliasA").stream()
				.anyMatch(name -> name.getServiceNameVersion().equals("serviceA@1.0")));

		// second alias
		node.getServiceAliasManager().registerServiceAlias(agentB, "aliasB");
		assertTrue(node.getServiceAliasManager().resolvePathToServiceNames("aliasB").stream()
				.anyMatch(name -> name.getServiceNameVersion().equals("serviceB@1.0")));

		// register second time, no conflict
		node.getServiceAliasManager().registerServiceAlias(agentA2, "aliasA");

		// duplicate
		node.getServiceAliasManager().registerServiceAlias(agentC, "aliasA");
		assertTrue(node.getServiceAliasManager().resolvePathToServiceNames("aliasA").stream()
				.anyMatch(name -> name.getServiceNameVersion().equals("serviceC@1.0")));
	}

	@Test
	public void testRegistering() throws CryptoException, InternalSecurityException, AliasConflictException,
			AliasNotFoundException, AgentAccessDeniedException, AgentOperationFailedException {
		LocalNode node = new LocalNodeManager().launchNode();
		ServiceAgentImpl agentA = ServiceAgentImpl.createServiceAgent(ServiceNameVersion.fromString("serviceA@1.0"),
				"asdf");
		agentA.unlock("asdf");
		ServiceAgentImpl agentB = ServiceAgentImpl.createServiceAgent(ServiceNameVersion.fromString("serviceB@1.0"),
				"asdf");
		agentB.unlock("asdf");
		ServiceAgentImpl agentC = ServiceAgentImpl.createServiceAgent(ServiceNameVersion.fromString("serviceC@1.0"),
				"asdf");
		agentC.unlock("asdf");
		ServiceAgentImpl agentD = ServiceAgentImpl.createServiceAgent(ServiceNameVersion.fromString("serviceD@1.0"),
				"asdf");
		agentD.unlock("asdf");

		// regular creation
		node.getServiceAliasManager().registerServiceAlias(agentA, "prefix/prefix/aliasA");
		assertTrue(node.getServiceAliasManager().resolvePathToServiceNames("prefix/prefix/aliasA").stream()
				.anyMatch(name -> name.getServiceNameVersion().equals("serviceA@1.0")));


		// second alias
		node.getServiceAliasManager().registerServiceAlias(agentB, "prefix/prefix/aliasB");
		assertTrue(node.getServiceAliasManager().resolvePathToServiceNames("prefix/prefix/aliasB").stream()
				.anyMatch(name -> name.getServiceNameVersion().equals("serviceB@1.0")));

		// existing alias is prefix of new one
		try {
			node.getServiceAliasManager().registerServiceAlias(agentC, "prefix/prefix/aliasA/aliasC");
			fail("AliasConflictException expected");
		} catch (AliasConflictException e) {
		}

		// alias is prefix of existing ones
//		try {
//			node.getServiceAliasManager().registerServiceAlias(agentD, "prefix/prefix");
//			fail("AliasConflictException expected");
//		} catch (AliasConflictException e) {
//		}
	}

	@SuppressWarnings("deprecation")
	@Test
	public void teckBackwardsCompatibility() throws AgentOperationFailedException, CryptoException, AgentAccessDeniedException, 
			IllegalArgumentException, EnvelopeException, SerializationException, AliasNotFoundException {
		LocalNode node = new LocalNodeManager().launchNode();
		ServiceAgentImpl agentA = ServiceAgentImpl.createServiceAgent(ServiceNameVersion.fromString("serviceA@1.0"),
				"asdf");
		
		node.getServiceAliasManager().createEntryForTest(agentA, "aliasA", "serviceA");
		
		assertTrue(node.getServiceAliasManager().resolvePathToServiceNames("aliasA").stream()
				.anyMatch(name -> ServiceNameVersion.fromString(name.getServiceNameVersion())
						.getVersion().equals(new ServiceVersion("*"))));
	}
	
	@Test
	public void testResolve() throws CryptoException, InternalSecurityException, AliasConflictException,
			AliasNotFoundException, AgentAccessDeniedException, AgentOperationFailedException {
		LocalNode node = new LocalNodeManager().launchNode();
		ServiceAgentImpl agentA = ServiceAgentImpl.createServiceAgent(ServiceNameVersion.fromString("serviceA@1.0"),
				"asdf");
		agentA.unlock("asdf");
		ServiceAgentImpl agentB = ServiceAgentImpl.createServiceAgent(ServiceNameVersion.fromString("serviceB@1.0"),
				"asdf");
		agentB.unlock("asdf");
		ServiceAgentImpl agentC = ServiceAgentImpl.createServiceAgent(ServiceNameVersion.fromString("serviceC@1.0"),
				"asdf");
		agentC.unlock("asdf");
		ServiceAgentImpl agentD = ServiceAgentImpl.createServiceAgent(ServiceNameVersion.fromString("serviceD@1.0"),
				"asdf");
		agentD.unlock("asdf");

		// register
		node.getServiceAliasManager().registerServiceAlias(agentA, "prefix/prefix/aliasA");
		node.getServiceAliasManager().registerServiceAlias(agentB, "prefix/prefix/aliasB");
		node.getServiceAliasManager().registerServiceAlias(agentC, "prefix/aliasC");
		node.getServiceAliasManager().registerServiceAlias(agentD, "prefix/aliasD");

		// resolve	
		assertTrue(node.getServiceAliasManager().resolvePathToServiceNames("prefix/prefix/aliasA").stream()
				.anyMatch(name -> name.getServiceNameVersion().equals("serviceA@1.0")));

		assertEquals(3,
				node.getServiceAliasManager().resolvePathToServiceNames("prefix/prefix/aliasA").get(0).getNumMatchedParts());
		assertEquals("serviceB@1.0",
				node.getServiceAliasManager().resolvePathToServiceNames("prefix/prefix/aliasB").get(0).getServiceNameVersion());
		assertEquals(3,
				node.getServiceAliasManager().resolvePathToServiceNames("prefix/prefix/aliasB").get(0).getNumMatchedParts());
		assertEquals("serviceC@1.0",
				node.getServiceAliasManager().resolvePathToServiceNames("prefix/aliasC").get(0).getServiceNameVersion());
		assertEquals(2, node.getServiceAliasManager().resolvePathToServiceNames("prefix/aliasC").get(0).getNumMatchedParts());
		assertEquals("serviceD@1.0",
				node.getServiceAliasManager().resolvePathToServiceNames("prefix/aliasD").get(0).getServiceNameVersion());
		assertEquals(2, node.getServiceAliasManager().resolvePathToServiceNames("prefix/aliasD").get(0).getNumMatchedParts());
		assertEquals("serviceA@1.0",
				node.getServiceAliasManager().resolvePathToServiceNames("prefix/prefix/aliasA/asdf").get(0).getServiceNameVersion());
		assertEquals(3, node.getServiceAliasManager().resolvePathToServiceNames("prefix/prefix/aliasA/asdf").get(0)
				.getNumMatchedParts());
		assertEquals("serviceC@1.0",
				node.getServiceAliasManager().resolvePathToServiceNames("prefix/aliasC/asdf/rtzh").get(0).getServiceNameVersion());
		assertEquals(2,
				node.getServiceAliasManager().resolvePathToServiceNames("prefix/aliasC/asdf/rtzh").get(0).getNumMatchedParts());
				
		// resolve with empty path parts
		assertTrue(node.getServiceAliasManager().resolvePathToServiceNames("//prefix//aliasC//asdf//rtzh//").stream()
				.anyMatch(name -> name.getServiceNameVersion().equals("serviceC@1.0")));
		assertEquals(2, node.getServiceAliasManager().resolvePathToServiceNames("//prefix//aliasC//asdf//rtzh//").get(0)
				.getNumMatchedParts());
	}
	
	@Test
	public void testMultipleVersions() throws AgentOperationFailedException, CryptoException, AgentAccessDeniedException, 
			AliasConflictException, AliasNotFoundException {
		LocalNode node = new LocalNodeManager().launchNode();
		ServiceAgentImpl agentA = ServiceAgentImpl.createServiceAgent(ServiceNameVersion.fromString("serviceA@1.0"),
				"asdf");
		agentA.unlock("asdf");
		ServiceAgentImpl agentB = ServiceAgentImpl.createServiceAgent(ServiceNameVersion.fromString("serviceA@2.0"),
				"asdf");
		agentB.unlock("asdf");
		
		node.getServiceAliasManager().registerServiceAlias(agentA, "aliasA");
		node.getServiceAliasManager().registerServiceAlias(agentB, "aliasA");
		
		// two matches
		assertEquals(2, node.getServiceAliasManager().resolvePathToServiceNames("aliasA").size());
		
		// correct service with version specified
		assertTrue(node.getServiceAliasManager().resolvePathToServiceNames("aliasA").stream()
				.anyMatch(name -> name.getServiceNameVersion().equals("serviceA@1.0")));
		assertTrue(node.getServiceAliasManager().resolvePathToServiceNames("aliasA").stream()
				.anyMatch(name -> name.getServiceNameVersion().equals("serviceA@2.0")));
	}

	@Test
	public void testIntegration() throws CryptoException, InternalSecurityException, AgentAlreadyRegisteredException,
			AgentException, AliasNotFoundException, AgentAccessDeniedException {
		LocalNode node = new LocalNodeManager().launchNode();
		node.startService(ServiceNameVersion.fromString("i5.las2peer.api.TestService@1.0"), "asdf");

		// regular creation
		assertTrue(node.getServiceAliasManager().resolvePathToServiceNames("test").stream()
				.anyMatch(name -> name.getServiceNameVersion().equals("i5.las2peer.api.TestService@1.0")));
	}
}
