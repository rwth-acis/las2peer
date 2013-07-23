package i5.las2peer.httpConnector.coder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.Writer;

import org.apache.commons.codec.binary.Base64;

/**
 * The XmlCoder class implements all abstract methods of the {@link ParamCoder}
 * to code parameters of a message into an XML String delivered by the connector
 * (for client request and server answer).
 *
 * @author Holger Jan&szlig;en
 */


public class XmlCoder extends ParamCoder {
	

	
	
	public XmlCoder ( Writer out ) throws IOException {
		super ( out );
	}
	
	/**
	 * Writes a coded byte to the outputstream
	 *
	 * @param    b                   a  byte
	 *
	 * @exception   IOException
	 *
	 */
	public void write(byte b) throws IOException {
		out.write ( "\t<param type=\"byte\">" + b + "</param>\n" );
	}
	
	/**
	 * writes a coded short to the output stream
	 *
	 * @param    s                   a  short
	 *
	 * @exception   IOException
	 *
	 */
	public void write(short s) throws IOException{
		out.write ( "\t<param type=\"short\">" + s + "</param>\n" );
	}
	
	/**
	 * Writes a coded int to the outputstream
	 *
	 * @param    i                   an int
	 *
	 * @exception   IOException
	 *
	 */
	public void write(int i) throws IOException{
		out.write ( "\t<param type=\"int\">" + i + "</param>\n" );
	}
	
	/**
	 * Writes a coded char to the outputstream
	 *
	 * @param    c                   a  char
	 *
	 * @exception   IOException
	 *
	 */
	public void write(char c) throws IOException{
		out.write ( "\t<param type=\"char\">" + c + "</param>\n" );
	}
	
	/**
	 * Writes a coded boolean to the outputstream
	 *
	 * @param    b                   a  boolean
	 *
	 * @exception   IOException
	 *
	 */
	public void write(boolean b) throws IOException{
		out.write ( "\t<param type=\"boolean\">" + b + "</param>\n" );
	}
	
	/**
	 * Writes a coded long to the outputstream
	 *
	 * @param    b                   a  long
	 *
	 * @exception   IOException
	 *
	 */
	public void write(long b) throws IOException {
		out.write ( "\t<param type=\"long\">" + b + "</param>\n" );
	}
	
	/**
	 * Writes a coded double to the outputstream
	 *
	 * @param    b                   a  double
	 *
	 * @exception   IOException
	 *
	 */
	public void write(double b) throws IOException {
		out.write ( "\t<param type=\"double\">" + b + "</param>\n" );
	}
	
	/**
	 * Writes a coded float to the outputstream
	 *
	 * @param    f                   a  float
	 *
	 * @exception   IOException
	 *
	 */
	public void write(float f) throws IOException {
		out.write ( "\t<param type=\"float\">" + f + "</param>\n" );
	}
	
	/**
	 * Writes a coded String to the outputstream
	 *
	 * @param    s                   a  String
	 *
	 * @exception   IOException
	 *
	 */
	public void write(String s) throws IOException {
		//out.write ( "\t<param type=\"String\">" + s.replaceAll ( "<", "&lt;" ) + "</param>\n" );
		out.write ( "\t<param type=\"String\"><![CDATA[" + s + "]]></param>\n" );
	}
	
	/**
	 * Writes a coded byte array to the outputstream
	 *
	 * @param    bytes               a  byte[]
	 *
	 * @exception   IOException
	 *
	 */
	public void write(byte[] bytes) throws IOException {
		Base64 base64 = new Base64();
		byte[] encoded = base64.encode ( bytes );
		
		String s = new String ( encoded );
		
		out.write ( "\t<param type=\"Array\" class=\"byte\" length=\""+s.length()+"\">\n\t\t" );

		out.write ( "<![CDATA[" + s + "]]>" );
		
		out.write ( "\n</param>\n" );
	}
		
	
	
	
	/**
	 * Writes a coded short array to the outputstream
	 *
	 * @param    shorts               a  short[]
	 *
	 * @exception   IOException
	 *
	 */
	public void write(short[] shorts) throws IOException {
		out.write ( "\t<param type=\"Array\" class=\"short\" length=\"" + shorts.length + "\">\n" );
		writeArray ( shorts );
		out.write ( "\t</param>\n" );
	}
	
