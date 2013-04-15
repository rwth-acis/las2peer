
package i5.las2peer.httpConnector.coder;


import java.io.IOException;
import java.io.StringWriter;
import java.io.StringReader;

import i5.las2peer.httpConnector.coder.InvalidCodingException;
import java.util.Random;

import org.junit.*;
import static org.junit.Assert.*;


/**
 * TestCoding.java
 *
 * @author Holger Jan�en
 * @version $Revision: 1.1 $, $Date: 2013/01/23 20:08:04 $
 */


public class CodingTest
{
	
	@Test
	public void testUnwrapper () {
		Integer[] ai = new Integer[] { 1,2,3,4,6,7 };
		
		int[] ai2 = (int[]) ParamCoder.unwrapArray ( ai );
		for ( int i=0; i< ai2.length; i++ ) {
			System.out.println("" + i + ": " + ai2[i]);
			assertEquals( (int)ai[i], ai2[i] );
		}
	}
	
	@Test
	public void testString () throws IOException, ParameterTypeNotImplementedException, InvalidCodingException {
		String test = "Dies ist ein String!";
		
		StringWriter out = new StringWriter();
		
		XmlCoder coder = new XmlCoder (out);
		
		coder.header(1);
		coder.write ( test );
		coder.footer();
		
		out.close();
		
		String message = out.toString();
		System.out.println(message);
		
		XmlDecoder decoder = new XmlDecoder ( new StringReader ( message ) );
		
		decoder.checkHeader();
		String retrieved = (String) decoder.decodeSingle();
		decoder.checkFooter();
		
		assertEquals( test, retrieved );
	}
	
	@Test
	public void testNewlineString () throws IOException, ParameterTypeNotImplementedException, InvalidCodingException {
		String test = "Dies ist ein String!\nMit Zeilenwechsel!";
		
		StringWriter out = new StringWriter();
		
		XmlCoder coder = new XmlCoder (out);
		
		coder.header(1);
		coder.write ( test );
		coder.footer();
		
		out.close();
		
		String message = out.toString();
		System.out.println(message);
		
		XmlDecoder decoder = new XmlDecoder ( new StringReader ( message ) );
		
		decoder.checkHeader();
		String retrieved = (String) decoder.decodeSingle();
		decoder.checkFooter();
		
		assertEquals( test, retrieved );
	}
	
	
	@Test
	public void testMessage () throws InvalidCodingException, IOException {
		String message = "<?xml version=\"1.0\" ?>\n"
		+	"<objectlist count=\"6\">\n"
		    +"\t<param type=\"int\">1</param>\n"
 			+"\t<param type=\"int\">1</param>\n"
			+"\t<param type=\"int\">1</param>\n"
			+"\t<param type=\"String\"><![CDATA[Post with Attachment]]></param>\n"
			+"\t<param type=\"String\"><![CDATA[This post should contain 2 attachments.]]></param>\n"
			+"\t<param type=\"Array\" class=\"String\" length=\"2\">\n"
			+"\t\t<element><![CDATA[http://www.rwth-aachen.de]]></element>\n"
			+"\t\t<element><![CDATA[ www-i5.informatik.rwth-aachen.de]]></element>\n"
			+"\t</param>\n"
			+"</objectlist>\n\n";
		
		XmlDecoder decoder = new XmlDecoder ( new StringReader ( message ) );
		decoder.checkHeader ();
		
		Object[] result = decoder.decodeArray();
		
		decoder.checkFooter ();
		
		assertEquals ( Integer.class, result[0].getClass() );
		assertEquals ( Integer.class, result[1].getClass() );
		assertEquals ( Integer.class, result[2].getClass() );
		
	}
	
	
	@Test
	public void testLongArray () throws IOException, ParameterTypeNotImplementedException, InvalidCodingException {
		Random r = new Random ();
		int length = 500 + r.nextInt(500);
		long[] testee = new long[length];
		
		for (int i = 0; i<testee.length; i++ ) {
			testee[i] = r.nextLong();
		}

		StringWriter out = new StringWriter();
		XmlCoder coder = new XmlCoder ( out );
		
		coder.header(1);
		coder.write ( testee );
		coder.footer();
		
		String code = out.toString();
		
		System.out.println( "LongOutput: " + out.toString() );
		
		StringReader in = new StringReader ( code );
		XmlDecoder dec = new XmlDecoder ( in );
		
		dec.checkHeader();
		long[] result = (long[]) dec.decodeSingle();
		dec.checkFooter();
		
		System.out.println(result);
		
		// compare result;
		for ( int i=0; i<testee.length; i++ )
			assertEquals( testee[i], result[i] );
	}
	
	@Test
	public void testWrapperLongArray () throws IOException, ParameterTypeNotImplementedException, InvalidCodingException {
		Random r = new Random ();
		int length = 500 + r.nextInt(500);
		Long[] testee = new Long[length];
		
		for (int i = 0; i<testee.length; i++ ) {
			testee[i] = r.nextLong();
		}

		StringWriter out = new StringWriter();
		XmlCoder coder = new XmlCoder ( out );
		
		coder.header(1);
		coder.write ( testee );
		coder.footer();
		
		String code = out.toString();
		
		System.out.println( "LongOutput: " + out.toString() );
		
		StringReader in = new StringReader ( code );
		XmlDecoder dec = new XmlDecoder ( in );
		
		dec.checkHeader();
		Long[] result = (Long[]) dec.decodeSingle();
		dec.checkFooter();
		
		System.out.println(result);
		
		// compare result;
		assertEquals( testee.length, result.length );
		for ( int i=0; i<testee.length; i++ ) {
			assertEquals( testee[i], result[i] );
		}
	}
	
	
	@Test
	public void testDoubleArray () throws IOException, InvalidCodingException {
		Random r = new Random ();
		int length = 500 + r.nextInt(500);
		double[] testee = new double[length];
		for ( int i=0; i<testee.length; i++ )
			testee[i] = r.nextDouble();
		
		StringWriter out = new StringWriter();
		XmlCoder coder = new XmlCoder ( out );
		
		coder.header(1);
		coder.write ( testee );
		coder.footer();
		
		String code = out.toString();
		StringReader in = new StringReader ( code );
		XmlDecoder dec = new XmlDecoder ( in );
		
		dec.checkHeader();
		double[] result = (double[]) dec.decodeSingle();
		dec.checkFooter();
		
		// compare result;
		assertEquals( testee.length, result.length );
		for ( int i=0; i<testee.length; i++ ) {
			assertEquals( testee[i], result[i], 0.001 );
		}
	}
	
	@Test
	public void testEmptyWrapperArray () throws IOException, ParameterTypeNotImplementedException, InvalidCodingException {
		StringWriter out = new StringWriter ();
		XmlCoder coder = new XmlCoder ( out );
		
		coder.header(1);
		coder.write ( new Long[0] );
		coder.footer();
		
		String code = out.toString();
		
		StringReader in = new StringReader ( code );
		XmlDecoder dec = new XmlDecoder ( in );
		dec.checkHeader();
		Long[] result = (Long[]) dec.decodeSingle();
		try {
			dec.checkFooter();
		} catch (InvalidCodingException e) {} catch (IOException e) {}
		
		assertEquals ( 0, result.length );
	}
	
	
}

