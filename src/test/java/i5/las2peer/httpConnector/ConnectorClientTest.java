package i5.las2peer.httpConnector;

import i5.las2peer.httpConnector.client.*;

import i5.las2peer.httpConnector.coder.ParameterTypeNotImplementedException;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Test for the http connector client (using http)
 *
 * @author Holger Janßen
 * @version $Revision: 1.8 $
 */


public class ConnectorClientTest 
{
	private static final String HTTP_ADDRESS = "localhost";
	private static final int HTTP_PORT = HttpConnector.DEFAULT_HTTP_CONNECTOR_PORT;

	private LocalNode node;
	private HttpConnector connector;
	private ByteArrayOutputStream logStream;
	
	private UserAgent testAgent;
	private static final String testPass = "adamspass";
	
	private static final String testServiceClass = "i5.las2peer.testing.TestService";
	
	
	@Before
	public void startServer () throws Exception {
		// start Node
		node = LocalNode.newNode();
		node.storeAgent(MockAgentFactory.getEve());
		node.storeAgent(MockAgentFactory.getAdam());
		node.storeAgent(MockAgentFactory.getAbel());
		node.storeAgent( MockAgentFactory.getGroup1());
		node.launch();
		
		ServiceAgent testService = ServiceAgent.generateNewAgent(testServiceClass, "a pass");
		testService.unlockPrivateKey("a pass");
		
		node.registerReceiver(testService);
		
		// start connector
		
		logStream = new ByteArrayOutputStream ();
		connector = new HttpConnector();
		connector.setSocketTimeout(10000);
		connector.setLogStream(new PrintStream ( logStream));
		connector.start ( node );

		// eve is the anonymous agent!
		testAgent = MockAgentFactory.getAdam();
	}
	
	@After
	public void shutDownServer () throws Exception {
		//connector.interrupt();
		
		connector.stop();
		node.shutDown();
		
		connector = null;
		node = null;
		
		LocalNode.reset();
		
		System.out.println("Connector-Log:");
		System.out.println("--------------");
		
		System.out.println(logStream.toString());
		
	}
	
	
	
	
	/**
	 * test for the answer if an not known service is to be accessed
	 *
	 */
	@Test
	public void testNotExistingService () {
		Client c = new Client ( HTTP_ADDRESS, HTTP_PORT );
		try {
			c.invoke ( "dummy", "dummy" );
		} catch (NotFoundException e) {
			// that's intended
		} catch (ConnectorClientException e) {
			e.printStackTrace();
			
			fail ( "exception: " + e );
		}
	}
	
