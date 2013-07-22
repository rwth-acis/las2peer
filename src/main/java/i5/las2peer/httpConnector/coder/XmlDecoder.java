package i5.las2peer.httpConnector.coder;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;

/**
 * The XmlDecoder decodes parameters coded by the {@link XmlCoder} and read as XML data
 * from the given reader.
 *
 * @author Holger Jan&szlig;en
 */

public class XmlDecoder extends ParamDecoder {

	private BufferedReader reader;
	
	private int iParamCount = 0;
	
	public XmlDecoder ( InputStream in ) {
		this ( new InputStreamReader (in) );
	}
	
	
	public XmlDecoder ( Reader reader ) {
		this.reader = new BufferedReader ( reader );
	}
	
	/**
	 * Method that should check the header of any message to decode
	 *
	 * @exception   InvalidCodingException
	 * @exception   IOException
	 *
	 */
	public int checkHeader() throws InvalidCodingException, IOException {
		String line = reader.readLine();
		// <?xml version="1.0"...?> expected
		if ( ! line.matches ( "<\\?xml\\s+version=\"1.0\"(\\s+encoding=\"[^\"]+\")?\\s*\\?>" ) )
			throw new InvalidCodingException ( "Excpected xml header, found '" + line +"' instead!" );
		
		do {
			line = reader.readLine ();
		} while ( line.matches ( "^\\s*$" ));
		
		Matcher m = Pattern.compile ( "\\s*<objectlist\\s+count=\"([0-9]+)\"\\s*>\\s*" ).matcher (line);
		
		if ( ! m.matches () )
			throw new InvalidCodingException ("Expected root element objectlist, found '" + line + "' instead!" );
		
		iParamCount = Integer.valueOf( m.group ( 1 ) ).intValue();
		
		return iParamCount;
	}
	
	/**
	 * Method checking the rest of the message after decoding all of the parameters
	 *
	 * @exception   InvalidCodingException
	 * @exception   IOException
	 *
	 */
	public void checkFooter() throws InvalidCodingException, IOException {
		String line = reader.readLine ();
		
		while ( line.matches ( "^\\s*$" ) )
			line = reader.readLine ();
		
		if ( ! line.matches ( "</objectlist>" ) )
			throw new InvalidCodingException ( "Expected closing tag for root element objectlist, but found '" + line + "' instead!" );
				
		do {
			line = reader.readLine ();
			if ( line != null && ! line.matches ( "^\\s*$" ) )
				throw new InvalidCodingException ( "No content after root element allowed!" );
		} while ( line != null );
	}
	
	/**
	 * decode a single parameter
	 *
	 * @return   an Object
	 *
	 * @exception   InvalidCodingException
	 * @exception   IOException
	 *
	 */
	public Object decodeSingle() throws InvalidCodingException, IOException {
		
		if ( iParamCount != 1 )
			throw new InvalidCodingException ( "Expected just one value, but got " + iParamCount + "!" );
		
		Object[] result = decodeArray();
		
		return result[0];
	}
	
