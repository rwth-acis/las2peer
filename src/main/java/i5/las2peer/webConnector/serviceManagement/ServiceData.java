package i5.las2peer.webConnector.serviceManagement;



/**
*
 */
public class ServiceData
{
    private String serviceName;
    private String serviceVersion;
    private boolean isActive;
    private String xml;

    public String getServiceName()
    {
        return serviceName;
    }

    public String getServiceVersion()
    {
        return serviceVersion;
    }

    public boolean isActive()
    {
        return isActive;
    }

    public String getXml()
    {
        return xml;
    }



    public ServiceData(String serviceName, String serviceVersion, boolean isActive, String xml)
    {
        this.serviceName = serviceName;
        this.serviceVersion = serviceVersion;
        this.isActive = isActive;
        this.xml = xml;
    }

    public void enable()
    {
        isActive=true;
    }

    public void disable()
    {
        isActive=false;
    }


}
