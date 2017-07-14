package i5.las2peer.connectors.nodeAdminConnector;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.connectors.nodeAdminConnector.services.TestService;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.tools.SimpleTools;

public class RMIHandlerTest extends AbstractTestHandler {

	public RMIHandlerTest() {
		this.networkSize = 3;
	}

	@Test
	public void testGetLocalAnonymous() {
		try {
			// start service on test node
			PastryNodeImpl serviceNode = nodes.get(0);
			ServiceAgentImpl serviceAgent = ServiceAgentImpl.createServiceAgent(
					new ServiceNameVersion(TestService.class.getCanonicalName(), "1.0"), "testpass");
			serviceAgent.unlock("testpass");
			serviceNode.registerReceiver(serviceAgent);
			// start actual test
			WebTarget target = sslClient
					.target(connector.getHostname() + "/rmi/" + TestService.class.getCanonicalName() + "/1.0/ok");
			Response response = target.request().get();
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.TEXT_HTML_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			Assert.assertEquals("OK", new String(bytes, StandardCharsets.UTF_8));
			response.close();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testGetLocalAgent() {
		try {
			// start service on test node
			PastryNodeImpl serviceNode = nodes.get(0);
			ServiceAgentImpl serviceAgent = ServiceAgentImpl.createServiceAgent(
					new ServiceNameVersion(TestService.class.getCanonicalName(), "1.0"), "testpass");
			serviceAgent.unlock("testpass");
			serviceNode.registerReceiver(serviceAgent);
			UserAgentImpl adam = MockAgentFactory.getAdam();
			adam.unlock("adamspass");
			serviceNode.storeAgent(adam);
			// start actual test
			WebTarget target = sslClient
					.target(connector.getHostname() + "/rmi/" + TestService.class.getCanonicalName() + "/1.0/ok");
			Response response = target.request()
					.header(HttpHeaders.AUTHORIZATION, "basic " + Base64.getEncoder()
							.encodeToString((adam.getLoginName() + ":" + "adamspass").getBytes(StandardCharsets.UTF_8)))
					.get();
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.TEXT_HTML_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			Assert.assertEquals("OK", new String(bytes, StandardCharsets.UTF_8));
			response.close();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Ignore // TODO This test currently fails because of incomplete Anonymous Agent implementation
	@Test
	public void testGetGlobalAnonymous() {
		try {
			// start service on test node
			PastryNodeImpl serviceNode = nodes.get(1);
			ServiceAgentImpl serviceAgent = ServiceAgentImpl.createServiceAgent(
					new ServiceNameVersion(TestService.class.getCanonicalName(), "1.0"), "testpass");
			serviceAgent.unlock("testpass");
			serviceNode.registerReceiver(serviceAgent);
			// start actual test
			WebTarget target = sslClient
					.target(connector.getHostname() + "/rmi/" + TestService.class.getCanonicalName() + "/1.0/ok");
			Response response = target.request().get();
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.TEXT_HTML_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			Assert.assertEquals("OK", new String(bytes, StandardCharsets.UTF_8));
			response.close();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testGetGlobalAgent() {
		try {
			// start service on test node
			PastryNodeImpl serviceNode = nodes.get(1);
			ServiceAgentImpl serviceAgent = ServiceAgentImpl.createServiceAgent(
					new ServiceNameVersion(TestService.class.getCanonicalName(), "1.0"), "testpass");
			serviceAgent.unlock("testpass");
			serviceNode.storeAgent(serviceAgent);
			serviceNode.registerReceiver(serviceAgent);
			PastryNodeImpl userNode = nodes.get(0);
			UserAgentImpl adam = MockAgentFactory.getAdam();
			adam.unlock("adamspass");
			userNode.storeAgent(adam);
			// start actual test
			WebTarget target = sslClient
					.target(connector.getHostname() + "/rmi/" + TestService.class.getCanonicalName() + "/1.0/ok");
			Response response = target.request()
					.header(HttpHeaders.AUTHORIZATION, "basic " + Base64.getEncoder()
							.encodeToString((adam.getLoginName() + ":" + "adamspass").getBytes(StandardCharsets.UTF_8)))
					.get();
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.TEXT_HTML_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			Assert.assertEquals("OK", new String(bytes, StandardCharsets.UTF_8));
			response.close();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