	/**
	 * Writes a coded integer array to the outputstream
	 *
	 * @param    integers            an int[]
	 *
	 * @exception   IOException
	 *
	 */
	public void write(int[] integers) throws IOException {
		out.write ( "\t<param type=\"Array\" class=\"integer\" length=\"" + integers.length + "\">\n" );
		writeArray ( integers );
		out.write ( "\t</param>\n" );
	}
	
	/**
	 * Writes a coded char array to the outputstream
	 *
	 * @param    characters          a  char[]
	 *
	 * @exception   IOException
	 *
	 */
	public void write(char[] characters) throws IOException {
		out.write ( "\t<param type=\"Array\" class=\"char\" length=\"" + characters.length + "\">\n" );
		writeArray ( characters );
		out.write ( "\t</param>\n" );
	}
	
	/**
	 * Writes a coded long array to the outputstream
	 *
	 * @param    longs               a  long[]
	 *
	 * @exception   IOException
	 *
	 */
	public void write(long[] longs) throws IOException {
		byte[] bytes = new byte[ longs.length * 8 ] ;
		for ( int i=0; i<longs.length; i++ ) {
			byte[] longbytes = toBytes(longs[i]);
			for (int j = 0; j < 8; j++) {
				bytes [ i*8+j] = longbytes[j];
			}
		}

		
		Base64 base64 = new Base64();
		byte[] encoded = base64.encode ( bytes );
		
		String s = new String ( encoded );
		
		out.write ( "\t<param type=\"Array\" class=\"long\" length=\""+s.length()+"\">\n\t\t" );
				
		out.write ( "<![CDATA[" + s + "]]>" );
		
		out.write ( "\n</param>\n" );
	}
	
	/**
	 * Writes a coded double array to the outputstream
	 *
	 * @param    doubles             a  double[]
	 *
	 * @exception   IOException
	 *
	 */
	public void write(double[] doubles) throws IOException {
		out.write ( "\t<param type=\"Array\" class=\"double\" length=\"" + doubles.length + "\">\n" );
		writeArray ( doubles );
		out.write ( "\t</param>\n" );
	}
	
	/**
	 * Writes a coded float array to the outputstream
	 *
	 * @param    floats              a  float[]
	 *
	 * @exception   IOException
	 *
	 */
	public void write(float[] floats) throws IOException {
		out.write ( "\t<param type=\"Array\" class=\"float\" length=\"" + floats.length + "\">\n" );
		writeArray ( floats );
		out.write ( "\t</param>\n" );
	}
	
	
	/**
	 * Writes a coded String array to the outputstream
	 *
	 * @param    strings              a  String[]
	 *
	 * @exception   IOException
	 *
	 */
	public void write(String[] strings) throws IOException {
		out.write ( "\t<param type=\"Array\" class=\"String\" length=\"" + strings.length + "\">\n" );
		
		for ( int i=0; i<strings.length; i++ ) {
			out.write ( "\t\t<element><![CDATA[" + strings[i] + "]]></element>\n" );
		}
		
		out.write ( "\t</param>\n" );
	}
		
