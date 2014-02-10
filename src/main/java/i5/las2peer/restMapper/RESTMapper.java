package i5.las2peer.restMapper;
import i5.las2peer.restMapper.PathTree.PathNode;
import i5.las2peer.restMapper.annotations.ContentParam;
import i5.las2peer.restMapper.annotations.DELETE;
import i5.las2peer.restMapper.annotations.DefaultValue;
import i5.las2peer.restMapper.annotations.GET;
import i5.las2peer.restMapper.annotations.POST;
import i5.las2peer.restMapper.annotations.PUT;
import i5.las2peer.restMapper.annotations.Path;
import i5.las2peer.restMapper.annotations.PathParam;
import i5.las2peer.restMapper.annotations.QueryParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;




/**
 * Maps REST requests to Methods
 * Very simple and only supports basic stuff (map paths and extract path parameters and map query parameters)
 * @author Alexander
 *
 */
public class RESTMapper {
	private static final String PATH_PARAM_BRACES = "{}";
	private static final String QUERYPARAM = "q";
	private static final String PATHPARAM = "p";
	private static final String CONTENTPARAM = "c";
	
	private PathTree _pathMapping=new PathTree();
	/**
	 * Constructor accepts a class name as a parameter and extracts all mapping information
	 * @param cl class name where to look for the methods
	 */
	public RESTMapper(Class<?> cl)
	{
		map(cl); 
	}
	/**
	 * Extracts needed data for later mapping of the RESTful requests
	 * All needed information is provided inside the annotations
	 * A path tree is constructed to look up the method
	 * Also the PathParams&QuereyParams are extracted from the path (position and name)
	 * 
	 * @param cl class name where to look for the methods
	 */
	
	public void map(Class<?> cl)
	{
		_pathMapping.getRoot().getChildren().clear();
		Method[] methods = cl.getMethods();	
		PathNode root=_pathMapping.getRoot();
		//first start with http method for the tree
		root.addChild("post");
		root.addChild("put");
		root.addChild("get");
		root.addChild("delete");
		
		//search all methods for annotations
		for (Method method : methods) 
		{	
			
			Annotation[] annotations=method.getAnnotations();
			Annotation[][] parameterAnnotations = method.getParameterAnnotations();			
			String httpMethod="";
			
			//first: which http method annotation is used?
			for (Annotation ann : annotations) 
			{
				if(ann instanceof POST)
				{
					httpMethod="post";
				}
				else if(ann instanceof PUT)
				{
					httpMethod="put";
				}
				else if(ann instanceof GET)
				{
					httpMethod="get";
				}
				else if(ann instanceof DELETE)
				{
					httpMethod="delete";
				}
			}
			if(httpMethod.isEmpty())//not supported/http method, just skip
				continue;
			
			
			
			
			HashMap<Integer,String> path2nameHash=new HashMap<Integer,String>();//maps pathPosition to a name
			HashMap<String,Integer> name2paramPosHash=new HashMap<String,Integer>();//maps names to method parameters (order by declaration)
			HashMap<Integer,String> paramPos2DefaultHash=new HashMap<Integer,String>();//maps pathPosition to a default value
			//Only @Path is supported
			if (method.isAnnotationPresent(Path.class)) 
			{
		    	Path pathAnnotation = method.getAnnotation(Path.class);	
		    	String path = pathAnnotation.value();
		    	
		    	
		    	// filter first / if available
		    	while(path.startsWith("/"))
		    		path=path.substring(1);
		    	
		    
		    	
		    	String[] pathParts=path.split("/");
		    	
		    	//now traverse path and create/expand mapping tree
		    	PathNode currentNode=root.getChild(httpMethod);
		    	
		    	for (int i = 0; i < pathParts.length; i++) {		    		
		    		
					if(pathParts[i].startsWith("{")&&pathParts[i].endsWith("}"))//PathParams are in {}
					{
						currentNode.addChild(PATH_PARAM_BRACES);
						currentNode=currentNode.getChild(PATH_PARAM_BRACES);
						path2nameHash.put(i,PATHPARAM+pathParts[i].substring(1,pathParts[i].length()-1));//save position and name
					}
					else
					{
						currentNode.addChild(pathParts[i]);
						currentNode=currentNode.getChild(pathParts[i]);						
					}
					
				}
		    	
		    	
		    	
		    	//Parameters
		    	for (int i = 0; i < parameterAnnotations.length; i++) 
		    	{//i:=parameterPos
		    		for (int j = 0; j < parameterAnnotations[i].length; j++) 
		    		{//j:=AnnotationNr
						Annotation ann=parameterAnnotations[i][j];
						if(ann instanceof PathParam)
						{
							String paramName=((PathParam) ann).value();
							name2paramPosHash.put(PATHPARAM+paramName, i);//save name and pos in method params
						}	
						else if(ann instanceof QueryParam)
						{
							String paramName=((QueryParam) ann).value();
							name2paramPosHash.put(QUERYPARAM+paramName, i);//save name and pos in method params
						}
						else if(ann instanceof ContentParam)
						{
							//String paramName=((ContentParam) ann).value();
							name2paramPosHash.put(CONTENTPARAM/*+paramName*/, i);//there will be only one content param anyway
						}
						else if(ann instanceof DefaultValue)
						{
							String paramVal=((DefaultValue) ann).value();
							paramPos2DefaultHash.put(i,paramVal);
							
						}
						//Why? PATHPARAM/QUERYPARAM? to distinguish between PathParams and QueryParams, if they have the same name
						
					}
				}
		    	
		    	MethodData methodData = new MethodData(method,path2nameHash,name2paramPosHash,paramPos2DefaultHash);
		    	currentNode.setMethodData(methodData);
			}
			else
				continue;
		}
		
		
	}
	/**
	 * Parses a RESTful request and invokes the appropriate method, if available
	 * @param obj instance to use for invocation
	 * @param method http Method
	 * @param URI URI without the variables PArt (?var1=val1&...)
	 * @param variables Query variables in format: {var1,val1} as String array
	 * @return String as answer
	 * @throws Throwable 
	 */
	public String parse(Object obj, String method, String URI, String[][] variables, String content) throws Throwable {
			
		if(URI.startsWith("/"))
	
		URI=URI.substring(1);
		String[] URISplit=URI.split("/");
		PathNode currentNode=_pathMapping.getRoot().getChild(method);
		if(currentNode==null)//Oh! Oh! not supported method!
		{
			//Error
			throw new Exception("Not supported HTTP method: "+ method);
		}
		for (int i = 0; i < URISplit.length; i++) 
		{			
			
			PathNode nextNode=currentNode.getChild(URISplit[i]);
			/*System.out.println(URISplit[i]+"---");
			for (String string : currentNode.getChildren().keySet()) {
				System.out.println(string);
			}*/
			if(nextNode==null)//maybe a PathParam?
			{
				
				currentNode=currentNode.getChild(PATH_PARAM_BRACES);
				
				if(currentNode==null)//is it a PathParam?
				{
					throw new Exception("Not supported URI path: "+ URI);
				}
			}
			else
			{
				currentNode=nextNode;
			}
			
		}
		
		
		MethodData methodData=currentNode.getMethodData();
		if(methodData==null)
			throw new Exception("URI : "+ URI+" is not associated with a method");
		return methodData.invoke(obj, URISplit,variables, content); 
	}
}
