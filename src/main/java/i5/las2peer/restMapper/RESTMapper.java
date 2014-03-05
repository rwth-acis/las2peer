package i5.las2peer.restMapper;
import i5.las2peer.restMapper.annotations.ContentParam;
import i5.las2peer.restMapper.annotations.DELETE;
import i5.las2peer.restMapper.annotations.DefaultValue;
import i5.las2peer.restMapper.annotations.GET;
import i5.las2peer.restMapper.annotations.POST;
import i5.las2peer.restMapper.annotations.PUT;
import i5.las2peer.restMapper.annotations.Path;
import i5.las2peer.restMapper.annotations.PathParam;
import i5.las2peer.restMapper.annotations.QueryParam;
import i5.las2peer.restMapper.annotations.Version;
import i5.las2peer.restMapper.data.InvocationData;
import i5.las2peer.restMapper.data.MethodData;
import i5.las2peer.restMapper.data.Pair;
import i5.las2peer.restMapper.data.ParameterData;
import i5.las2peer.restMapper.data.PathTree;
import i5.las2peer.restMapper.data.PathTree.PathNode;
import i5.las2peer.restMapper.exceptions.NoMethodFoundException;
import i5.las2peer.restMapper.exceptions.NotSupportedUriPathException;


import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;

import javax.xml.xpath.XPathFactory;



/**
 * Maps REST requests to Methods
 * Very simple and only supports basic stuff (map paths and extract path parameters and map query parameters)
 * @author Alexander
 *
 */
public class RESTMapper {

    public static final String SERVICES_TAG = "services";
    public static final String END_PATH_PARAMETER = "}";
    public static final String START_PATH_PARAMETER = "{";
    public static final String DELETE = "delete";
    public static final String GET = "get";
    public static final String PUT = "put";
    public static final String POST = "post";
    public static final String DEFAULT_TAG = "default";
    public static final String ANNOTATION_TAG = "annotation";
    public static final String CONTENT_ANNOTATION = "content";
    public static final String QUERY_ANNOTATION = "query";
    public static final String PATH_ANNOTATION = "path";
    public static final String INDEX_TAG = "index";
    public static final String PARAMETER_TAG = "parameter";
    public static final String PARAMTERS_TAG = "parameters";
    public static final String TYPE_TAG = "type";
    public static final String PATH_TAG = PATH_ANNOTATION;
    public static final String HTTP_METHOD_TAG = "httpMethod";
    public static final String METHOD_TAG = "method";
    public static final String VERSION_TAG = "version";
    public static final String DEFAULT_SERVICE_VERSION = "1.0";
    public static final String METHODS_TAG = "methods";
    public static final String NAME_TAG = "name";
    public static final String SERVICE_TAG = "service";
    public static final String PATH_PARAM_BRACES = "{}";

