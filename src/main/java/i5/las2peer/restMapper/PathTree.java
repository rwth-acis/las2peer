package i5.las2peer.restMapper;

import java.util.HashMap;

public class PathTree 
{	
	private PathNode _root;

    public PathTree() 
    {
        _root = new PathNode();
       
    }
    public PathNode getRoot()
    {
    	return _root;
    }
	public static class PathNode 
	{
		private MethodData _data;       
        private HashMap<String,PathNode> _children;
        
        public MethodData getMethodData()
        {
        	return _data;
        }
        public void setMethodData(MethodData md)
        {
        	_data=md;
        }
        public HashMap<String,PathNode> getChildren()
        {
        	return _children;
        }
        public void addChild(String child)
        {
        	if(!_children.containsKey(child))
        	{
        		_children.put(child,  new PathNode());
        	}
        }
        
        public PathNode getChild(String child)
        {
        	return _children.get(child);
        }
		public PathNode()
		{			
			_children=new HashMap<String,PathNode>();
		}
		public PathNode(MethodData md)
		{			
			this();
			setMethodData(md);
		}
        
        
    }
}
