package i5.las2peer.connectors.nodeAdminConnector;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
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
	protected NodeAdminConnector connector;
	protected Client sslClient;

	@Before
	public void beforeTest() {
		try {
			nodes = TestSuite.launchNetwork(networkSize);
			connector = new NodeAdminConnector(null, false);
			connector.start(nodes.get(0));
			Assert.assertNotNull(connector.getCACertificate());
			Assert.assertNotNull(connector.getCertificate());
			SSLContext ctx = SSLContext.getInstance(NodeAdminConnector.SSL_INSTANCE_NAME);
			ctx.init(null, new TrustManager[] { new TrustManager() {
			} }, null);
			// don't follow redirects, some tests want to test for correct redirect responses
			sslClient = ClientBuilder.newBuilder().sslContext(ctx).register(MultiPartFeature.class)
					.property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE).build();
			sslClient.getSslContext().init(null, new TrustManager[] { new X509TrustManager() {
				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[] { connector.getCACertificate() };
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					if (!Arrays.equals(chain,
							new X509Certificate[] { connector.getCertificate(), connector.getCACertificate() })) {
						throw new CertificateException();
					}
				}

				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					if (!Arrays.equals(chain,
							new X509Certificate[] { connector.getCertificate(), connector.getCACertificate() })) {
						throw new CertificateException();
					}
				}
			} }, null);
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
