package i5.las2peer.httpConnector.coder;



import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Test;


/**
 * TestSerializable.java
 *
 * @author Holger Jan�en
 * @version	$Revision: 1.1 $, $Date: 2013/01/23 20:08:04 $
 */



public class SerializableTest
{
		
	@Test
	public void testSerial () throws IOException, InvalidCodingException, ParameterTypeNotImplementedException {
		SomeSerializable testee = new SomeSerializable ( 100 );
		
		StringWriter out = new StringWriter();
		XmlCoder coder = new XmlCoder( out );
		
		coder.header(1);
		coder.write( testee );
		coder.footer();
		
		out.close ();
		String s = out.toString();
		
		System.out.println(s);
		
		
		StringReader in = new StringReader ( s );
		
		XmlDecoder decoder = new XmlDecoder ( in );
		
		decoder.checkHeader();
		Object retrieved = decoder.decodeSingle();
		decoder.checkFooter();
		SomeSerializable ts = (SomeSerializable) retrieved;
		
		assertEquals( testee.getI(), ts.getI() );
		assertEquals( testee.getJ(), ts.getJ() );
		
		System.out.println(retrieved);
		
	}
	
	@Test
	public void testSerArray () throws ParameterTypeNotImplementedException, IOException, InvalidCodingException {
		Object[] arr = new Object[10];
		
		arr[0] = new SomeSerializable ( 100 );
		arr[3] = new java.util.Date();
		arr[5] = new Integer ( 200 );
				
		StringWriter out = new StringWriter();
		XmlCoder coder = new XmlCoder( out );
		
		coder.header(arr.length);
		
		for (int i=0; i<arr.length; i++ )
			coder.write( arr[i] );
		coder.footer();
		
		out.close ();
		String s = out.toString();
		
		System.out.println(s);
		
		StringReader in = new StringReader ( s );
		
		XmlDecoder decoder = new XmlDecoder ( in );
		
		decoder.checkHeader();
		Object[] retrieved = (Object[]) decoder.decodeArray();
		decoder.checkFooter();
	

		assertNull ( retrieved[1] );
		assertNull ( retrieved[2] );
		assertNull ( retrieved[1] );
		assertNull ( retrieved[4] );
		assertNull ( retrieved[6] );
		assertNull ( retrieved[7] );
		assertNull ( retrieved[8] );
		assertNull ( retrieved[9] );
		
		assertEquals ( 100, ((SomeSerializable) retrieved[0]).getI() );
		assertEquals ( 110, ((SomeSerializable) retrieved[0]).getJ() );
		
		assertEquals( arr[3], retrieved[3] );
		assertEquals( 200, arr[5] );
	}
	

	
}

