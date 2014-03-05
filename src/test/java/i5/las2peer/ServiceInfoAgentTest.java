package i5.las2peer;


import i5.las2peer.p2p.*;

import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.ServiceInfoAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.tools.*;

import org.junit.BeforeClass;


import org.junit.Test;



import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Alexander
 */
public class ServiceInfoAgentTest
{
    private static final int NODES_AMOUNT = 3;
    public static final int START_PORT=8000;
    private static Node[] nodes= new Node[NODES_AMOUNT];
    private static ServiceInfoAgent[] agents= new ServiceInfoAgent[NODES_AMOUNT];
    private static UserAgent testAgent;
    private static final String testPass = "adamspass";

    @BeforeClass
    public static void start () throws Exception {

    }

    @Test
    public void test() throws UnknownHostException, SerializationException, CryptoException
    {

        ColoredOutput.allOff();


        String host = getHostString();

        nodes[0]=new PastryNodeImpl(START_PORT+0,"");
        for(int i = 1; i < nodes.length; i++)
        {
            nodes[i]=new PastryNodeImpl(START_PORT+i,host+":"+Integer.toString(START_PORT+i-1));
        }


        try
        {
            ServiceInfoAgent agent =  ServiceInfoAgent.getServiceInfoAgent();


            for(int i = 0; i < nodes.length; i++)
            {
                nodes[i].launch();

                nodes[i].registerReceiver(agent);

            }

            PastryNodeImpl node= (PastryNodeImpl) nodes[0];

            String testClass1="i5.las2peer.api.TestService";
            ServiceAgent testService = ServiceAgent.generateNewAgent(testClass1, "a pass");
            testService.unlockPrivateKey("a pass");
            nodes[0].registerReceiver(testService);

            String testClass2="i5.las2peer.api.TestService2";
            ServiceAgent testService2 = ServiceAgent.generateNewAgent(testClass2, "a pass");
            testService2.unlockPrivateKey("a pass");
            nodes[1].registerReceiver(testService2);

            System.out.println();
            ServiceNameVersion [] services = agent.getServices();

            String servicesString="";
            String[] serviceNames=new String[services.length];
            for(int i = 0; i < services.length; i++)
            {
                serviceNames[i]=services[i].getName();
            }

            Arrays.sort(serviceNames);

            assertEquals("i5.las2peer.api.TestServicei5.las2peer.api.TestService2",serviceNames[0]+serviceNames[1]);



        }
        catch(Exception e)
        {
            fail ( "Exception: " + e );
        }



    }

    private String getHostString() throws UnknownHostException
    {
        String[] hostAddress=String.valueOf(InetAddress.getLocalHost()).split("/");
        return hostAddress[hostAddress.length - 1];
    }


}
