package i5.las2peer.restMapper;


import java.io.Serializable;
import java.util.ArrayList;

/**
 * Class to store additional attributes together with a service method result
 */
public class ResponseAttribute implements Serializable
{
    private static final long serialVersionUID = 8461502484887895970L;
    public static final String DEFAULT = "default";
    public static final String ASTERISK = "*";
    private ArrayList<String> targetConnectors=new ArrayList<String>();

    private String attributeType= DEFAULT;
    private String attributeName="";
    private String attributeValue="";

    /**
     * Attributes can have different custom types.
     * For example HTTP-Headers.
     * @return type of the attribute
     */
    public String getAttributeType()
    {
        return attributeType;
    }

    /**
     * An attribute can be identified by a name.
     * Names do not need to be unique.
     * @return name of the attribute
     */
    public String getAttributeName()
    {
        return attributeName;
    }

    /**
     * Each Attribute has a value.
     * @return value of an attribute
     */
    public String getAttributeValue()
    {
        return attributeValue;
    }

    /**
     * Each attribute can specify the connectors, which should (be able) to process the attribute.
     * @return list of compatible connectors (the information is intended to)
     */
    public ArrayList<String> getTargetConnectors()
    {
        return targetConnectors;
    }

    /**
     * Each attribute can specify the connectors, which should (be able) to process the attribute.
     * @param target new connector to add to the internal list
     */
    public void addTargetConnector(String target)
    {
        targetConnectors.add(target.trim().toLowerCase());
    }
    /**
     * Each attribute can specify the connectors, which should (be able) to process the attribute.
     * @param target the connector to check for
     *               use * to accept any connector
     * @return true, if target is one of the accepted connectors
     */
    public boolean hasTargetConnector(String target)
    {
        String temp=target.trim().toLowerCase();
        //Asterisk * for all connectors
        if(temp.equals(ASTERISK))
            return true;
        for(String t:targetConnectors)
        {
            if(t.equals(temp))
            {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param attributeValue new value of the attribute
     */
    public void setAttributeValue(String attributeValue)
    {
        this.attributeValue = attributeValue;
    }

    /**
     *
     * @param name the name of the attribute
     * @param value the value of the attribute
     */
    public ResponseAttribute(String name, String value)
    {
        attributeName=name.trim();
        attributeValue=value.trim();
    }

    /**
     *
     * @param type what is the type of the attribute?
     * @param name the name of the attribute
     * @param value the value of the attribute
     */
    public ResponseAttribute(String type, String name, String value)
    {
        this(name,value);
        attributeType=type.trim();

    }

    /**
     *
     * @param target which connector is the primary recipient of this attribute?
     * @param type what is the type of the attribute?
     * @param name the name of the attribute
     * @param value the value of the attribute
     */
    public ResponseAttribute(String target, String type, String name, String value)
    {
        this(type,name,value);
        targetConnectors.add(target.trim().toLowerCase());
    }



}