    private static final HashMap<String,Class<?>> classMap= new HashMap<String, Class<?>>();
    public static final String XML = ".xml";
    private static XPath _xPath;
	
	
	
	
	/** 
	 * default constructor
	 */
	public RESTMapper()
	{

	}
	/**
	 * accepts a (service) class and creates an XML file from it
	 * containing all method/parameter information using annotations in the code
	 * @param cl class to extract information fromm
	 * @return XML file
	 * @throws Exception
	 */
	public static String getMethodsAsXML(Class<?> cl) throws Exception
	{
		DocumentBuilderFactory dbFactory;
		DocumentBuilder dBuilder;
		Document doc;
		dbFactory = DocumentBuilderFactory.newInstance();
		Element root;
		Element methodsNode;
		
		dBuilder = dbFactory.newDocumentBuilder();
		doc=dBuilder.newDocument();
		root=doc.createElement(SERVICE_TAG);
		root.setAttribute(NAME_TAG, cl.getName());
		
		methodsNode=doc.createElement(METHODS_TAG);
		root.appendChild(methodsNode);
		doc.appendChild(root);
		
		//gather method and annotation information from class
		Method[] methods = cl.getMethods();	
		Annotation[] classAnnotations=cl.getAnnotations();
		String version=DEFAULT_SERVICE_VERSION;
		for (int i = 0; i < classAnnotations.length; i++) {//get service version if available
			if(classAnnotations[i] instanceof Version)
			{
				version=((Version) classAnnotations[i]).value();
				break;
			}
		}		
		root.setAttribute(VERSION_TAG, version);
		
		for (Method method : methods) { //create method information
			Annotation[] annotations=method.getAnnotations();
			Annotation[][] parameterAnnotations = method.getParameterAnnotations();	
			String httpMethod = getHttpMethod(annotations);
			
			//only valid method, if there is a http method and @Path annotation
			if(httpMethod.isEmpty()||!method.isAnnotationPresent(Path.class))
				continue;
			
			
	    	String path = method.getAnnotation(Path.class).value();
	    	
			Element methodNode=doc.createElement(METHOD_TAG);
			
			
			methodNode.setAttribute(NAME_TAG, method.getName());
			methodNode.setAttribute(HTTP_METHOD_TAG, httpMethod);
			methodNode.setAttribute(PATH_TAG, path);
			methodNode.setAttribute(TYPE_TAG, (method.getReturnType().getName()));
			
			Element parameters =doc.createElement(PARAMTERS_TAG);
			methodNode.appendChild(parameters);
			
			//handle parameters
			Class<?>[] parameterTypes = method.getParameterTypes();			
			for (int i = 0; i < parameterAnnotations.length; i++) 
	    	{//i:=parameterPos
				Element parameter =doc.createElement(PARAMETER_TAG);
				parameter.setAttribute(INDEX_TAG, Integer.toString(i));
				parameter.setAttribute(TYPE_TAG, (parameterTypes[i].getName()));
				String parameterAnnotation=null;
				String parameterName=null;
				String parameterDefault=null;
				
	    		for (int j = 0; j < parameterAnnotations[i].length; j++) 
	    		{//j:=AnnotationNr
					Annotation ann=parameterAnnotations[i][j];
					//check for parameter annotation type
					if(ann instanceof PathParam)
					{
						String paramName=((PathParam) ann).value();
						parameterAnnotation=PATH_ANNOTATION;
						parameterName=paramName;
					}	
					else if(ann instanceof QueryParam)
					{
						String paramName=((QueryParam) ann).value();
						parameterAnnotation=QUERY_ANNOTATION;
						parameterName=paramName;
						
					}
					else if(ann instanceof ContentParam)
					{
						parameterAnnotation=CONTENT_ANNOTATION;
						parameterName="";
					}
					else if(ann instanceof DefaultValue)
					{
						String paramVal=((DefaultValue) ann).value();
						parameterDefault=paramVal;
						
					}
					
					if(parameterAnnotation!=null)	//if an nonexposed parameter is used, works only if default value is provided				
						parameter.setAttribute(ANNOTATION_TAG, parameterAnnotation);
					
					if(parameterName!=null) //not needed for content annotation or if nonexposed parameter is used
						parameter.setAttribute(NAME_TAG, parameterName);
					
					//default value is optional
					if(parameterDefault!=null)
						parameter.setAttribute(DEFAULT_TAG, parameterDefault);
					
				}
	    		parameters.appendChild(parameter);
			}
			methodsNode.appendChild(methodNode);
		}
			
		
		return XMLtoString(doc);
	}
	public static String mergeXMLs(String[] xmls) throws ParserConfigurationException
	{
		
		DocumentBuilderFactory dbFactory;
		DocumentBuilder dBuilder;
		Document doc;		
		dbFactory = DocumentBuilderFactory.newInstance();	
		dBuilder = dbFactory.newDocumentBuilder();
		doc=dBuilder.newDocument();
		Element root=doc.createElement(SERVICES_TAG);
		doc.appendChild(root);
		
		for (int i = 0; i < xmls.length; i++) {
			try {
				Document local=dBuilder.parse(new InputSource(new StringReader(xmls[i])));
				doc.getDocumentElement().appendChild(doc.importNode(local.getDocumentElement(), true));
			} catch (SAXException e) {				
				e.printStackTrace();
			} catch (IOException e) {				
				e.printStackTrace();
			}
		}
		doc.getDocumentElement().normalize();
		return XMLtoString(doc);
		
	}
	
