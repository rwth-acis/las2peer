package i5.las2peer.p2p;

import i5.las2peer.security.ServiceAgent;
import rice.pastry.NodeHandle;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * 
 */

/**
 * This class stores information about each service, that is registered in the network
 */
public class ServiceInfoData implements Serializable
{

    private static final long serialVersionUID = -4423523057987069063L;

    private HashSet<String> services = new HashSet<String>();
	private HashMap<String,ArrayList<NodeHandle>> availableNodes = new HashMap<String, ArrayList<NodeHandle>>();

    /**
     * Default constructor
     */
    public ServiceInfoData()
    {

    }

    /**
     * Returns an array with the names of all registered services
     * @return the service names
     */
    public ServiceNameVersion[] getServices()
    {

        ServiceNameVersion[] result = new ServiceNameVersion[services.size()];
		String[] serv=new String[services.size()];
        services.toArray(serv);

		for (int i = 0; i < serv.length; i++){
			String[] split=serv[i].split(ServiceNameVersion.SEPERATOR);
			result[i] = new ServiceNameVersion(split[0],split[1]);

		}

		return result;
    }

	/**
	 * Get a list of nodes a service is running on
	 * @param serviceClassName
	 * @return
	 */
	public NodeHandle[] getServiceNodes(String serviceClassName)
	{


		if(availableNodes.containsKey(serviceClassName))//keep number of registered services of this type
		{
			ArrayList<NodeHandle> nodes =availableNodes.get(serviceClassName);
			return nodes.toArray(new NodeHandle[nodes.size()]);
		}
		else
			return new NodeHandle[]{};
	}

	/**
	 * Adds a new service to the list
	 * @param serviceAgent
	 * @param node
	 */
    public void addService(ServiceAgent serviceAgent, Node node)
    {
        ServiceNameVersion name = new ServiceNameVersion(serviceAgent.getServiceClassName(),"1.0");
		NodeHandle id=(NodeHandle)node.getNodeId();

		if(!services.contains(name.getNameVersion()))//keep number of registered services of this type
        {
            services.add(name.getNameVersion());
			ArrayList<NodeHandle> nodes= new ArrayList<NodeHandle>();

			nodes.add(id);
			availableNodes.put(name.getNameVersion(), nodes);
        }
		else if (availableNodes.containsKey(name.getNameVersion()))
		{

			if(!(availableNodes.get(name.getNameVersion()).contains(id)))
			{
				availableNodes.get(name.getNameVersion()).add(id);
			}

		}

    }

	/**
	 * Removes a service from the list
	 * @param serviceAgent
	 * @param node
	 */
    public void removeService(ServiceAgent serviceAgent, Node node)
    {
        ServiceNameVersion name = new ServiceNameVersion(serviceAgent.getServiceClassName(),"1.0");
        if(services.contains(name.getNameVersion()))
        {
			NodeHandle id= (NodeHandle) node.getNodeId();

			availableNodes.get(name.getNameVersion()).remove(id);
			if(availableNodes.get(name.getNameVersion()).size()<=0)
				services.remove(name.getNameVersion());

        }

    }


}
