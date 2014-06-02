package i5.las2peer.webConnector;

import i5.las2peer.p2p.LocalNode;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.webConnector.client.ClientResponse;
import i5.las2peer.webConnector.client.MiniClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.*;

/**
 * @author Alexander
 */
public class WebConnectorServiceInfoTest
{
    private static final String HTTP_ADDRESS = "http://127.0.0.1";
    private static final int HTTP_PORT = WebConnector.DEFAULT_HTTP_PORT;

    private static LocalNode node;
    private static WebConnector connector;
    private static ByteArrayOutputStream logStream;

    private static UserAgent testAgent;
    private static final String testPass = "adamspass";

    private static final String testServiceClass = "i5.las2peer.webConnector.TestService";
    private static final String testServiceClass2 = "i5.las2peer.webConnector.TestService2";
    private static final String testServiceClass3 = "i5.las2peer.webConnector.Calculator3";
    private static final String testServiceClass4 = "i5.las2peer.webConnector.Calculator2CompatibilityService";
    private static final String testServiceClass5 = "i5.las2peer.webConnector.Calculator2";

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
        ServiceAgent testService3 = ServiceAgent.generateNewAgent(testServiceClass3, "a pass");
        ServiceAgent testService4 = ServiceAgent.generateNewAgent(testServiceClass4, "a pass");
        ServiceAgent testService5 = ServiceAgent.generateNewAgent(testServiceClass5, "a pass");

        testService.unlockPrivateKey("a pass");
        testService2.unlockPrivateKey("a pass");
        testService3.unlockPrivateKey("a pass");
        testService4.unlockPrivateKey("a pass");
        testService5.unlockPrivateKey("a pass");
        node.registerReceiver(testService);
        node.registerReceiver(testService2);
        node.registerReceiver(testService3);
        node.registerReceiver(testService4);
        node.registerReceiver(testService5);
        // start connector

        logStream = new ByteArrayOutputStream ();

        //String xml= RESTMapper.mergeXMLs(new String[]{RESTMapper.getMethodsAsXML(TestService.class), RESTMapper.getMethodsAsXML(TestService2.class)});
        //System.out.println(xml);
        //System.out.println(RESTMapper.getMethodsAsXML(Calculator2.class));
        connector = new WebConnector(true,HTTP_PORT,false,1000,"./XMLCompatibility");
        connector.setSocketTimeout(10000);
        connector.setLogStream(new PrintStream( logStream));
        connector.start ( node );


        // eve is the anonymous agent!
        testAgent = MockAgentFactory.getAdam();

        Thread.sleep(1000);
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
    public void testServices() {
        connector.updateServiceList();
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

        MiniClient c = new MiniClient();
        c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);

        connector.updateServiceList();
        try //Calculator3, only known by XML in ./XMLCompatibility
        {
            c.setLogin(Long.toString(testAgent.getId()), testPass);
            ClientResponse result=c.sendRequest("GET", "5/add/6", "");
            assertEquals("11.0",result.getResponse().trim());

        }

        catch(Exception e)
        {
            fail (e.getMessage());
        }

        try //Testservice1, only known by XML in ./XMLCompatibility AND getRESTMapping()
        {
            c.setLogin(Long.toString(testAgent.getId()), testPass);
            ClientResponse result=c.sendRequest("PUT", "add/7/6", "");
            assertEquals("13",result.getResponse().trim());

        }

        catch(Exception e)
        {
            fail (e.getMessage());
        }
        try //Calculator2 only indirectly known by Calculator2CompatibilityService
        {
            c.setLogin(Long.toString(testAgent.getId()), testPass);
            ClientResponse result=c.sendRequest("GET", "mul/7/6", "");
            assertEquals("42.0",result.getResponse().trim());

        }

        catch(Exception e)
        {
            fail (e.getMessage());
        }

        try //TestService2, only known by getRESTMapping()
        {
            c.setLogin(Long.toString(testAgent.getId()), testPass);
            ClientResponse result=c.sendRequest("POST", "do/h/i", "!");
            assertEquals("hi!",result.getResponse().trim());

        }

        catch(Exception e)
        {
            fail (e.getMessage());
        }
    }
}