	/**
	 * creates a tree from the class data xml
	 * the tree can then be used to map requests directly to the proper services and methods
	 * @param xml XML containing service class information
	 * @return tree structure for request mapping
	 * @throws Exception
	 */
	public static PathTree getMappingTree(String xml) throws Exception
	{
		 _xPath =  XPathFactory.newInstance().newXPath();
		DocumentBuilderFactory dbFactory;
		DocumentBuilder dBuilder;
		Document doc;		
		dbFactory = DocumentBuilderFactory.newInstance();	
		
		
		dBuilder = dbFactory.newDocumentBuilder();
		doc=dBuilder.parse(new InputSource(new StringReader(xml)));
		
		PathTree rootTree=new PathTree();
		PathNode root=rootTree.getRoot();
		
		// start tree with http methods
		root.addChild(POST);
		root.addChild(PUT);
		root.addChild(GET);
		root.addChild(DELETE);
		
		//for each service in the XML
		NodeList serviceNodeList =(NodeList) _xPath.compile(".//"+SERVICE_TAG).evaluate(doc, XPathConstants.NODESET);
		for (int i = 0; i < serviceNodeList.getLength(); i++) 
		{
			Element serviceNode =(Element)serviceNodeList.item(i);
			String serviceName=serviceNode.getAttribute(NAME_TAG).trim();
			String serviceVersion=serviceNode.getAttribute(VERSION_TAG).trim();
			
			//for each method in a service
			NodeList methodeNodeList =(NodeList) _xPath.compile(".//"+METHOD_TAG).evaluate(serviceNode, XPathConstants.NODESET);
			for (int j = 0; j < methodeNodeList.getLength(); j++) 
			{
				Element methodNode =(Element)methodeNodeList.item(j);
				String methodName=methodNode.getAttribute(NAME_TAG).trim();
				String methodHttpMethod=methodNode.getAttribute(HTTP_METHOD_TAG).trim().toLowerCase();
				String methodPath=methodNode.getAttribute(PATH_TAG).trim();
				String methodType=methodNode.getAttribute(TYPE_TAG).trim();
				
				//begin traversing tree, start from http method node
				PathNode currentNode=root.getChild(methodHttpMethod);
				
				//is there any path to traverse?
				if(methodPath.length()>0){
					
					//transform path in correct format
					if(methodPath.startsWith("/"))
						methodPath=methodPath.substring(1);
					if(methodPath.endsWith("/"))
						methodPath=methodPath.substring(0,methodPath.length()-1);
					
					//for each URI path segment
					String[] pathParts=methodPath.split("/");
				
					for (int l = 0; l < pathParts.length; l++) 
					{	
						//if it is a variable parameter in the path...
						if(pathParts[l].startsWith(START_PATH_PARAMETER)&&pathParts[l].endsWith(END_PATH_PARAMETER))//PathParams are in {}
						{
							currentNode.addChild(PATH_PARAM_BRACES); //add it as a child with {} as name (parameter node)
							currentNode=currentNode.getChild(PATH_PARAM_BRACES); //and set is as the current node
							//add the name of the parameter to a list, for later value mapping
							currentNode.addPathParameterName(pathParts[l].substring(1,pathParts[l].length()-1));
						}
						else 
						{
							currentNode.addChild(pathParts[l]); //text content of path as node name
							currentNode=currentNode.getChild(pathParts[l]);	//set new node as active node
						}
					}
				}
				//get parameter information from the method
				NodeList parameterNodeList =
						(NodeList) _xPath.compile(".//"+PARAMETER_TAG).evaluate(methodNode, XPathConstants.NODESET);
				ParameterData[] parameters=new ParameterData[parameterNodeList.getLength()];
				
				
				for (int k = 0; k < parameterNodeList.getLength(); k++) 
				{
					Element parameter =(Element)parameterNodeList.item(k);
					
					int parameterIndex=Integer.parseInt(parameter.getAttribute(INDEX_TAG));
					String parameterType=parameter.getAttribute(TYPE_TAG);
					//check of the optional attributes
					String parameterAnnotation=null;					
					if(parameter.hasAttribute(ANNOTATION_TAG))
						parameterAnnotation=parameter.getAttribute(ANNOTATION_TAG).toLowerCase();
					String parameterName=null;
					if(parameter.hasAttribute(NAME_TAG))
						parameterName=parameter.getAttribute(NAME_TAG);
					String parameterDefault=null;
					if(parameter.hasAttribute(DEFAULT_TAG))
						parameterDefault=parameter.getAttribute(DEFAULT_TAG);
					
					
					//create array sorted by the occurrence of the parameter in the method declaration
					parameters[parameterIndex]=
							new ParameterData(parameterAnnotation, parameterIndex,
									parameterName, parameterType, parameterDefault);
				}
				//currentNode is the node, where the URI path traversion stopped, so these paths are then mapped to this method
				//since multiple methods can respond to a single path, a node can store a set of methods from different services
				currentNode.addMethodData(new MethodData(serviceName, serviceVersion, methodName,methodType,parameters));
			}
		}
		return rootTree;
	}
	
	
	/**
	 * gets the proper HTTP method from the used annotations
	 * @param annotations
	 * @return HTTP Method (put,post,get etc...)
	 */
	private static String getHttpMethod(Annotation[] annotations) {
		String httpMethod="";
		for (Annotation ann : annotations) 
		{
			if(ann instanceof POST)
			{
				httpMethod=POST;
			}
			else if(ann instanceof PUT)
			{
				httpMethod=PUT;
			}
			else if(ann instanceof GET)
			{
				httpMethod=GET;
			}
			else if(ann instanceof DELETE)
			{
				httpMethod=DELETE;
			}
		}
		return httpMethod;
	}
	