	/**
	 * Writes a coded Serializable object stream as byte array to the outputstream
	 *
	 * @param    o                   a  Serializable
	 *
	 * @exception   IOException
	 *
	 */
	public void writeSerializable(Serializable o) throws IOException {
		// transform serializable into byte array
		ByteArrayOutputStream baos = new ByteArrayOutputStream ();
		ObjectOutputStream oos = new ObjectOutputStream ( baos ) ;
		oos.writeObject ( o );
		oos.close();
		
		byte[] abBytes = baos.toByteArray();
		
		// encode byte array to base64
		Base64 base64 = new Base64();
		byte[] encoded = base64.encode ( abBytes );
		String s = new String ( encoded );
		
		// and write to output as tag with CDATA content
		out.write ( "\t<param type=\"Serializable\" class=\"" + o.getClass().getName() + "\" length=\""+s.length()+"\">" );
		out.write ( "<![CDATA[" + s + "]]>" );
		out.write ( "</param>\n" );
	}
	
	/**
	 * Writes a null value as coded parameter to the output stream
	 *
	 * @exception   IOException
	 *
	 */
	public void writeNull() throws IOException {
		out.write ( "\t<param type=\"NULL\"></param>\n" );
	}
	
	
	/**
	 * Writes a coded array of a java wrapper class (Integer, Byte, Long etc.)
	 * ot the outputstram
	 *
	 * @param    o                   an Object
	 *
	 * @exception   IOException
	 * @exception   ParameterTypeNotImplementedException
	 *
	 */
	public void writeWrapperArray(Object o) throws IOException, ParameterTypeNotImplementedException {
		out.write ( "\t<param type=\"Array\" class=\"" );
		// remove java.lang. from classname
		
		out.write(o.getClass().getCanonicalName().replaceAll("java.lang.","").replaceAll("\\[\\]",""));
		
		out.write ( "\" length=\"" );
		out.write ( "" + java.lang.reflect.Array.getLength( o ) );
		out.write ( "\" >\n" );
		
		for ( int i=0; i< java.lang.reflect.Array.getLength( o ); i++ ) {
			out.write("\t\t<element>");
			out.write ( java.lang.reflect.Array.get ( o, i ).toString() ) ;
			out.write ( "</element>\n" );
		}
		out.write ( "\t</param>\n" );
	}
	
	
	/**
	 * Method called by the using class to start the encoding
	 *
	 * @param    count               number of the parameters to code
	 *
	 * @exception   IOException
	 *
	 */
	public void header ( int count ) throws IOException {
		out.write ( "<?xml version=\"1.0\"?>\n" );
		out.write ( "<objectlist count=\"" + count + "\">\n" );
	}
	
	/**
	 * Method called by the using class to finish the coding
	 *
	 * @exception   IOException
	 *
	 */
	public void footer() throws IOException {
		out.write ( "</objectlist>\n" );
		
		out.flush();
	}
	
	
	/**
	 * write a sequence of &lt;param&gt;[value]&lt;/param&gt; strings
	 * from a given array
	 * <br />
	 * per Default, each line will be started with two tab (\t) characters.
	 *
	 * @param    arr                 an array of some kind
	 *
	 * @exception   IOException
	 *
	 */
	private void writeArray ( Object arr ) throws IOException {
		writeArray ( arr, 2 );
	}
	
	/**
	 * write a sequence of &lt;param&gt;[value]&lt;/param&gt; strings
	 * from a given array
	 *
	 * @param    arr                 an array of some kind
	 * @param    depth               number of tabs at the beginning of each line
	 *
	 * @exception   IOException
	 *
	 */
	private void writeArray (Object arr, int depth) throws IOException {
		String tabs = "";
		for ( int i=0; i< depth; i++ )
			tabs += "\t";
		
		for ( int i=0; i<java.lang.reflect.Array.getLength( arr ); i++ ) {
			out.write( tabs );
			out.write( "<element>");
			out.write ( java.lang.reflect.Array.get ( arr, i ).toString() );
			out.write( "</element>\n");
		}
	}
	
	static byte[] toBytes(long n) {
		byte[] b = new byte[8];
		for (int i = 0; i < b.length; i++) {
			b[i] = (byte) n;
			n >>= 8;
		}
		return b;
	}
}


