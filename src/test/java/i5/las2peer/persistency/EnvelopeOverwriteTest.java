package i5.las2peer.persistency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import i5.las2peer.p2p.ArtifactNotFoundException;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.p2p.StorageException;
import i5.las2peer.security.Agent;
import i5.las2peer.security.AgentException;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.tools.SerializationException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.junit.Before;
import org.junit.Test;

public class EnvelopeOverwriteTest {

	
	private LocalNode node;
	private UserAgent eve;
	private UserAgent adam;

	
	@Before
	public void startServer () throws MalformedXMLException, IOException, AgentException, L2pSecurityException {
		LocalNode.reset();
		
		
		node = LocalNode.newNode();
		
		eve = (UserAgent) MockAgentFactory.getEve();
		adam = (UserAgent) MockAgentFactory.getAdam();
		Agent abel = MockAgentFactory.getAbel();
		
		node.storeAgent(eve);
		node.storeAgent(adam);
		node.storeAgent(abel);
		
		node.launch();
	}
	
	@Test
	public void testWithoutSignature() throws UnsupportedEncodingException, SerializationException, L2pSecurityException, ArtifactNotFoundException, StorageException, EnvelopeException {

		Envelope simple = Envelope.createClassIdEnvelope(new Long ( 100), "simple", adam);
		
		node.storeArtifact(simple);
		

		Envelope overwrite = Envelope.createClassIdEnvelope(new Long ( 200), "simple", eve);
		node.storeArtifact( overwrite );
		
		eve.unlockPrivateKey("evespass");
		
		Envelope fetch = node.fetchArtifact( Envelope.getClassEnvelopeId(Long.class,  "simple") );
				
		fetch.open ( eve );
		assertEquals ( new Long(200), fetch.getContent(Long.class));
	}
	
	@Test
	public void testWithSignature () throws EnvelopeException, L2pSecurityException, UnsupportedEncodingException, SerializationException, ArtifactNotFoundException, StorageException{
		eve.unlockPrivateKey("evespass");
		Envelope simple = Envelope.createClassIdEnvelope(new Long ( 100), "simple", eve);
		simple.open ( eve );
		
		simple.addSignature(eve);
		
		node.storeArtifact(simple);
		
		

		Envelope overwrite = Envelope.createClassIdEnvelope(new Long ( 200), "simple", adam);		
		try {
			node.storeArtifact( overwrite );
			fail ( "L2pSecurityException expected");
		} catch (L2pSecurityException e) {
			// expected
		}
		
		overwrite = Envelope.createClassIdEnvelope(new Long ( 200), "simple", eve);
		overwrite.open ( eve );
		overwrite.addSignature(eve);

		node.storeArtifact(overwrite);
		
		Envelope fetch = node.fetchArtifact( Envelope.getClassEnvelopeId(Long.class,  "simple") );
		
		fetch.open ( eve );
		assertEquals ( new Long(200), fetch.getContent(Long.class));
	}
	
	@Test
	public void testWithSignature2 () throws EnvelopeException, L2pSecurityException, UnsupportedEncodingException, SerializationException, ArtifactNotFoundException, StorageException{
		eve.unlockPrivateKey("evespass");
		adam.unlockPrivateKey("adamspass" );
		Envelope complex = Envelope.createClassIdEnvelope(new Long ( 100), "complex", new Agent[] { eve, adam  });
		complex.open ( eve );
		complex.addSignature(eve);
		
		complex.close ();
		complex.open ( adam );
		complex.addSignature( adam);
		
		node.storeArtifact(complex);
		
		

		Envelope overwrite = Envelope.createClassIdEnvelope(new Long ( 200), "complex", adam);		
		try {
			node.storeArtifact( overwrite );
			fail ( "L2pSecurityExcsption expected");
		} catch (L2pSecurityException e) {
			// expected
		}
		
		overwrite = Envelope.createClassIdEnvelope(new Long ( 200), "complex", adam);
		overwrite.open ( adam );
		overwrite.addSignature(adam);

		node.storeArtifact(overwrite);
		
		Envelope fetch = node.fetchArtifact( Envelope.getClassEnvelopeId(Long.class,  "complex") );
		
		fetch.open ( adam );
		assertEquals ( new Long(200), fetch.getContent(Long.class));
	}
	

}
