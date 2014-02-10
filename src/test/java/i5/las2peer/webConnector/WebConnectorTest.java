package i5.las2peer.webConnector;



import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class WebConnectorTest {

	private static final String HTTP_ADDRESS = "http://127.0.0.1";
	private static final int HTTP_PORT = WebConnector.DEFAULT_HTTP_PORT;
	
	private static LocalNode node;
	private static WebConnector connector;
	private static ByteArrayOutputStream logStream;
	
	private static UserAgent testAgent;
	private static final String testPass = "adamspass";
	
	private static final String testServiceClass = "i5.las2peer.webConnector.TestService";
	
	@BeforeClass
	public static void startServer () throws Exception {
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
		connector = new WebConnector();
		connector.setSocketTimeout(10000);
		connector.setLogStream(new PrintStream ( logStream));
		connector.start ( node );

		// eve is the anonymous agent!
		testAgent = MockAgentFactory.getAdam();
	}
	
	@AfterClass
	public static void shutDownServer () throws Exception {
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
	
	@Test
	public void testNotExistingService() {
		
		
		
		TestClient c = new TestClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);		
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			c.setServiceClass("ad");
			c.sendRequest("GET", "", "");			
			fail ( "Not existing service caused no exception" );
		}
		catch(HttpErrorException e)
		{
			assertEquals(503,e.getErrorCode());
		}
		catch(Exception e)
		{
			fail ("Not existing service caused wrong exception");
		}
		
	}
	
	
	
	@Test
	public void testLogin() {
		
		
		
		TestClient c = new TestClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		//correct, id based
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			c.setServiceClass(testServiceClass);
			String result=c.sendRequest("GET", "", "");			
			assertEquals("OK",result.trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}	
		
		//correct, name based
		try
		{
			c.setLogin("adam", testPass);
			c.setServiceClass(testServiceClass);
			String result=c.sendRequest("GET", "", "");			
			assertEquals("OK",result.trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}	
		
		//invalid password
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), "aaaaaaaaaaaaa");
			c.setServiceClass(testServiceClass);
			c.sendRequest("GET", "", "");			
			fail ( "Login with invalid password caused no exception");
		}
		catch(HttpErrorException e)
		{
			assertEquals(401,e.getErrorCode());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}	
		//invalid user
		try
		{
			c.setLogin(Long.toString(65464), "aaaaaaaaaaaaa");
			c.setServiceClass(testServiceClass);
			c.sendRequest("GET", "", "");			
			fail ( "Login with invalid user caused no exception");
		}
		catch(HttpErrorException e)
		{
			assertEquals(401,e.getErrorCode());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}	
	}
	@Test
	public void testCalls() 
	{
		TestClient c = new TestClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		//call all methods of the testService
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			c.setServiceClass(testServiceClass);
			String result=c.sendRequest("PUT", "add/5/6", "");			
			assertEquals("11",result.trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}	
		
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			c.setServiceClass(testServiceClass);
			String result=c.sendRequest("POST", "sub/5/6", "");			
			assertEquals("-1",result.trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}	
		
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			c.setServiceClass(testServiceClass);
			String result=c.sendRequest("DELETE", "div/12/6", "");			
			assertEquals("2",result.trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}	
		
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			c.setServiceClass(testServiceClass);
			String result=c.sendRequest("GET", "do/2/it/3?param1=4&param2=5", "");			
			assertEquals("14",result.trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}	
		
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			c.setServiceClass(testServiceClass);
			String result=c.sendRequest("GET", "do/2/it/3/not?param1=4&param2=5", "");			
			assertEquals("-10",result.trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}	
		
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			c.setServiceClass(testServiceClass);
			String result=c.sendRequest("GET", "do/2/this/3/not?param1=4&param2=5", "");			
			assertEquals("-14",result.trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}	
		
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			c.setServiceClass(testServiceClass);
			String result=c.sendRequest("POST", "do/a/b", "c");			
			assertEquals("abc",result.trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}	
	}
}
