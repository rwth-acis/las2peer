package i5.las2peer.httpConnector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import i5.las2peer.httpConnector.coder.InvalidCodingException;
import i5.las2peer.httpConnector.coder.XmlDecoder;

import java.io.StringReader;

import org.junit.Test;

/**
 * Simple tests for the XML decoder
 *
 * @author Holger Jan&szlig;en
 */

public class TestXmlDecoder 
{
	
	@Test
	public void testIncompleteStringArray () throws Exception {
		
		StringReader input = new StringReader (
			 "<?xml version=\"1.0\"?>\n"
				+"\t<objectlist count=\"1\">\n"
				+"\t\t<param type=\"Array\" class=\"String\" length=\"1\">\n"
				+"\t\t\t<element><![CDATA[[datablock]]]><element>\n"
				+"\t\t</param>\n"
				+"</objectlist>\n"
		);
		
		XmlDecoder testee = new XmlDecoder (  input );
		
		testee.checkHeader();
		try
		{
			testee.decodeArray();
			fail ( "InvalidCodingException should have been thrown!" );
		}
		catch (InvalidCodingException e) {
			assertEquals ( "End for node <element> (String Array) not found before message end!",
						  e.getMessage() );
		}
		
	}

	
}

