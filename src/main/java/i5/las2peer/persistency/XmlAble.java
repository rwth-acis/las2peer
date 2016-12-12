package i5.las2peer.persistency;

import i5.las2peer.tools.SerializationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * The old XmlAble interface enforced an setStateFromXmlMethod.
 * 
 * Due to several problems I decided to remove this method and leave the <i>deserialization</i> in the hand of the
 * programmer.
 * 
 * If standard methods are to be used, the programmer of an XmlAble class should used one of the following methods
 * 
 * <ol>
 * <li>Use a factory called createFromXml ( String xml )<br>
 * This is the preferred idea. An interface may not enforce static methods however...</li>
 * <li>Use a Constructor with a single String parameter</li>
 * <li>Use a base constructor in combination with setStateFromXml ( String )<br>
 * This corresponds to the old XmlAble.</li>
 * </ol>
 * 
 */
public interface XmlAble {

	/**
	 * create an XmlAble java object from its xml representation
	 * 
	 * @param xml
	 * @param classname
	 * @return deserialized object
	 * @throws MalformedXMLException
	 * @throws SerializationException
	 * @throws ClassNotFoundException
	 */
	public static XmlAble createFromXml(String xml, String classname) throws MalformedXMLException,
			SerializationException, ClassNotFoundException {
		Class<?> c = Class.forName(classname);
		return createFromXml(xml, c);
	}

	/**
	 * this method goes through the three possible standard deserialization methods defined in {@link XmlAble} to try to
	 * deserialize an XmlString into an instance of the given class
	 * 
	 * @param xml
	 * @param c
	 * @return deserialized object
	 * @throws MalformedXMLException
	 * @throws SerializationException
	 */
	public static XmlAble createFromXml(String xml, Class<?> c) throws MalformedXMLException, SerializationException {
		try {
			Method createFromXml = c.getDeclaredMethod("createFromXml", String.class);
			if (Modifier.isStatic(createFromXml.getModifiers())) {
				return (XmlAble) createFromXml.invoke(null, xml);
			}
		} catch (Exception e1) {
			if (e1 instanceof MalformedXMLException) {
				throw (MalformedXMLException) e1;
				// just try next idea
			}
		}

		try {
			Constructor<?> constr = c.getDeclaredConstructor(String.class);
			return (XmlAble) constr.newInstance(xml);
		} catch (Exception e) {
			if (e instanceof MalformedXMLException) {
				throw (MalformedXMLException) e;
				// again just the next one
			}
		}

		try {
			Method setState = c.getMethod("setStateFromXml", String.class);
			XmlAble result = (XmlAble) c.newInstance();
			setState.invoke(result, xml);

			return result;
		} catch (Exception e) {
			if (e instanceof MalformedXMLException) {
				throw (MalformedXMLException) e;
			}
		}

		throw new SerializationException("unable to generate a new instance for the given xml: " + xml);
	}

	/**
	 * Returns a XML representation of this object.
	 *
	 *
	 * @return a XML String representation
	 * @throws SerializationException
	 */
	public String toXmlString() throws SerializationException;

}
