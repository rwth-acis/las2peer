package i5.las2peer.communication;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.TreeSet;

public class ListMethodsContent implements Serializable {

	public class MethodComparator implements Comparator<String[]>, Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = -3575390237718967189L;

		@Override
		public int compare(String[] o1, String[] o2) {
			if (o1.length != o2.length)
				return o1.length - o2.length;
			else {
				for (int i = 0; i < o1.length; i++)
					if (!o1[i].equals(o2[i]))
						return o1[i].compareTo(o2[i]);

				return 0;
			}
		}

	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8167152562828966791L;

	private TreeMap<String, TreeSet<String[]>> htMethodDescriptions = null;

	private boolean bFinalized = false;

	/**
	 * create a new ListMethodContent as a request
	 */
	public ListMethodsContent() {
		this(true);
	}

	/**
	 * create a new ListMethodsContent either as a request or as a response, a response is open for adding methods after
	 * creation
	 * 
	 * @param request
	 */
	public ListMethodsContent(boolean request) {
		if (request) {
			htMethodDescriptions = null;
			bFinalized = true;
		} else {
			htMethodDescriptions = new TreeMap<String, TreeSet<String[]>>();
		}
	}

	/**
	 * add a method to this response
	 * 
	 * @param m
	 */
	public void addMethod(Method m) {
		if (isRequest() || bFinalized)
			throw new IllegalStateException("not possible to add methods!");

		TreeSet<String[]> multiple = htMethodDescriptions.get(m.getName());
		if (multiple == null) {
			multiple = new TreeSet<String[]>(new MethodComparator());
			htMethodDescriptions.put(m.getName(), multiple);
		}

		Class<?>[] paramTypes = m.getParameterTypes();
		String[] paramTypeNames = new String[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++)
			paramTypeNames[i] = paramTypes[i].getSimpleName();

		multiple.add(paramTypeNames);
	}

	/**
	 * close this response for adding methods
	 */
	@Override
	public void finalize() {
		bFinalized = true;
	}

	/**
	 * get a sorted array with all names of methods stored in this answer
	 * 
	 * @return array with method names
	 */
	public String[] getSortedMethodNames() {
		return htMethodDescriptions.keySet().toArray(new String[0]);
	}

	/**
	 * get a sorted array of all parameter signatures for the given method name
	 * 
	 * @param methodName
	 * @return sorted array with all parameter signatures
	 */
	public String[][] getSortedMethodParameters(String methodName) {
		return htMethodDescriptions.get(methodName).toArray(new String[0][]);
	}

	/**
	 * is this a request or response message?
	 * 
	 * @return true, if this is a request message
	 * 
	 */
	public boolean isRequest() {
		return htMethodDescriptions == null;
	}

	/**
	 * get an XML representation of this method list (this class is not xml able since the way back is not implemented
	 * (yet?)
	 * 
	 * @return xml representation
	 */
	public String toXmlString() {
		StringBuffer result = new StringBuffer("<methodList>\n");

		if (!isRequest()) {
			for (String methodName : getSortedMethodNames()) {
				result.append("\t<method name=\"").append(methodName).append("\">\n");

				for (String[] signature : getSortedMethodParameters(methodName)) {
					result.append("\t\t<signature>\n");
					for (String param : signature)
						result.append("\t\t\t<parameter type=\"").append(param).append("\" />\n");
					result.append("\t\t</signature>\n");
				}

				result.append("\t</method>\n");
			}
		}

		result.append("</methodList>\n");

		return result.toString();
	}

	@Override
	public String toString() {
		return toXmlString();
	}

}
