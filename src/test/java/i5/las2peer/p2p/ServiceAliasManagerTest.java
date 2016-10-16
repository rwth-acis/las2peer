package i5.las2peer.p2p;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import i5.las2peer.security.AgentException;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.tools.CryptoException;

import org.junit.Before;
import org.junit.Test;

public class ServiceAliasManagerTest {
	@Before
	public void reset() {
		LocalNode.reset();
	}

	@Test
	public void testDuplication() throws CryptoException, L2pSecurityException, AliasConflictException,
			AliasNotFoundException {
		LocalNode node = LocalNode.launchNode();
		ServiceAgent agentA = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString("serviceA@1.0"), "asdf");
		agentA.unlockPrivateKey("asdf");
		ServiceAgent agentB = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString("serviceB@1.0"), "asdf");
		agentB.unlockPrivateKey("asdf");
		ServiceAgent agentA2 = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString("serviceA@1.0"), "asdf");
		agentA2.unlockPrivateKey("asdf");
		ServiceAgent agentC = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString("serviceC@1.0"), "asdf");
		agentC.unlockPrivateKey("asdf");

		// regular creation
		node.getServiceAliasManager().registerServiceAlias(agentA, "aliasA");
		assertEquals(node.getServiceAliasManager().resolvePathToServiceName("aliasA").getServiceName(), "serviceA");

		// second alias
		node.getServiceAliasManager().registerServiceAlias(agentB, "aliasB");
		assertEquals(node.getServiceAliasManager().resolvePathToServiceName("aliasB").getServiceName(), "serviceB");

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
	public void testRegistering() throws CryptoException, L2pSecurityException, AliasConflictException,
			AliasNotFoundException {
		LocalNode node = LocalNode.launchNode();
		ServiceAgent agentA = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString("serviceA@1.0"), "asdf");
		agentA.unlockPrivateKey("asdf");
		ServiceAgent agentB = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString("serviceB@1.0"), "asdf");
		agentB.unlockPrivateKey("asdf");
		ServiceAgent agentC = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString("serviceC@1.0"), "asdf");
		agentC.unlockPrivateKey("asdf");
		ServiceAgent agentD = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString("serviceD@1.0"), "asdf");
		agentD.unlockPrivateKey("asdf");

		// regular creation
		node.getServiceAliasManager().registerServiceAlias(agentA, "prefix/prefix/aliasA");
		assertEquals(node.getServiceAliasManager().resolvePathToServiceName("prefix/prefix/aliasA").getServiceName(),
				"serviceA");

		// second alias
		node.getServiceAliasManager().registerServiceAlias(agentB, "prefix/prefix/aliasB");
		assertEquals(node.getServiceAliasManager().resolvePathToServiceName("prefix/prefix/aliasB").getServiceName(),
				"serviceB");

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
	public void testResolve() throws CryptoException, L2pSecurityException, AliasConflictException,
			AliasNotFoundException {
		LocalNode node = LocalNode.launchNode();
		ServiceAgent agentA = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString("serviceA@1.0"), "asdf");
		agentA.unlockPrivateKey("asdf");
		ServiceAgent agentB = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString("serviceB@1.0"), "asdf");
		agentB.unlockPrivateKey("asdf");
		ServiceAgent agentC = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString("serviceC@1.0"), "asdf");
		agentC.unlockPrivateKey("asdf");
		ServiceAgent agentD = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString("serviceD@1.0"), "asdf");
		agentD.unlockPrivateKey("asdf");

		// register
		node.getServiceAliasManager().registerServiceAlias(agentA, "prefix/prefix/aliasA");
		node.getServiceAliasManager().registerServiceAlias(agentB, "prefix/prefix/aliasB");
		node.getServiceAliasManager().registerServiceAlias(agentC, "prefix/aliasC");
		node.getServiceAliasManager().registerServiceAlias(agentD, "prefix/aliasD");

		// resolve
		assertEquals(node.getServiceAliasManager().resolvePathToServiceName("prefix/prefix/aliasA").getServiceName(),
				"serviceA");
		assertEquals(node.getServiceAliasManager().resolvePathToServiceName("prefix/prefix/aliasA")
				.getNumMatchedParts(), 3);
		assertEquals(node.getServiceAliasManager().resolvePathToServiceName("prefix/prefix/aliasB").getServiceName(),
				"serviceB");
		assertEquals(node.getServiceAliasManager().resolvePathToServiceName("prefix/prefix/aliasB")
				.getNumMatchedParts(), 3);
		assertEquals(node.getServiceAliasManager().resolvePathToServiceName("prefix/aliasC").getServiceName(),
				"serviceC");
		assertEquals(node.getServiceAliasManager().resolvePathToServiceName("prefix/aliasC").getNumMatchedParts(), 2);
		assertEquals(node.getServiceAliasManager().resolvePathToServiceName("prefix/aliasD").getServiceName(),
				"serviceD");
		assertEquals(node.getServiceAliasManager().resolvePathToServiceName("prefix/aliasD").getNumMatchedParts(), 2);
		assertEquals(node.getServiceAliasManager().resolvePathToServiceName("prefix/prefix/aliasA/asdf")
				.getServiceName(), "serviceA");
		assertEquals(node.getServiceAliasManager().resolvePathToServiceName("prefix/prefix/aliasA/asdf")
				.getNumMatchedParts(), 3);
		assertEquals(
				node.getServiceAliasManager().resolvePathToServiceName("prefix/aliasC/asdf/rtzh").getServiceName(),
				"serviceC");
		assertEquals(node.getServiceAliasManager().resolvePathToServiceName("prefix/aliasC/asdf/rtzh")
				.getNumMatchedParts(), 2);
	}

	@Test
	public void testIntegration() throws CryptoException, L2pSecurityException, AgentAlreadyRegisteredException,
			AgentException, AliasNotFoundException {
		LocalNode node = LocalNode.launchNode();
		ServiceAgent agentA = ServiceAgent.createServiceAgent(
				ServiceNameVersion.fromString("i5.las2peer.api.TestService@1.0"), "asdf");
		agentA.unlockPrivateKey("asdf");
		node.registerReceiver(agentA);

		// regular creation
		assertEquals(node.getServiceAliasManager().resolvePathToServiceName("test").getServiceName(),
				"i5.las2peer.api.TestService");
	}
}
