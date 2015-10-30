package i5.las2peer.persistency;

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
	 * Returns a XML representation of this object.
	 *
	 *
	 * @return a XML String representation
	 */
	public String toXmlString();

}
