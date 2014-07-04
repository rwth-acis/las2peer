package i5.las2peer.p2p;

import java.io.Serializable;

/**
 * 
 */
public class ServiceNameVersion implements Serializable
{
    private static final long serialVersionUID = 2683103174627316556L;

    public String getName()
    {
        return name;
    }

    public String getVersion()
    {
        return version;
    }

    private String name;
    private String version;

    public ServiceNameVersion(String name, String version)
    {
        this.name = name;
        this.version = version;
    }

}