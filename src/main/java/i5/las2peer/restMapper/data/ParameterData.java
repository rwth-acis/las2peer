package i5.las2peer.restMapper.data;

import i5.las2peer.restMapper.RESTMapper;

/**
 * Stores data for a parameter of a method
 * @author Alexander
 *
 */
public class ParameterData {
	
	String annotation;
	int index;
	String name;
	Class<?> type;
	Object defaultValue;
	/**
	 * 
	 * @return type of annotation used (e.g. path, content, query)
	 */
	public String getAnnotation()
	{
		return annotation;
	}
	/**
	 * 
	 * @return n-th position of parameter in method declaration
	 */
	public int getIndex()
	{
		return index;
	}
	/**
	 * 
	 * @return name of parameter used in annotation
	 */
	public String getName()
	{
		return name;
	}
	/**
	 * 
	 * @return type of parameter
	 */
	public Class<?> getType()
	{
		return type;
	}
	/**
	 * 
	 * @return default value for parameter, used if no value given
	 */
	public Object getDefaultValue()
	{
		return defaultValue;
	}
	/**
	 * 
	 * @return true, if a default value was set
	 */
	public boolean hasDefaultValue()
	{
		return defaultValue==null;
	}
	/**
	 * constructor
	 * @param annotation  type of annotation used (e.g. path, content, query)
	 * @param index n-th position of parameter in method declaration
	 * @param name name of parameter used in annotation
	 * @param type type of parameter
	 * @param defaultValue default value for parameter, used if no value given
	 * @throws Exception
	 */
	public ParameterData(String annotation, int index, String name, String type, String defaultValue) throws Exception
	{
		
		this.annotation=annotation;
		this.name=name;
		this.index=index;		
		this.type=RESTMapper.getClassType(type);
		
		if(defaultValue!=null)
			this.defaultValue=RESTMapper.castToType(defaultValue, this.type);
	
	}
	/**
	 * constructor
	 * @param annotation  type of annotation used (e.g. path, content, query)
	 * @param index n-th position of parameter in method declaration
	 * @param name name of parameter used in annotation
	 * @param type type of parameter
	 * @param defaultValue default value for parameter, used if no value given
	 * @throws Exception
	 */
	public ParameterData(String annotation, String index, String name, String type, String defaultValue) throws Exception
	{
		this(annotation,Integer.parseInt(index),name,type,defaultValue);
	}

}