	/**
	 * receives a request and tries to map it to an existing service and method
	 * @param tree structure to use for the mapping process
	 * @param httpMethod HTTP method of the request
	 * @param uri URI path of the request
	 * @param variables array of parameter/value pairs of the request (query variables)
	 * @param content content of the HTTP body
	 * @return array of matching services and methods, parameter values are already pre-filled.
	 * @throws Exception
	 */
	public static InvocationData[] parse(PathTree tree, String httpMethod, String uri, Pair<String>[] variables, String content) throws Exception
	{
		
		
		//map input values from uri path and variables to the proper method parameters
		HashMap<String,String> parameterValues=new HashMap<String,String> ();
		
		if(uri.startsWith("/"))			
			uri=uri.substring(1);
		
		//start with creating a value mapping using the provided variables
		for (int i = 0; i < variables.length; i++) {
			parameterValues.put(variables[i].getOne(), variables[i].getTwo());
		}
		
		
		
		//begin traversing the tree from one of the http method nodes
		PathNode currentNode=tree.getRoot().getChild(httpMethod);
		
		if(currentNode==null)//if not supported method
			throw new NotSerializableException(httpMethod);
		
		if(uri.trim().length()>0)//is there any URI path?
		{
			String[] uriSplit=uri.split("/");
			for (int i = 0; i < uriSplit.length; i++) //for each segment
			{
				PathNode nextNode=currentNode.getChild(uriSplit[i]); //get child node with segment name
				
				if(nextNode==null)//maybe a PathParam?
				{
					currentNode=currentNode.getChild(PATH_PARAM_BRACES);
					if(currentNode==null)//is it a PathParam?
					{
						throw new NotSupportedUriPathException(httpMethod+" "+uri);
					}
					
					String[] paramNames=currentNode.listPathParameterNames();//it is a PathParam, so get all given names of it
					for (int j = 0; j < paramNames.length; j++) {
						parameterValues.put(paramNames[j], uriSplit[i]); //map the value provided in the URI path to the stored parameter names
					}
					
				}
				else
				{
					currentNode=nextNode; //continue in tree
				}
			}
		}
		//so all segments of the URI where handled, current node must contain the correct method, if there is any
		MethodData[] methodData=currentNode.listMethodData(); 
		if(methodData==null || methodData.length==0)//no method mapped to the URI path?
		{
			throw new NoMethodFoundException(httpMethod+" "+uri);
		}
		//create data needed to invoke the methods stored in this node
		ArrayList<InvocationData> invocationData=new ArrayList<InvocationData>();
		for (int i = 0; i < methodData.length; i++) {
			
			ParameterData[] parameters=methodData[i].getParameters();
			
			Serializable[] values= new Serializable[parameters.length]; //web connector uses Serializable for invocation
			Class<?>[] types= new Class<?>[parameters.length];
			boolean abort=false;
			for (int j = 0; j < parameters.length; j++) {
				
				ParameterData param=parameters[j];
				
				if(param.getAnnotation()!=null && param.getAnnotation().equals(CONTENT_ANNOTATION)) //if it's a content annotation
				{
					values[j]=(Serializable) RESTMapper.castToType(content,param.getType()); //fill it with the given content
					types[j]=param.getType();
					
				}
				else
				{
					if(param.getName()!=null && parameterValues.containsKey(param.getName()))//if parameter has a name (given by an annotation) and a value given
					{
						values[j]=(Serializable) RESTMapper.castToType(parameterValues.get(param.getName()),param.getType()); //use the created value mapping to assign a value
						types[j]=param.getType();
						
					}
					else if(param.hasDefaultValue())//if no name, then look for default value
					{
						values[j]=(Serializable) param.getDefaultValue();
						types[j]=param.getType();
					}
					else //no value could be assigned to the parameter
					{
						
						abort=true; 
						break;
					}
				}
				
			}
			
			if(!abort)//return only methods which can be invoked
			invocationData.add(
					new InvocationData(methodData[i].getServiceName(), 
							methodData[i].getServiceVersion(), methodData[i].getName(),
							methodData[i].getType(),values, types));			
		}
		InvocationData[] result=new InvocationData[invocationData.size()];
		invocationData.toArray(result);
		return result;
	}
	
