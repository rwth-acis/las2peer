package i5.las2peer.restMapper;


import java.io.Serializable;
import java.util.ArrayList;

/**
 * Class for a general response type for all connectors
 */
public class GeneralResponse implements Serializable
{
    private static final long serialVersionUID = 1478553738935879786L;

    private ArrayList<ResponseAttribute> attributes= new ArrayList<ResponseAttribute>();
    private String result ="";

    /**
     * All responses should include some sort of response value
     * @param response value
     */
    public GeneralResponse(String response)
    {
        setResult(response);
    }

    public GeneralResponse()
    {

    }

    /**
     *
     * @return result of the service method call
     */
    public String getResult()
    {
        return result;
    }

    /**
     *
     * @param result value to store as result
     */
    public void setResult(String result)
    {
        this.result = result;
    }

    /**
     * Each response can have additional attributes, like headers, response codes etc.
     * Each attribute can be interpreted by the appropriate connector and ignored by other connectors
     * @param attribute new attribute to add
     */
    public void addAttribute(ResponseAttribute attribute)
    {
        attributes.add(attribute);
    }

    /**
     *
     * @return a list of all stored attributes
     */
    public ArrayList<ResponseAttribute> getAttributes()
    {
        return attributes;
    }

    /**
     *
     * @param targetConnector connector, that should accept a specific attribute
     * @return a list of attributes, that is accepted by targetConnector
     */
    public ArrayList<ResponseAttribute> getAttributesByTargetConnector(String targetConnector)
    {
        ArrayList<ResponseAttribute> temp= new ArrayList<ResponseAttribute>();
        for(ResponseAttribute attr:attributes)
        {
            if(attr.hasTargetConnector(targetConnector))
            {
                temp.add(attr);
            }
        }
        return temp;
    }

    /**
     *
     * @param targetConnector connector, that should accept a specific attribute
     * @param type type of an attribute
     * @return a list of attributes, that is accepted by targetConnector and has a certain type
     */
    public ArrayList<ResponseAttribute> getAttributesByType(String targetConnector, String type)
    {
        return filterAttributesType(type, getAttributesByTargetConnector(targetConnector));
    }

    /**
     *
     * @param targetConnector connector, that should accept a specific attribute
     * @param type type of an attribute
     * @param name name of an attribute
     * @return a list of attributes, that is accepted by targetConnector and has a certain type and name
     */
    public ArrayList<ResponseAttribute> getAttributesByTypeName(String targetConnector, String type, String name)
    {
        return filterAttributesName(name, getAttributesByType(targetConnector, type));
    }

    /**
     *
     * @param targetConnector connector, that should accept a specific attribute
     * @param name name of an attribute
     * @return a list of attributes, that is accepted by targetConnector and has a certain name
     */
    public ArrayList<ResponseAttribute> getAttributesByName(String targetConnector, String name)
    {
        return filterAttributesName(name, getAttributesByTargetConnector(targetConnector));
    }

    /**
     *
     * @param targetConnector connector, that should accept a specific attribute
     * @param type type of an attribute
     * @param name name of an attribute
     * @return the first element, that is accepted by targetConnector and has a certain type and name
     */
    public String getAttributeByTypeName(String targetConnector, String type, String name)
    {
        ArrayList<ResponseAttribute> result= getAttributesByTypeName(targetConnector, type, name);
        if(result.size()>0)
            return result.get(0).getAttributeValue();

        return null;
    }

    /**
     *
     * @param targetConnector connector, that should accept a specific attribute
     * @param name name of an attribute
     * @return the first element, that is accepted by targetConnector and has a certain name
     */
    public String getAttributeByName(String targetConnector, String name)
    {
        ArrayList<ResponseAttribute> result= getAttributesByName(targetConnector,name);
        if(result.size()>0)
            return result.get(0).getAttributeValue();

        return null;
    }

    /**
     * Filters a list of attributes by name
     * @param name name of the attribute
     * @param filter pre-filtered list
     * @return filtered attributes
     */
    private ArrayList<ResponseAttribute> filterAttributesName(String name, ArrayList<ResponseAttribute> filter)
    {
        ArrayList<ResponseAttribute> temp= new ArrayList<ResponseAttribute>();
        for(ResponseAttribute attr:filter)
        {
            String searchPattern = name.trim().toLowerCase();
            if(attr.getAttributeName().toLowerCase().equals(searchPattern))
            {
                temp.add(attr);
            }
        }
        return temp;
    }

    /**
     * Filters a list of attributes by type
     * @param type type of the attribute
     * @param filter pre-filtered list
     * @return filtered attributes
     */
    private ArrayList<ResponseAttribute> filterAttributesType(String type, ArrayList<ResponseAttribute> filter)
    {
        ArrayList<ResponseAttribute> temp= new ArrayList<ResponseAttribute>();
        for(ResponseAttribute attr:filter)
        {
            String searchPattern = type.trim().toLowerCase();
            if(attr.getAttributeType().toLowerCase().equals(searchPattern))
            {
                temp.add(attr);
            }
        }
        return temp;
    }


}
