package i5.las2peer.p2p;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.security.InternalSecurityException;
import i5.las2peer.security.ServiceAgentImpl;
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
		assertEquals("serviceA", node.getServiceAliasManager().resolvePathToServiceName("aliasA").getServiceName());

		// second alias
		node.getServiceAliasManager().registerServiceAlias(agentB, "aliasB");
		assertEquals("serviceB", node.getServiceAliasManager().resolvePathToServiceName("aliasB").getServiceName());

		// register second time, no conflict
		node.getServiceAliasManager().registerServiceAlias(agentA2, "aliasA");

		// duplicate
		try {
			node.getServiceAliasManager().registerServiceAlias(agentC, "aliasA");
			fail("AliasConflictException expected");
		} catch (AliasConflictException e) {
		}
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
		assertEquals("serviceA",
				node.getServiceAliasManager().resolvePathToServiceName("prefix/prefix/aliasA").getServiceName());

		// second alias
		node.getServiceAliasManager().registerServiceAlias(agentB, "prefix/prefix/aliasB");
		assertEquals("serviceB",
				node.getServiceAliasManager().resolvePathToServiceName("prefix/prefix/aliasB").getServiceName());

		// existing alias is prefix of new one
		try {
			node.getServiceAliasManager().registerServiceAlias(agentC, "prefix/prefix/aliasA/aliasC");
			fail("AliasConflictException expected");
		} catch (AliasConflictException e) {
		}

		// alias is prefix of existing ones
		try {
			node.getServiceAliasManager().registerServiceAlias(agentD, "prefix/prefix");
			fail("AliasConflictException expected");
		} catch (AliasConflictException e) {
		}
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
		assertEquals("serviceA",
				node.getServiceAliasManager().resolvePathToServiceName("prefix/prefix/aliasA").getServiceName());
		assertEquals(3,
				node.getServiceAliasManager().resolvePathToServiceName("prefix/prefix/aliasA").getNumMatchedParts());
		assertEquals("serviceB",
				node.getServiceAliasManager().resolvePathToServiceName("prefix/prefix/aliasB").getServiceName());
		assertEquals(3,
				node.getServiceAliasManager().resolvePathToServiceName("prefix/prefix/aliasB").getNumMatchedParts());
		assertEquals("serviceC",
				node.getServiceAliasManager().resolvePathToServiceName("prefix/aliasC").getServiceName());
		assertEquals(2, node.getServiceAliasManager().resolvePathToServiceName("prefix/aliasC").getNumMatchedParts());
		assertEquals("serviceD",
				node.getServiceAliasManager().resolvePathToServiceName("prefix/aliasD").getServiceName());
		assertEquals(2, node.getServiceAliasManager().resolvePathToServiceName("prefix/aliasD").getNumMatchedParts());
		assertEquals("serviceA",
				node.getServiceAliasManager().resolvePathToServiceName("prefix/prefix/aliasA/asdf").getServiceName());
		assertEquals(3, node.getServiceAliasManager().resolvePathToServiceName("prefix/prefix/aliasA/asdf")
				.getNumMatchedParts());
		assertEquals("serviceC",
				node.getServiceAliasManager().resolvePathToServiceName("prefix/aliasC/asdf/rtzh").getServiceName());
		assertEquals(2,
				node.getServiceAliasManager().resolvePathToServiceName("prefix/aliasC/asdf/rtzh").getNumMatchedParts());

		// resolve with empty path parts
		assertEquals("serviceC", node.getServiceAliasManager()
				.resolvePathToServiceName("//prefix//aliasC//asdf//rtzh//").getServiceName());
		assertEquals(2, node.getServiceAliasManager().resolvePathToServiceName("//prefix//aliasC//asdf//rtzh//")
				.getNumMatchedParts());
	}

	@Test
	public void testIntegration() throws CryptoException, InternalSecurityException, AgentAlreadyRegisteredException,
			AgentException, AliasNotFoundException, AgentAccessDeniedException {
		LocalNode node = new LocalNodeManager().launchNode();
		node.startService(ServiceNameVersion.fromString("i5.las2peer.api.TestService@1.0"), "asdf");

		// regular creation
		assertEquals("i5.las2peer.api.TestService",
				node.getServiceAliasManager().resolvePathToServiceName("test").getServiceName());
	}
}
