package i5.las2peer.webConnector.serviceManagement;


import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.ServiceNameVersion;
import i5.las2peer.persistency.EnvelopeException;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.data.PathTree;

import i5.las2peer.security.ServiceInfoAgent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.Serializable;
import java.io.StringReader;

import java.util.*;

/**
 * @author Alexander
 */
public class ServiceRepositoryManager
{
    private static ServiceRepositoryManager manager;
    private static HashMap<String,ServiceData> serviceRepository = new HashMap<String,ServiceData>();
    private static HashSet<String> checkedServices=new HashSet<>();
    private static PathTree tree;
    private static Timer timer=new Timer();
    private static boolean timerRunning=false;
    private static final int DEFAULT_TIMER_INTERVAL_SECONDS=300;
    private static int timerIntervalSeconds=DEFAULT_TIMER_INTERVAL_SECONDS;

    public static final String SERVICE_SELFINFO_METHOD="getRESTMapping";
    public ServiceRepositoryManager()
    {

    }
    public static ServiceRepositoryManager getManager()
    {
        if (manager==null)
        {
            manager=new ServiceRepositoryManager();

        }
        return manager;
    }



    public static void start(Node node) throws Exception
    {
        startTimer(node);
    }
    public static void start (Node node, int timerIntervalSeconds) throws Exception
    {
        ServiceRepositoryManager.timerIntervalSeconds =timerIntervalSeconds;
        startTimer(node);
    }
    public static void stop() throws Exception
    {
        stopTimer();
    }
    public static boolean hasService(String serviceName)
    {
        if(!serviceRepository.containsKey(serviceName))
            return false;

        return serviceRepository.get(serviceName).isActive();
    }
    private static void stopTimer() throws Exception
    {
        timer.cancel();
        timerRunning=false;
    }

    public static void manualUpdate(final Node node) throws Exception
    {
        executeTimer(node,getServiceInfoAgent(node));
    }
    private static void executeTimer(Node node, ServiceInfoAgent finalAgent)
    {
        try
        {
            ServiceNameVersion[] services=ServiceInfoAgent.getServices();
            checkedServices.clear();
            Iterator it = serviceRepository.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry)it.next();
                checkedServices.add((String) pairs.getKey());

            }


            for(int i = 0; i < services.length; i++)
            {
                String internalServiceName=getInternalServiceName(services[i].getName(),services[i].getVersion());
                // System.out.println("###");
                // System.out.println(internalServiceName);
                // System.out.println("###");
                if(!serviceRepository.containsKey(internalServiceName))//new service
                {

                    String xml="";
                    try
                    {
                        xml=(String) node.invokeGlobally(finalAgent, services[i].getName(), SERVICE_SELFINFO_METHOD, new Serializable[]{});

                        try
                        {


                            //tree.merge(RESTMapper.getMappingTree(xml));
                            ServiceData data = new ServiceData(services[i].getName(),services[i].getVersion(),true,xml);
                            serviceRepository.put(internalServiceName,data);

                            addXML(new String[]{xml});//for compatibility services: a service can also give XML definition to other services

                        }
                        catch(Exception e)
                        {
                            //do nothing for now

                        }
                    }
                                   /* catch(NoSuchServiceMethodException e)
                                    {
                                        //do nothing for now
                                        System.out.println("#################");
                                        System.out.println(e.getMessage());
                                        System.out.println("#################");
                                    }
                                    catch(ServiceInvocationException e)
                                    {
                                        //do nothing for now
                                        System.out.println("#################");
                                        System.out.println(e.getMessage());
                                        System.out.println("#################");
                                    }*/
                    catch(Exception e)
                    {
                        //do nothing for now

                    }



                }
                else if(!serviceRepository.get(internalServiceName).isActive())//enable not active services
                {
                    serviceRepository.get(internalServiceName).enable();
                }
                checkedServices.remove(internalServiceName);

            }



            for(String service: checkedServices)
            {

                serviceRepository.get(service).disable();
            }

        }
        catch(EnvelopeException e)
        {
            //do nothing for now
        }
    }
    private static void startTimer(final Node node) throws Exception
    {
        if(timerRunning)
            return;
        ServiceInfoAgent agent;
        agent = getServiceInfoAgent(node);


        final ServiceInfoAgent finalAgent = agent;
        timerRunning=true;

        timer.scheduleAtFixedRate(
                new TimerTask()
                {
                    public void run()
                    {
                        executeTimer(node,finalAgent);
                    }
                },
                0,      // run first occurrence immediately
                timerIntervalSeconds*1000);  // run every three seconds
    }

    private static ServiceInfoAgent getServiceInfoAgent(Node node) throws Exception
    {
        ServiceInfoAgent agent;
        try
        {
            agent = ServiceInfoAgent.getServiceInfoAgent(ServiceInfoAgent.getDefaultPassphrase());

            if(!node.hasAgent(agent.getId()))
            {

                node.registerReceiver(agent);
            }
        }
        catch(Exception e)
        {
            throw new Exception("Error registering ServiceInfoAgent",e);
        }
        return agent;
    }

    public static void addXML(String[] xmls) throws Exception
    {
        DocumentBuilderFactory dbFactory=DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder=dbFactory.newDocumentBuilder();
        XPath xPath =  XPathFactory.newInstance().newXPath();
        for(String xml : xmls)
        {
            Document doc = dBuilder.parse(new InputSource(new StringReader(xml)));
            NodeList serviceNodeList = (NodeList) xPath.compile(String.format(".//%s", RESTMapper.SERVICE_TAG)).evaluate(doc, XPathConstants.NODESET);
            for(int j = 0; j < serviceNodeList.getLength(); j++)
            {
                Element serviceNode = (Element) serviceNodeList.item(j);
                String serviceName = serviceNode.getAttribute(RESTMapper.NAME_TAG).trim();
                String serviceVersion = serviceNode.getAttribute(RESTMapper.VERSION_TAG).trim();

                tree.merge(RESTMapper.getMappingTree(xml));
                Document d=dBuilder.newDocument();
                d.importNode(serviceNode,true);
                ServiceData data = new ServiceData(serviceName, serviceVersion, true, RESTMapper.XMLtoString(d));
                serviceRepository.put(getInternalServiceName(serviceName,serviceVersion), data);
            }

        }



    }

    private static String getInternalServiceName(String serviceName, String serviceVersion)
    {
        return String.format("%s@%s", serviceName, serviceVersion);
    }
    public static ServiceData getService(String serviceName, String serviceVersion)
    {
        return serviceRepository.get(getInternalServiceName(serviceName,serviceVersion));
    }

    public static void setTree(PathTree tree)
    {
        ServiceRepositoryManager.tree = tree;
    }
}