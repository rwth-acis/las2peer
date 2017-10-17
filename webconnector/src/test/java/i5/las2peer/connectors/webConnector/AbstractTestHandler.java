package i5.las2peer.connectors.webConnector;

import java.util.ArrayList;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.notification.RunListener;

import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.testing.TestSuite;

public abstract class AbstractTestHandler extends RunListener {

	protected int networkSize = 1;
	protected ArrayList<PastryNodeImpl> nodes;
	protected WebConnector connector;
	protected Client webClient;

	@Before
	public void beforeTest() {
		try {
			nodes = TestSuite.launchNetwork(networkSize);
			connector = new WebConnector(null);
			connector.start(nodes.get(0));
			// don't follow redirects, some tests want to test for correct redirect responses
			webClient = ClientBuilder.newBuilder().register(MultiPartFeature.class)
					.property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE).build();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@After
	public void afterTest() {
		for (PastryNodeImpl node : nodes) {
			try {
				node.shutDown();
			} catch (Exception e) {
				// XXX do we care?
			}
		}
	}

}
