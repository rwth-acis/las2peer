package i5.las2peer.p2p;

import static org.junit.Assert.*;
import i5.las2peer.persistency.MalformedXMLException;
import i5.simpleXML.XMLSyntaxException;

import org.junit.Test;

public class NodeInformationTest {

	@Test
	public void testXmlAndBack() throws XMLSyntaxException, MalformedXMLException {

		NodeInformation testee = NodeInformation.createFromXml(
				"<las2peerNode><adminEmail>test@bla.com</adminEmail><adminName>Steven</adminName><description>some desc</description></las2peerNode>");

		assertEquals("test@bla.com", testee.getAdminEmail());
		assertEquals("Steven", testee.getAdminName());
		assertNull(testee.getOrganization());
		assertEquals(0, testee.getHostedServices().length);
		assertEquals("some desc", testee.getDescription());

		String xml = testee.toXmlString();

		NodeInformation testee2 = NodeInformation.createFromXml(xml);
		assertEquals("test@bla.com", testee2.getAdminEmail());
		assertEquals("Steven", testee2.getAdminName());
		assertNull(testee2.getOrganization());
		assertEquals(0, testee2.getHostedServices().length);
		assertEquals("some desc", testee2.getDescription());

	}

}