	/**
	 * test for the answer if a not existing method is tried to be accessed
	 *
	 */
	@Test
	public void testNotExistingMethod () {
		Client c = new Client ( HTTP_ADDRESS, HTTP_PORT );
		try {
			c.invoke ( testServiceClass, "dummy" );
		} catch (NotFoundException e) {
			// that's intended
		} catch (ConnectorClientException e) {
			e.printStackTrace();
			fail ( "exception: " + e );
		}
	}
	
	
	/**
	 * tests the login with user name and password
	 *
	 */
	@Test
	public void testLogin() {
		// correct test, id based
		Client c = new Client ( HTTP_ADDRESS, HTTP_PORT, ""+testAgent.getId(), testPass );
		try {
			c.connect();
			c.disconnect();
		} catch (ConnectorClientException e) {
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
		
		// correct test, login name based
		c = new Client ( HTTP_ADDRESS, HTTP_PORT, "adam", testPass);
		try {
			c.connect();
			c.disconnect();
		} catch (ConnectorClientException e) {
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
		
		
		// with invalid password
		c = new Client ( HTTP_ADDRESS, HTTP_PORT, "" + testAgent.getId(), "invalid pass" );
		try {
			c.connect();
		} catch (AuthenticationFailedException e) {
			// that's intended
		} catch (UnableToConnectException e) {
			fail ( "Exception: " + e );
		}

		// with not existing user
		c = new Client ( HTTP_ADDRESS, HTTP_PORT, "invalid user", "foiheufoh" );
		try {
			c.connect();
		} catch (AuthenticationFailedException e) {
			// that's intended
		} catch (UnableToConnectException e) {
			fail ( "Exception: " + e );
		}		
	}
	
	/*@Test
	public void testLoginCryptoPass() {
		
		// correct test
		Client c = new Client ( HTTP_ADDRESS, HTTP_PORT, "crypto$&?", "pass&?" );
		try {
			c.connect();
			c.disconnect();
		} catch (ConnectorClientException e) {
			fail ( "Exception: " + e );
		}
	}*/
	
	
	@Test
	public void testParameters () throws UnableToConnectException, AuthenticationFailedException, TimeoutException, ServerErrorException, AccessDeniedException, NotFoundException, ConnectorClientException {
		Client c = new Client ( HTTP_ADDRESS, HTTP_PORT );
		
		Object[] params = new Object[] { new Integer (10), "hallo", new Long (100), new Boolean (true) };
		
		Object result = c.invoke ( testServiceClass, "multipleArguments", params );
		System.out.println ( "Result of the call: " + result );
		result = c.invoke ( testServiceClass, "multipleArguments2", params );
		System.out.println ( "Result of the call: " + result );
	}
	
	/**
	 * simple test with anonymous connection, three basic
	 * invokations, and disconnection
	 *
	 */
	@Test
	public void testSimpleCall () {
		Client c = new Client ( HTTP_ADDRESS, HTTP_PORT);
		
		try {
			c.connect();
			
			Object result = c.invoke ( testServiceClass, "counter" );
			assertEquals( new Integer(1), result );
			//System.out.println ( "Response of the invoke-request: " + result );
			
			result = c.invoke ( testServiceClass, "counter" );
			assertEquals( result, new Integer (2) );
			//System.out.println ( "Response of the invoke-request: " + result );

			result = c.invoke ( testServiceClass, "counter" );
			assertEquals( result, new Integer (3) );
			//System.out.println ( "Response of the invoke-request: " + result );
			
			c.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
		
	}
	
	
	/**
	 * test of the answer when invoking a methods that throws an exception
	 *
	 */
	@Test
	public void testInternalError () throws ConnectorClientException {
		Client c = new Client ( HTTP_ADDRESS, HTTP_PORT );
		
		try {
			c.invoke ( testServiceClass, "exceptionThrower" );
		} catch (ServerErrorException e) {
			assertNotSame ( e, e.getCause() );
			assertNotNull ( e.getCause() );
			assertEquals ( Exception.class, e.getCause().getClass() );
			
			assertTrue ( e.getCause().getMessage().contains("An Exception to deal with!"));
		}
		
		
		try {
			c.invoke ( testServiceClass, "runtimeExceptionThrower" );
		} catch ( ServerErrorException e ) {
			assertNotSame ( e, e.getCause() );
			assertNotNull ( e.getCause() );
			assertEquals ( RuntimeException.class, e.getCause().getClass() );
		}
		
		
		
		try {
			c.invoke ( testServiceClass, "myExceptionThrower" );
		} catch ( ServerErrorException e ) {
			// hm, how to test this, if the server and the client run in the same jvm?!?
		}
		
		try {
			c.disconnect();
		} catch (UnableToConnectException e) {} catch (InvalidServerAnswerException e) {}
	}
	
	
	/**
	 * test the delivery of an array of strings as the result of an invokation
	 *
	 * @exception   ConnectorClientException
	 *
	 */
	@Test
	public void testStringArrayResult () throws ConnectorClientException {
		Client c = new Client ( HTTP_ADDRESS, HTTP_PORT );
		
		String[] test = (String[]) c.invoke ( testServiceClass, "stringArrayReturner" );
		
		assertEquals( 4, test.length );
		
		assertEquals ( "This", test[0] );
		assertEquals ( "is an", test[1] );
		assertEquals ( "array", test[2] );
		assertEquals ( "with Strings", test[3] );
		
		
		test = (String[]) c.invoke ( testServiceClass, "stringArrayReturner", (Object) new String[]{ "test" } );
		assertEquals( 1, test.length );
		assertEquals( "test", test[0]);
		
	}
	
	
	/**
	 * test the delivery of an empty string array as the result of an invokation
	 *
	 * @exception   ConnectorClientException
	 *
	 */
	@Test
	public void testEmptyStringArrayResult () throws ConnectorClientException {
		Client c = new Client (HTTP_ADDRESS, HTTP_PORT );

		String[] result = (String[]) c.invoke( testServiceClass, "emptyStringArrayReturner" );
		
		assertNotNull( result );
		assertEquals( 0, result.length );
	}
	
	
	
	/**
	 * test calling a method with a return type, that is not implemented in the server
	 *
	 */
	@Test
	public void testUntransportableAnswer () {
		Client c = new Client ( HTTP_ADDRESS, HTTP_PORT );
		
		try {
			c.invoke ( testServiceClass, "getAHash" );
		} catch ( ReturnTypeNotImplementedException e ) {
			//that's correct
		} catch (ConnectorClientException e) {
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
	
	}
	
	
	/**
	 * Test the behaviour on an access to a forbidden method
	 *
	 */
	@Test
	public void testAccessForbidden () {
		Client c = new Client ( HTTP_ADDRESS, HTTP_PORT );
		
		try {
			c.invoke( testServiceClass, "accessForbidden" );
		} catch ( AccessDeniedException e ) {
			// correct
		} catch ( ConnectorClientException e ) {
			e.printStackTrace();
			fail ( "Exception: " + e  ) ;
		}
	}
	
	
	/**
	 * tests the delivery of an array of Strings
	 *
	 */
	@Test
	public void testStringArray() {
		Client c = new Client ( HTTP_ADDRESS, HTTP_PORT ) ;
		String[] params = new String[]{
			"Hallo\nwach\n!",
			"Hallo",
			"wach",
			"!"
		};
		
		try {
			Object result = c.invoke ( testServiceClass, "concatStrings",  (Object) params );
			assertEquals ( result, params[0] + params[1] + params[2] + params[3]);
		} catch  ( Exception e ) {
			fail ( "Exception: " + e );
		}

		params = new String[]{
			"<hmm><xmltest>blablebla\n</xmltest>\n\t\t</hmm>\n",
			"Hallo",
			"wach",
			"<hmm><xmltest>blablebla\n</xmltest>\n\t\t</hmm>",
		};
		
		try {
			Object result = c.invoke ( testServiceClass, "concatStrings", (Object) params );
			assertEquals ( result, params[0] + params[1] + params[2] + params[3]);
		} catch  ( Exception e ) {
			fail ( "Exception: " + e );
		}
	}
	
	
	@Test
	public void testByteArray () {
		Client c = new Client ( HTTP_ADDRESS, HTTP_PORT ) ;
		byte[] paramAB = new byte[]{ 10,20,30,125 };
		
		try {
			Object result = c.invoke ( testServiceClass, "byteAdder", paramAB  );
			
			assertEquals ( result, new Long ( paramAB[0] + paramAB[1] + paramAB[2] + paramAB[3] ));
		} catch  ( Exception e ) {
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
	}
	
	
	
	/**
	 * Test for each possible byte value, if it is correctly shipped and received
	 *
	 * @exception   ConnectorClientException
	 * @exception   ParameterTypeNotImplementedException
	 *
	 */
	@Test
	public void testBigByteArray () throws ConnectorClientException, ParameterTypeNotImplementedException {
		Client c = new Client ( HTTP_ADDRESS, HTTP_PORT );
		byte[] paramArray = new byte [ Byte.MAX_VALUE - Byte.MIN_VALUE+1 ];
		
		for ( int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++ ) {
			paramArray[ i - Byte.MIN_VALUE ] = (byte) i;
		}
		
		byte[] result = (byte[]) c.invoke ( testServiceClass, "byteArrayReturner",  paramArray );
		
		assertEquals( paramArray.length, result.length );
		for ( int i = 0; i< result.length; i++ )
			assertEquals ( paramArray[i], result[i] );
	}
	
	
	
	@Test
	public void testReallyBigByteArray () throws ConnectorClientException {
		Client c = new Client ( HTTP_ADDRESS, HTTP_PORT );
		c.connect();
		
		byte[] ba = new byte[ 1000000 ];
		byte[] result;
		
		Random r = new Random();
		r.nextBytes( ba );
		
		result = (byte[]) c.invoke ( testServiceClass, "byteArrayReturner",  ba );
		
		assertEquals( ba.length, result.length );
		for ( int i = 0; i< result.length; i++ )
			assertEquals ( ba[i], result[i] );
	}

	/*
	@Test
	public void testLongValues () throws ConnectorClientException, ParameterTypeNotImplementedException {
		Client c = new Client ( host, port );
		Random r = new Random ();
		
		for ( int i = 0; i<2000; i++ ) {
			long l = r.nextLong();
			Long result = (Long) c.invoke ( "test_service", "longReturner", new Object [] {new Long(l) } );
			assertEquals ( new Long(l), result );
		}
	 }*/
	
	@Test
	public void testLongArrays () throws ConnectorClientException, ParameterTypeNotImplementedException {
		Client c = new Client (HTTP_ADDRESS, HTTP_PORT );
		Random r = new Random();
		
		for ( int i=0; i<10; i++ ) {
			int length = r.nextInt(100);
			long[] al = new long[length];
			for ( int j=0; j<al.length; j++ )
				al[j] = r.nextLong();
			
			long[] alResult = (long[]) c.invoke ( testServiceClass, "longArrayReturner",  al );
			assertEquals ( al.length, alResult.length );
			
			for ( int j=0; j<al.length; j++ )
				assertEquals ( al[j], alResult[j] );
		}
	}
	
	@Test
	public void testBytes () {
		
		for ( byte b= Byte.MIN_VALUE;  b < Byte.MAX_VALUE; b++ ) {
			char c = (char) (b & 0xFF );
			byte b2 = (byte) (c & 0xFF);
			
			assertEquals ( b, b2 );
		}
	}
	
	
	@Test
	public void testSerializable () throws ParameterTypeNotImplementedException, ConnectorClientException {
		Client c = new Client (HTTP_ADDRESS, HTTP_PORT );
		
		java.util.Date test = new java.util.Date () ;
		
		java.util.Date result = (java.util.Date) c.invoke (testServiceClass, "addADay", test ) ;
				
		assertEquals( test.getTime() + 1000*60*60*24, result.getTime() );
	}
	
	
	
	@Test
	public void testPersistentSession () throws AuthenticationFailedException, UnableToConnectException, ConnectorClientException {
		Client c = new Client ( HTTP_ADDRESS, HTTP_PORT, ""+testAgent.getId(), testPass );
		c.setTryPersistent( true );
		
		c.connect();
		assertTrue ( c.isConnected() );
		assertTrue ( c.getTryPersistent() );
		assertTrue ( c.isPersistent() );
		String sid = c.getSessionId();
		
		Object result = c.invoke ( testServiceClass, "counter" );
		c.invoke ( testServiceClass, "setStoredString", "Hallo" );
		System.out.println("First: " + result);
		
		c.detach();
		
		assertFalse( c.isConnected() );
		assertFalse( c.isPersistent() );
		
		Client d = new Client ( HTTP_ADDRESS, HTTP_PORT, ""+testAgent.getId(), testPass, sid );
		d.connect();
		
		Object result2 = d.invoke (testServiceClass, "counter");
		System.out.println("second: " + result2);
		
		Object result3 = d.invoke ( testServiceClass, "getStoredString" );
		System.out.println("third: " + result3);
		assertEquals ( "Hallo", result3 );
		
		assertTrue ( d.isConnected() );
		assertTrue( d.isPersistent() );
		
		Client f = new Client ( HTTP_ADDRESS, HTTP_PORT, ""+testAgent.getId(), testPass, sid );
		try {
			f.connect();
			fail ( "it should not be possible to open a session twice!" );
		} catch (UnableToConnectException e) {
			// that's correct
		}
		
		d.disconnect();
		
		Client e = new Client ( HTTP_ADDRESS, HTTP_PORT, ""+testAgent.getId(), testPass, sid );
		
		try {
			e.connect();
			fail ( "The session has been closed! - connection should fail." );
		} catch (UnableToConnectException utce) {
			// that's intended
		}
		
	}
	
	
	@Test
	public void testTouch () throws AuthenticationFailedException, UnableToConnectException, InterruptedException, ConnectorClientException {
		Client c = new Client ( HTTP_ADDRESS, HTTP_PORT );
		c.setSessionTimeout( 4000 );
		
		c.connect();
		
		assertTrue( c.isConnected() );
		
		c.disconnect();
		
		assertFalse ( c.isConnected() );
		
		c.connect ();
		
		assertTrue ( c.isConnected( ) );
		Thread.sleep( (int) (c.getTimeoutMs() * 1.5 ));
		
		assertTrue ( c.isConnected( false ) );
		assertFalse ( c.isConnected( true ) );
	}

	
	
	
	@Test
	public void testEnvelopeAccess () throws ConnectorClientException, MalformedXMLException, IOException {
		UserAgent adam = MockAgentFactory.getAdam();
		
		System.out.println ("adam: " + adam.getId());
		
		Client c = new Client ( HTTP_ADDRESS, HTTP_PORT, "" + adam.getId(), "adamspass");
		c.setSessionTimeout ( 1000 );
		c.connect();
		
		c.invoke(testServiceClass, "storeEnvelopeString", "ein test");
		
		Object result = c.invoke( testServiceClass,  "getEnvelopeString", new Object[0] );
		
		assertEquals ( "ein test", result);
		
		c.disconnect();
		
		UserAgent eve = MockAgentFactory.getEve();
		
		Client c2 = new Client ( HTTP_ADDRESS, HTTP_PORT, "" + eve.getId(), "evespass");
		c.setSessionOutdate(3000);
		c.connect();
		
		try {
			result = c2.invoke( testServiceClass,  "getEnvelopeString", new Object[0] );
			fail ( "AccessDeniedException expected");
		} catch ( AccessDeniedException e ) {}
		
	}
	
	
	@Test
	public void testGroupAccess () throws MalformedXMLException, IOException, UnableToConnectException, AuthenticationFailedException, TimeoutException, ServerErrorException, AccessDeniedException, NotFoundException, ConnectorClientException {
		
		
		UserAgent adam = MockAgentFactory.getAdam();
		
		System.out.println ("adam: " + adam.getId());
		
		Client c = new Client ( HTTP_ADDRESS, HTTP_PORT, "" + adam.getId(), "adamspass");
		c.setSessionTimeout ( 1000 );
		c.connect();
		
		c.invoke(testServiceClass, "storeGroupEnvelopeString", "ein test");
		
		Object result = c.invoke( testServiceClass,  "getGroupEnvelopeString", new Object[0] );
		
		assertEquals ( "ein test", result);
		
		c.disconnect();
		
		UserAgent eve = MockAgentFactory.getEve();
		
		Client c2 = new Client ( HTTP_ADDRESS, HTTP_PORT, "" + eve.getId(), "evespass");
		c.setSessionOutdate(3000);
		c.connect();
		
		result = c2.invoke( testServiceClass,  "getGroupEnvelopeString", new Object[0] );
		assertEquals ( "ein test", result);
		
	}
	
	
			

	
}





