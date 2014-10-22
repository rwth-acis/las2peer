package i5.las2peer.p2p;

import i5.las2peer.security.ServiceAgent;
import rice.pastry.NodeHandle;

import java.io.Serializable;
import java.util.*;

/**
 * This class stores information about each service, that is registered in the network
 */
public class ServiceList implements Serializable
{

    private static final long serialVersionUID = -4423523057987069063L;

    private HashMap<String, ServiceNameVersion> services = new HashMap<String, ServiceNameVersion>();


    /**
     * Default constructor
     */
    public ServiceList()
    {

    }

    /**
     * Returns an array with the names of all registered services
     * @return the service names
     */
    public ServiceNameVersion[] getServices()
    {

        ServiceNameVersion[] result = new ServiceNameVersion[services.size()];

		Iterator it = services.entrySet().iterator();
		int i=0;
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry)it.next();
			result[i++]= (ServiceNameVersion) pairs.getValue();
			it.remove(); // avoids a ConcurrentModificationException
		}

		return result;
    }



	/**
	 * Adds a new service to the list

	 */
    public void addService(ServiceNameVersion servicenameVersion)
    {

		if(!services.containsKey(servicenameVersion.getNameVersion()))
        {
            services.put(servicenameVersion.getNameVersion(), servicenameVersion);
        }
    }

	/**
	 * Removes a service from the list

	 */
    public void removeService(ServiceNameVersion servicenameVersion)
    {
        String name = servicenameVersion.getNameVersion();
		if(services.containsKey(name))
        {
			services.remove(name);
        }

    }


}
