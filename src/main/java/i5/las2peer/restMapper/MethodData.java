package i5.las2peer.restMapper;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
/**
 * Saves important method data and invokes methods
 * @author Alexander
 *
 */
public class MethodData {

	private static final String QUERYPARAM = "q";
	private static final String CONTENTPARAM = "c";
	private HashMap<Integer, String> _pathMapping;//pos in path to a variable name
	private HashMap<String, Integer> _nameMapping;//variable name to pos in parameter order
	private HashMap<Integer, String> _defaultMapping;//pos in path to a default value
	private Method _method;
	private Class<?>[] _parameterTypes;
	/**
	 * Constructor accepts mappings and the method
	 * @param method method for which a path mapping was found
	 * @param path2nameHash mapping of positions in paths to names
	 * @param name2paramPosHash mapping of names to positions in method parameter list
	 */
	public MethodData(Method method,
			HashMap<Integer, String> path2nameHash,
			HashMap<String, Integer> name2paramPosHash,
			HashMap<Integer,String> paramPos2DefaultHash
			){
		
		_pathMapping=path2nameHash;
		_nameMapping=name2paramPosHash;
		_method=method;	
		_parameterTypes=method.getParameterTypes();
		_defaultMapping=paramPos2DefaultHash;
	}
	/**
	 * returns method name
	 */
	public String toString()
	{
		return _method.getName();
	}
	/**
	 * Invokes the method
	 * @param obj Instance to use for invocation
	 * @param URISplit individual parts of the URI path
	 * @param variables Query variables in format: {var1,val1} as String array
	 * @throws Throwable 
	 */
	public String invoke(Object obj, String[] URISplit, String[][] variables, String content) throws Throwable {
		
		Object[] parameters=new Object[_parameterTypes.length];//Array for the method parameters, which will get filled with values
		HashSet<Integer> parametersReceived= new HashSet<Integer>();//keep track, if all parameters got a value.
		for (Integer pos : _defaultMapping.keySet()) {
			String val=_defaultMapping.get(pos);
			parameters[pos]=castToType(val,_parameterTypes[pos]);
			parametersReceived.add(pos);
		}
		
		for (Integer uriPos : _pathMapping.keySet()) //for each path position:
		{
			String val=URISplit[uriPos];// extract value at appropriate position			
			int pos=_nameMapping.get(_pathMapping.get(uriPos));//look up the corresponding position in the parameters			
			parameters[pos]=castToType(val,_parameterTypes[pos]);//cast and set parameter value
			parametersReceived.add(pos);
		}
		
		for (int i = 0; i < variables.length; i++) //for Query variables
		{
			
			if(variables[i].length<2)//{var1,val1}
				throw new Exception("Query variables not correctly formatted.");
			String attr=QUERYPARAM+variables[i][0];
			String value=variables[i][1];
			
			Integer pos=_nameMapping.get(attr);		
			if(pos==null)//unknown mapping
				throw new Exception("Query variable not known: "+variables[i][0]);
			parameters[pos]=castToType(value,_parameterTypes[pos]);
			parametersReceived.add(pos);
		}
		Integer pos=_nameMapping.get(CONTENTPARAM);	
		if(pos!=null)
		{
			parameters[pos]=castToType(content,_parameterTypes[pos]);
			parametersReceived.add(pos);
		}
		if(parametersReceived.size()!=_parameterTypes.length)//check if all parameters have a value
			throw new Exception("Not all "+_parameterTypes.length+" parameters for: "+this.toString()+" provided!");
		Object result=null;
		try
		{
			result=_method.invoke(obj, parameters);
		}
		catch  (InvocationTargetException e)
		{
			throw e.getCause();
		}
		catch  (Exception e)
		{
			throw e;
		}
		Class<?> returnType=_method.getReturnType();
		if(returnType.equals(void.class))//void, return empty string
		{
			return "";
		}
		
		return castToString(result);
	}
	
	/**
	 * Converts a method return value to String for the response
	 * @param result value to cast to a String
	 * @return
	 */
	private String castToString(Object result) {
		if(result instanceof String)
		{
			return (String)result;
		}
		if(result instanceof Integer)
		{
			return Integer.toString((int) result);
		}
		if(result instanceof Byte)
		{
			return Byte.toString((byte) result);
		}
		if(result instanceof Short)
		{
			return Short.toString((short) result);
		}
		if(result instanceof Long)
		{
			return Long.toString((long) result);
		}
		if(result instanceof Float)
		{
			return Float.toString((float) result);
		}
		if(result instanceof Double)
		{
			return Double.toString((double) result);
		}
		if(result instanceof Boolean)
		{
			return Boolean.toString((boolean) result);
		}
		if(result instanceof Character)
		{
			return Character.toString((char) result);
		}
		return result.toString(); //desperate measures
	}
	/**
	 * Casts received String values to appropriate types the method demands
	 * Currently only supports Strings and primitive types
	 * @param val String value to cast
	 * @param class1 Type the parameter expects
	 * @return returns the proper type as an Object
	 * @throws Exception
	 */
	private Object castToType(String val, Class<?> class1) throws Exception {
		//Byte		
		if(class1.equals(Byte.class)||class1.equals(byte.class))
		{			
			return Byte.valueOf(val);
		}
		//Short		
		if(class1.equals(Short.class)||class1.equals(short.class))
		{			
			return Short.valueOf(val);
		}
		//Long		
		if(class1.equals(Long.class)||class1.equals(long.class))
		{			
			return Long.valueOf(val);
		}
		//Float		
		if(class1.equals(Float.class)||class1.equals(float.class))
		{			
			return Float.valueOf(val);
		}
		//Double		
		if(class1.equals(Double.class)||class1.equals(double.class))
		{			
			return Double.valueOf(val);
		}
		//Boolean		
		if(class1.equals(Boolean.class)||class1.equals(boolean.class))
		{			
			return Boolean.valueOf(val);
		}
		//Char		
		if(class1.equals(Character.class)||class1.equals(char.class))
		{			
			return val.charAt(0);
		}
		//Integer		
		if(class1.equals(Integer.class)||class1.equals(int.class))
		{			
			return Integer.valueOf(val);
		}
		//String
		if(class1.equals(String.class))
		{			
			return val;
		}
		//not supported type
		throw new Exception("Parameter Type: "+class1.getName() +"not supported!");
		
	}

}