package i5.las2peer.webConnector;



import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.restMapper.RESTMapper;
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
	private static final String testServiceClass2 = "i5.las2peer.webConnector.TestService2";
	
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
		ServiceAgent testService2 = ServiceAgent.generateNewAgent(testServiceClass2, "a pass");
		testService.unlockPrivateKey("a pass");
		testService2.unlockPrivateKey("a pass");
		
		node.registerReceiver(testService);
		node.registerReceiver(testService2);
		
		// start connector
		
		logStream = new ByteArrayOutputStream ();
		
		//String xml=RESTMapper.mergeXMLs(new String[]{RESTMapper.getMethodsAsXML(TestService.class),RESTMapper.getMethodsAsXML(TestService2.class)});
		//System.out.println(xml);
        /*System.out.println(RESTMapper.getMethodsAsXML(TestService.class));
        System.out.println(RESTMapper.getMethodsAsXML(TestService2.class));*/

		connector = new WebConnector(true,HTTP_PORT,false,1000, "./XMLCompatibility");
		connector.setSocketTimeout(10000);
		connector.setLogStream(new PrintStream ( logStream));
		connector.start ( node );

		// eve is the anonymous agent!
		testAgent = MockAgentFactory.getAdam();
        //avoid timing errors: wait for the repository manager to get all services, before invoking them

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
	public void testNotMethodService() {
		
		
		
		TestClient c = new TestClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);		
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			
			c.sendRequest("GET", "asdag", "");			
			fail ( "Not existing method caused no exception" );
		}
		catch(HttpErrorException e)
		{
			assertEquals(404,e.getErrorCode());
		}
		catch(Exception e)
		{
			fail ("Not existing service caused wrong exception");
		}
		
	}
	
	/*@Test
	public void testMapping() {
		TestService service = new TestService();
		String output=service.getMapping();
		System.out.println(output);
	}*/

	@Test
	public void testLogin() {
		
		
		
		TestClient c = new TestClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		//correct, id based
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			String result=c.sendRequest("get", "", "");			
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

        //avoid timing errors: wait for the repository manager to get all services, before invoking them
        try
        {
            System.out.println("waiting..");
            Thread.sleep(15000);
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
		TestClient c = new TestClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		//call all methods of the testService
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			
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
