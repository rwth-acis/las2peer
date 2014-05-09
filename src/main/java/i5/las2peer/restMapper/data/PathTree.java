package i5.las2peer.restMapper.data;


import i5.las2peer.restMapper.exceptions.ConflictingMethodPathException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
/**
 * Data structure to store method and service data
 * allows mapping of http method and uri to actual services and methods
 * @author Alexander
 *
 */
public class PathTree 
{	
	private PathNode _root;
	/**
	 * constructor, creates a new node as root
	 */
    public PathTree() 
    {
        _root = new PathNode();
       
    }
    /**
     * merges the current tree with another
     * the current tree then contains both data
     * @param tree PathTree to import
     */
	public String merge(PathTree tree)
    {
    	PathNode currentLeft=getRoot();
    	PathNode currentRight=tree.getRoot();
    	StringBuilder sb = new StringBuilder();
    	merge(currentLeft,currentRight, sb);
        return sb.toString();
    	
    }
    /**
     * merges two nodes
     * @param self node of the consumer tree
     * @param other node of the giver tree
     */
    @SuppressWarnings("rawtypes")
	private void merge (PathNode self, PathNode other,StringBuilder sb)
    {


        for (Map.Entry node : other.getMethodData().entrySet()) {
            String key=(String)node.getKey();
            MethodData value=(MethodData)node.getValue();
            if(!self.getMethodData().containsKey(key))//copy all method data
            {
                try
                {
                    self.addMethodData(value);
                }
                catch(ConflictingMethodPathException e)
                {
                    sb.append(e.getMessage()).append("\n");
                }
            }
        }
        String[] parameterNames=other.listPathParameterNames();
        for(String parameterName : parameterNames)
        {
            self.addPathParameterName(parameterName);
        }

        for (Map.Entry node : other.getChildren().entrySet()) { //for each child
    		String key=(String)node.getKey();
    		PathNode value=(PathNode)node.getValue();
    		
			if(!self.getChildren().containsKey(key))//append children
			{
				self.addChild(key, value); //no recursion needed, since each subchild is new to self
				
			}
			else
			{
				merge (self.getChildren().get(key),value, sb); // if both have the same child, recursive call
			}
		}    

    }
    /**
     * 
     * @return root node
     */
    public PathNode getRoot()
    {
    	return _root;
    }
    /**
     * Stores method and service information as a node
     * @author Alexander
     *
     */
	public static class PathNode 
	{
		private HashMap<String,MethodData> data=new HashMap<String,MethodData> ();       
        private HashMap<String,PathNode> children;
        private HashSet<String> pathParameterNames=new HashSet<String>();
        /**
         * 
         * @return method information, key consists of unique service/method combination
         */
        public HashMap<String,MethodData> getMethodData()
        {
        	return data;
        }
        /**
         * 
         * @return array of method information
         */
        @SuppressWarnings("rawtypes")
		public MethodData[] listMethodData()
        {
        	MethodData[] result=new MethodData[data.size()];
        	int i=0;
        	for (Map.Entry d : data.entrySet()) {
        		
        		result[i++]=(MethodData)d.getValue();
        	}
        	return result;
        }
        /**
         * Adds method information to the internal hashMap
         * MethodData.toString is used for the key
         * Only adds method information, if same service as existing entries (avoid interference)
         * @param md method data to add
         */
        public void addMethodData(MethodData md) throws ConflictingMethodPathException
        {
            //only if empty or if same service name, as already existing
        	if(data.size()==0 || (md.getServiceName().equals(data.entrySet().iterator().next().getValue().getServiceName())))
            {

                    data.put(md.toString(),md);
            }
            else
            {
                MethodData old =data.entrySet().iterator().next().getValue();
                throw new ConflictingMethodPathException(md.getServiceName()+"."+md.getName(),old.getServiceName()+"."+old.getName());
            }

        }
        /**
         * 
         * @return true, if node stores any method
         */
        public boolean hasMethodData()
        {
        	return data.size()>0;
        }
        /**
         * 
         * @return child nodes of the current node
         */
        public HashMap<String,PathNode> getChildren()
        {
        	return children;
        }
        /**
         * stores the name of the uri path parameter
         * e.g. if the path is a/{b}/c and this node would store the middle part, 
         * b would be stored as the path parameter name
         * different methods with similar uri structure can have a different parameter name
         * so each name is stored
         * @param name parameter name
         */
        public void addPathParameterName(String name)
        {
        	pathParameterNames.add(name);
        }
        /**
         * 
         * @return array of all stored parameter names
         */
        public String[] listPathParameterNames()
        {
        	String[] result=new String[pathParameterNames.size()];
        	int i=0;
        	for (String name : pathParameterNames) {
				result[i++]=name;
			}
        	return result;
        }
        /**
         * 
         * @return true, if node is not leaf
         */
        public boolean hasChildren()
        {
        	return children.size()>0;
        }
        /**
         * Adds a new child to the current node
         * @param child uri path identificator
         */
        public void addChild(String child)
        {
        	if(!children.containsKey(child))
        	{
        		children.put(child,  new PathNode());
        	}
        }
        /**
         * Adds a new child to the current node
         * @param child uri path identificator
         * @param node PathNode object to use as child
         */
        public void addChild(String child, PathNode node)
        {
        	if(!children.containsKey(child))
        	{
        		children.put(child,  node);
        	}
        }
        /**
         * 
         * @param child name of the child
         * @return child node with specified name
         */
        public PathNode getChild(String child)
        {
        	return children.get(child);
        }
        /**
         * constructor, initializes a new HashMap for children
         */
		public PathNode()
		{			
			children=new HashMap<String,PathNode>();
		}
		/**
		 * constructor, initializes with a new method
		 * @param md method to add to the PathNode
		 */
		/*public PathNode(MethodData md)
		{			
			this();
			addMethodData(md);
		}*/
        
        
    }
}
