package i5.las2peer.restMapper.data;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Data structure to store method and service data allows mapping of http method and uri to actual services and methods
 * 
 *
 */
public class PathTree {
	private PathNode _root;

	/**
	 * constructor, creates a new node as root
	 */
	public PathTree() {
		_root = new PathNode();

	}

	/**
	 * 
	 * @return root node
	 */
	public PathNode getRoot() {
		return _root;
	}

	/**
	 * Stores method and service information as a node
	 * 
	 * @author Alexander
	 *
	 */
	public static class PathNode {
		private HashMap<String, MethodData> data = new HashMap<String, MethodData>();
		private HashMap<String, PathNode> children;
		private HashSet<String> pathParameterNames = new HashSet<String>();

		/**
		 * 
		 * @return method information, key consists of unique service/method combination
		 */
		public HashMap<String, MethodData> getMethodData() {
			return data;
		}

		/**
		 * 
		 * @return array of method information
		 */
		public MethodData[] listMethodData() {
			return data.values().toArray(new MethodData[] {});
		}

		/**
		 * Adds method information to the internal hashMap MethodData.toString is used for the key Only adds method
		 * information, if same service as existing entries (avoid interference)
		 * 
		 * @param md method data to add
		 */
		public void addMethodData(MethodData md) {
			data.put(md.toString(), md);
		}

		/**
		 * 
		 * @return true, if node stores any method
		 */
		public boolean hasMethodData() {
			return data.size() > 0;
		}

		/**
		 * 
		 * @return child nodes of the current node
		 */
		public HashMap<String, PathNode> getChildren() {
			return children;
		}

		/**
		 * stores the name of the uri path parameter e.g. if the path is a/{b}/c and this node would store the middle
		 * part, b would be stored as the path parameter name different methods with similar uri structure can have a
		 * different parameter name so each name is stored
		 * 
		 * @param name parameter name
		 */
		public void addPathParameterName(String name) {
			pathParameterNames.add(name);
		}

		/**
		 * 
		 * @return array of all stored parameter names
		 */
		public String[] listPathParameterNames() {
			String[] result = new String[pathParameterNames.size()];
			int i = 0;
			for (String name : pathParameterNames) {
				result[i++] = name;
			}
			return result;
		}

		/**
		 * 
		 * @return true, if node is not leaf
		 */
		public boolean hasChildren() {
			return children.size() > 0;
		}

		/**
		 * Adds a new child to the current node
		 * 
		 * @param child uri path identificator
		 */
		public void addChild(String child) {
			if (!children.containsKey(child)) {
				children.put(child, new PathNode());
			}
		}

		/**
		 * Adds a new child to the current node
		 * 
		 * @param child uri path identificator
		 * @param node PathNode object to use as child
		 */
		public void addChild(String child, PathNode node) {
			if (!children.containsKey(child)) {
				children.put(child, node);
			}
		}

		/**
		 * 
		 * @param child name of the child
		 * @return child node with specified name
		 */
		public PathNode getChild(String child) {
			return children.get(child);
		}

		/**
		 * constructor, initializes a new HashMap for children
		 */
		public PathNode() {
			children = new HashMap<String, PathNode>();
		}

	}
}