	/**
	 * decode an array of parameters
	 *
	 * @return   an Object[]
	 *
	 * @exception   InvalidCodingException
	 * @exception   IOException
	 *
	 */
	public Serializable[] decodeArray() throws InvalidCodingException, IOException {
		Serializable[] result = new Serializable [ iParamCount ];
		
		Pattern pParam = Pattern.compile (
				"\\s*<param\\s+type=\"([A-Za-z\\.0-9]+)\"(\\s+class=\"([A-Za-z\\.0-9;\\[]+)\"\\s+length=\"([0-9]+)\")?\\s*>(.*)" );
		Pattern pSingle = Pattern.compile ( "\\s*<param\\s+[^>]*>([^<]+)</param>\\s*" );
				
		for ( int i=0; i<iParamCount; i++ ) {
			String line = reader.readLine();
			
			while ( line.matches( "^\\s*$" ) )
				line = reader.readLine ();
			
			Matcher mParam = pParam.matcher ( line );
			
			if ( ! mParam.matches() )
				throw new InvalidCodingException ( "Expected element of type param but found '" + line + "'!");
			
			Matcher mSingle = pSingle.matcher ( line );
			
			if ( mParam.group(1).equals ( "NULL" ) ) {
				result[i] = null;
			} else if ( mParam.group(1).equals ( "int" ) ) {
				if (! mSingle.matches())
					throw new InvalidCodingException ("Parameter line does not match!");
				result[i] = Integer.valueOf( mSingle.group(1) );
			} else if ( mParam.group(1).equals ( "boolean" ) ) {
				if (! mSingle.matches())
					throw new InvalidCodingException ("Parameter line does not match!");
				result[i] = Boolean.valueOf( mSingle.group(1 ) );
			} else if ( mParam.group(1).equals ( "char" ) ) {
				if (! mSingle.matches())
					throw new InvalidCodingException ("Parameter line does not match!");
				if ( mSingle.group(1).length() > 1 )
					throw new InvalidCodingException ( "Char parameter too long!" );
				result[i] = new Character (mSingle.group(1 ).charAt(0 ));
			} else if ( mParam.group(1).equals ( "long" ) ) {
				if (! mSingle.matches())
					throw new InvalidCodingException ("Parameter line does not match!");
				result[i] = Long.valueOf( mSingle.group(1 ) );
			} else if ( mParam.group(1).equals ( "float" ) ) {
				if (! mSingle.matches())
					throw new InvalidCodingException ("Parameter line does not match!");
				result[i] = Float.valueOf( mSingle.group(1 ) );
			} else if ( mParam.group(1).equals ( "double" ) ) {
				if (! mSingle.matches())
					throw new InvalidCodingException ("Parameter line does not match!");
				result[i] = Double.valueOf( mSingle.group(1 ) );
			} else if ( mParam.group(1).equals ( "String" ) ) {
				result[i] = readCDataSection( line );
			} else if ( mParam.group(1).equals ( "Serializable" ) ) {
				String sCData = readCDataSection(line);
				
				try {
					result[i] = deserializeString ( sCData );
				} catch (ClassNotFoundException e) {
					throw new InvalidCodingException ("The Class o fthe parameter could not be found!", e );
				}
			} else if ( mParam.group(1).equals ( "Array" ) ){
				result [i] = decodeParamArray ( mParam.group(3), Integer.valueOf ( mParam.group(4)).intValue() );
				
			}
		}
		
		// check, if all elements are of the same type
		if ( result.length > 0 && result[0] != null) {
			@SuppressWarnings("rawtypes")
			Class c = result[0].getClass();
			boolean bCheck = true;
			for ( int i=1; i<result.length && bCheck; i++ )
				bCheck &= (result[i]!= null) && c.equals( result[i].getClass() );
	
			if ( bCheck ) {
				// cast into aray of given class
				Object temp = java.lang.reflect.Array.newInstance( c, result.length );
				
				for ( int j=0; j<result.length; j++ )
					((Object[])temp) [j] = result[j];
				
				return (Serializable[])temp;
			}
		}
		return result;
	}
	
	
	/**
	 * Reads a nested CDATA section and returns it as a string.
	 *
	 * The parsing will be started with the given line and extended to a sufficient
	 * number of lines, if the section does not end within the given line.
	 *
	 * @param    currentLine         a  String
	 *
	 * @return   a String
	 *
	 * @exception   InvalidCodingException
	 * @exception   IOException
	 *
	 */
	private String readCDataSection ( String currentLine) throws InvalidCodingException, IOException {
		String result;

		Pattern pSingleCData = Pattern.compile ( "\\s*<param\\s+[^>]*><!\\[CDATA\\[(.*)\\]\\]></param>\\s*" );
		Pattern pCDataOpen = Pattern.compile ( "\\s*<param\\s+[^>]*><!\\[CDATA\\[(.*)" );
		Pattern pCDataClosing = Pattern.compile ( "(.*)\\]\\]></param>\\s*" );
				
		Matcher mSingle = pSingleCData.matcher ( currentLine );
		
		// read CDATA section into result[i]
		if ( mSingle.matches() ) {
			result = mSingle.group(1);
		} else {
			Matcher mCData = pCDataOpen.matcher ( currentLine );
			if ( ! mCData.matches() )
				throw new InvalidCodingException ( "CDATA section expected!" );
			// multi line String!
			StringBuffer buffer = new StringBuffer ( mCData.group ( 1 ) ).append("\n");
			
			do {
				String line = reader.readLine ();
				
				mCData = pCDataClosing.matcher ( line );
				if ( mCData.matches () )
					buffer.append ( mCData.group(1) );
				else
					buffer.append ( line ).append("\n");
			} while ( ! mCData.matches() );
			
			result = buffer.toString();
		}
			
		return result;
	}
	
	
	/**
	 * Deserializes a String into a bunch of java objects if possible.
	 *
	 * @param    s                   a  String
	 *
	 * @return   a Serializable
	 *
	 */
	private Serializable deserializeString (String s) throws IOException, ClassNotFoundException {
		// transform String to byte array
		Base64 base64 = new Base64();
		byte[] abBytes = base64.decode ( s.getBytes() );
		
		// deserialize
		ObjectInputStream ois = new ObjectInputStream ( new ByteArrayInputStream ( abBytes ) );
		
		Serializable result = (Serializable) ois.readObject();
		
		return result;
	}
	
	
	/**
	 * decodes an array of values in the parameter array
	 * the opening tyg <param class=...> has already been read at this point.
	 * This method will read the closing tag </param> after successfully processing
	 * of the values
	 *
	 * @param    className           a  String
	 * @param    count               an int
	 *
	 * @return   an Object
	 *
	 * @exception   InvalidCodingException
	 * @exception   IOException
	 *
	 */
	private Serializable decodeParamArray ( String className, int count ) throws InvalidCodingException, IOException {
		Serializable result = null;;
				
		if ( className.equals ( "String" ) ){
			result = decodeStringArray( count );
		} else if ( className.equals ( "byte" ) ) {
			result = decodeByteArray ( count );
		} else if ( className.equals ( "long" ) ) {
			result = decodeLongArray ( count );
		} else {
			if ( className.equals ( "int" ) ) result = new int[ count ];
			else if ( className.equals ("float" ) ) result = new float[ count ];
			else if ( className.equals ("double" ) ) result = new double[ count ];
			else if ( className.equals ("char" ) ) result = new char[ count ];
			else if ( className.equals ( "Byte" )) result = new Byte[ count ];
			else if ( className.equals ( "Float" )) result = new Float[ count ];
			else if ( className.equals ( "Double" )) result = new Double[ count ];
			else if ( className.equals ( "Character" )) result = new Character[ count ];
			else if ( className.equals ( "Long" ) ) result = new Long[ count ];
			else if ( className.equals ( "Integer" ) ) result = new Integer[ count ];
			else throw new InvalidCodingException ( "Arrays of type " + className + " are not implemented in this protocol!" );
			
			Pattern pElement = Pattern.compile( "\\s*<element>([^>]*)</element>\\s*" );
			for ( int i=0; i<count; i++ ) {
				String line = reader.readLine();
				Matcher m = pElement.matcher( line );
				if ( m.matches() ) {
					if ( className.equals ( "int" ) || className.equals ( "Integer" ) )
						java.lang.reflect.Array.set ( result, i, Integer.valueOf( m.group( 1 ) ) );
					else if ( className.equals ( "float" ) || className.equals ( "Float" ) )
						java.lang.reflect.Array.set ( result, i, Float.valueOf( m.group( 1 ) ) );
					else if ( className.equals ( "char" ) || className.equals ( "Character" ) )
						java.lang.reflect.Array.set ( result, i, m.group( 1 ).charAt(0) );
					else if ( className.equals ( "double" ) || className.equals ( "Double" ) )
						java.lang.reflect.Array.set ( result, i, Double.valueOf( m.group( 1 ) ) );
					else if ( className.equals ( "Long" ) )
						java.lang.reflect.Array.set ( result, i, Long.valueOf( m.group( 1 ) ) );
					else if ( className.equals ( "Byte" ) )
						java.lang.reflect.Array.set ( result, i, Byte.valueOf( m.group( 1 ) ) );
				} else
					throw new InvalidCodingException ( "Not able to match '" + line + " into an array element!" );
			}
		}
		
		String line = reader.readLine();
		while ( line.matches( "\\s*" ))
			line = reader.readLine();
		Matcher mClose = Pattern.compile( "\\s*</param>\\s*" ).matcher ( line );
		
		if ( ! mClose.matches() )
			throw new InvalidCodingException ( "Closing tag </param> expected, but found '" + line + "'!" );
		
		return result;
	}
	
	
	
	
	/**
	 * reads a <i>count</i> string values given by <element> nodes.
	 *
	 * @param    count               an int
	 *
	 * @return   a String[]
	 *
	 */
	private String[] decodeStringArray ( int count ) throws IOException, InvalidCodingException {
		Pattern pFullElement = Pattern.compile ( "\\s*<element><!\\[CDATA\\[(.*)\\]\\]></element>\\s*");
		Pattern pFirst = Pattern.compile ( "\\s*<element><!\\[CDATA\\[(.*)" );
		Pattern pLast  = Pattern.compile ( "(.*)\\]\\]></element>\\s*" );
		String[] result = new String[count];
		
		for ( int i=0; i<count; i++ ) {
			String line = reader.readLine ();
			Matcher m = pFullElement.matcher ( line );
			if ( m.matches() ) {
				result[i] = m.group ( 1 );
			} else {
				m=pFirst.matcher ( line );
				if ( m.matches() ) {
					StringBuffer buffer = (new StringBuffer ( m.group (1 ) )).append ( "\n" );
					
					do {
						line = reader.readLine();
						if ( line == null )
							throw new InvalidCodingException ( "End for node <element> (String Array) not found before message end!" );
						m = pLast.matcher ( line );
						
						if ( m.matches() )
							buffer.append( m.group(1) );
						else
							buffer.append ( line ) .append( "\n" );
					} while ( ! m.matches() );
					result[i] = buffer.toString();
				} else {
					throw new InvalidCodingException ( "Node <element> expected but found '" + line + "'!" );
				}
			}
		}
		
		return result;
	}