	/**
	 * prints readable XML
	 * @param doc XML document
	 * @return readable XML
	 */
	public static String XMLtoString(Document doc)
	{
		if(doc!=null)
		{			
			try
			{				
				Transformer t = TransformerFactory.newInstance().newTransformer();
				StreamResult out = new StreamResult(new StringWriter());
				t.setOutputProperty(OutputKeys.INDENT, "yes"); //pretty printing
				t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
				t.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2");
				t.transform(new DOMSource(doc),out);
				return out.getWriter().toString();
			}
			catch(Exception e)
			{
				e.printStackTrace();
				return "";
			}
			
		}
		else
			return "";
	}
	
	/**
	 * Casts received String values to appropriate types the method demands
	 * Currently only supports Strings and primitive types
	 * @param val String value to cast
	 * @param class1 Type the parameter expects
	 * @return returns the proper type as an Object
	 * @throws Exception
	 */
	public static Object castToType(String val, Class<?> class1) throws Exception {
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
	/**
	 * Converts a methods return value to String
	 * @param result value to cast to a String
	 * @return
	 */
	public static String castToString(Object result) {
		if(result instanceof String)
		{
			return (String)result;
		}
		if(result instanceof Integer)
		{
			return Integer.toString((Integer) result);
		}
		if(result instanceof Byte)
		{
			return Byte.toString((Byte) result);
		}
		if(result instanceof Short)
		{
			return Short.toString((Short) result);
		}
		if(result instanceof Long)
		{
			return Long.toString((Long) result);
		}
		if(result instanceof Float)
		{
			return Float.toString((Float) result);
		}
		if(result instanceof Double)
		{
			return Double.toString((Double) result);
		}
		if(result instanceof Boolean)
		{
			return Boolean.toString((Boolean) result);
		}
		if(result instanceof Character)
		{
			return Character.toString((Character) result);
		}
		return result.toString(); //desperate measures
	}
	
	/**
	 * Gets the class type based on a string
	 * needed because int.class.getName() can later not be found by the VM behavior
	 * only Strings and primitive types are supported
	 * @param type name of type given by .class.getName()
	 * @return class type
	 * @throws ClassNotFoundException
	 */
	public static Class<?> getClassType(String type) throws ClassNotFoundException
	{
        initClassmap();
        Class<?> result=classMap.get(type);
        if(result!=null)
        {
            return result;
        }
        else
        {
            return Class.forName(type);
        }
	}

    /**
     * Initializes the String to Class mapping HashSet (faster lookup than else if)
     * Needed to map String Notations of Types to actual primitive Types
     */
    private static void initClassmap()
    {
        if(classMap.isEmpty())
        {
            classMap.put("int", int.class);
            classMap.put("float", float.class);
            classMap.put("byte", byte.class);
            classMap.put("short", short.class);
            classMap.put("long", long.class);
            classMap.put("double", double.class);
            classMap.put("char", char.class);
            classMap.put("boolean", boolean.class);

        }
    }

    /**
     * Looks for all xml files in a directory and its subdirectories
     * Reads each file and puts them into a String array
     * @param dir path to the directory
     * @return array of all found XML contents
     * @throws IOException
     */
    public static String[] readAllXMLFromDir(String dir) throws IOException
    {
        File folder = new File(dir);
        ArrayList<File> files = new ArrayList<File>();
        listFilesForFolder(folder, XML,files);

        String[] xmls=new String[files.size()];
        for(int i = 0; i < xmls.length; i++)
        {
            xmls[i]=getFile(files.get(i));

        }
        return xmls;
    }

    /**
     * Reads a given file
     * @param file file to read
     * @return content of file
     * @throws IOException
     */
    public static String getFile(File file) throws IOException
    {
        String content = null;
        FileReader reader = null;
        try {
            reader = new FileReader(file);
            char[] chars = new char[(int) file.length()];
            reader.read(chars);
            content = new String(chars);
            reader.close();
        }

        finally {
            reader.close();
        }

        return content;
    }

    /**
     * Lists all files matching the given type as suffix
     * @param folder parent folder from where to start looking
     * @param type suffix, e.g. ".xml"
     * @param list reference to result array (stores all files found)
     */
    private static void listFilesForFolder(final File folder,String type, ArrayList<File> list) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry,type,list);
            } else if (fileEntry.getName().toLowerCase().endsWith(type)){
                list.add(fileEntry);
            }
        }
    }
}
