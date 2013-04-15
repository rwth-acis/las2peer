/**
 * Simple tests for the XML decoder
 *
 * @author Holger Jan�en
 * @version	$Revision: 1.1 $, $Date: 2013/01/23 20:08:04 $
 */

package i5.las2peer.httpConnector;

import i5.las2peer.httpConnector.coder.InvalidCodingException;
import i5.las2peer.httpConnector.coder.XmlDecoder;

import java.io.StringReader;

import org.junit.*;
import static org.junit.Assert.*;

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