	/**
	 * reads a byte array given as cdata
	 *
	 * @param    count               an int
	 *
	 * @return   a String[]
	 *
	 */
	private byte[] decodeByteArray ( int count ) throws IOException, InvalidCodingException {
		// read until first '<'
		char c = ' ';
		do {
			c = (char) reader.read ();
			if ( c != ' ' && c != '\n' && c != '\t' && c != '\r' && c != '<')
				throw new InvalidCodingException ( "Invalid character: " + c + " before CDATA block of byte array parameter!\n");
		} while ( c != '<' );
		
		// read ![CDATA[
		char[] chars = new char[8];
		reader.read ( chars, 0, 8 );
		if ( ! new String ( chars ).equals ( "![CDATA[" ) )
			throw new InvalidCodingException ( "'![CDATA[' expected but got " + new String (chars) + "!" );

		// ok, read the content
		
		chars = new char[count];
		int iRead = 0;
		int iWait = 0;
		
	
		while ( iRead < count ) {
			// "buffered" reading
			int iNow = reader.read(chars, iRead, count-iRead);
			iRead += iNow;
			
			if ( iNow == 0 ) {
				iWait += 200;
				try {
					Thread.sleep ( 200 );
				} catch (InterruptedException e) {}
				if ( iWait > 10000 )
					throw new InvalidCodingException ( "Unable to read " + count + " chars from the input - Timeout!" );
			} else if ( iNow == -1 ) {
				throw new InvalidCodingException ( "Unable to read " + count + " chars from the input - input has ended!" );
			}
		}
		
		String s = new String ( chars );
		
		Base64 base64 = new Base64();
		byte[] result = base64.decode ( s.getBytes() );
		
		// read ]]>
		chars = new char[3];
		reader.read ( chars, 0, 3 );
		if ( ! new String ( chars ) .equals ( "]]>" ) )
			throw new InvalidCodingException ( "]]> expected but got "  + new String ( chars ) + "!" );
		 
		return result;
	}
	
	
	/**
	 * decode a long array
	 *
	 * @param    count               an int
	 *
	 * @return   a long[]
	 *
	 * @exception   IOException
	 * @exception   InvalidCodingException
	 *
	 */
	private long[] decodeLongArray ( int count ) throws IOException, InvalidCodingException {
		
		byte[] bytes = decodeByteArray ( count );

		
		long[] result = new long[ bytes.length / 8 ];
		
		for ( int i=0; i< bytes.length / 8; i++ ) {
			result[i] = bytesToLong ( bytes, i*8 );
		}
				
		return result;
	}
	
	
	
	/**
	 * decode a byte array into a long value
	 * 
	 * @param bytes
	 * @param offset
	 * 
	 * @return
	 */
	private long bytesToLong(byte[] bytes, int offset)  {
		long helper;
		long result = 0;
		int bits = 64;
		for(int i=8; i-- > 0;)  {
			bits -= 8;
			helper = bytes[offset+i];
			helper = helper < 0 ? 256 + helper : helper;
			result |= helper << bits;
		}
		return result;
	}

	/*
	private long bytesTolong(char[] c)  {
		int i;
		long val = 0;
		int bits = 64;
		long tval;
		
		for(i=8; i-- > 0;)  {
			bits -= 8;
			tval = (byte) c[i];
			tval = tval < 0 ? 256 + tval : tval;
			val |= tval << bits;
		}
		return val;
	}*/
}


