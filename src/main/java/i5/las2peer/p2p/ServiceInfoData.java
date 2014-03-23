package i5.las2peer.p2p;

import java.io.Serializable;
import java.util.HashMap;

/**
 * @author Alexander
 */

/**
 * This class stores information about each service, that is registered in the network
 */
public class ServiceInfoData implements Serializable
{

    private static final long serialVersionUID = -4423523057987069063L;

    private HashMap<ServiceNameVersion,Integer> services = new HashMap<>();

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
        services.keySet().toArray(result);

        return result;
    }

    /**
     * Adds a service to the list
     * @param serviceClassName
     */
    public void addService(String serviceClassName, String serviceVersion)
    {
        ServiceNameVersion name = new ServiceNameVersion(serviceClassName,serviceVersion);
        if(!services.containsKey(name))//keep number of registered services of this type
        {
            services.put(name,1);
        }
        else
        {
            services.put(name,services.get(name)+1);
        }

    }

    /**
     * Removes a service from the list
     * @param serviceClassName
     */
    public void removeService(String serviceClassName, String serviceVersion)
    {
        ServiceNameVersion name = new ServiceNameVersion(serviceClassName,serviceVersion);
        if(services.containsKey(name))
        {
            if(services.get(name)>1)
            {
                services.put(name,services.get(name)-1);
            }
            else
            {
                services.remove(name);
            }
        }

    }


}
