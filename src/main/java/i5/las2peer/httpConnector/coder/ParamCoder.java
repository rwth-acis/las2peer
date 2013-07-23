package i5.las2peer.httpConnector.coder;


import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;

/**
 * The abstract ParamCode Class defines all methods necessary to code parameters
 * of a message into a java.io.Writer.
 *
 * @author Holger Jan&szlig;en
 */


public abstract class ParamCoder
{
	
	protected Writer out = null;
	
	/**
	 * Standard constructor.
	 * Overriding classes must use this signature!
	 *
	 * @param    out                 a  Writer
	 *
	 * @exception   IOException
	 *
	 */
	public ParamCoder ( Writer out ) throws IOException {
		this.out = out;
	}
			
	/**
	 * Writes an Object as a parameter to the outputstream
	 *
	 * @param    o                   an Object
	 *
	 * @exception   IOException
	 * @exception   ParameterTypeNotImplementedException 	Parameter type is not implemented in this codes
	 *
	 */
	public void write ( Object o ) throws IOException, ParameterTypeNotImplementedException {
		if ( o == null )
			writeNull ();
		else if ( o.getClass().equals( Byte.class ) )
			write ( ((Byte) o).byteValue() );
		else if ( o.getClass().equals( Integer.class ) )
			write ( ((Integer) o).intValue() );
		else if ( o.getClass().equals( Character.class ) )
			write ( ((Character) o).charValue() );
		else if ( o.getClass().equals( Boolean.class ) )
			write ( ((Boolean) o).booleanValue() );
		else if ( o.getClass().equals( Long.class ) )
			write ( ((Long) o).longValue() );
		else if ( o.getClass().equals( Double.class ) )
			write ( ((Double) o).doubleValue() );
		else if ( o.getClass().equals( Float.class ) )
			write ( ((Float) o).floatValue() );
		else if ( o.getClass().equals( String.class ) )
			write ( (String) o);
		else if ( o.getClass().equals( byte[].class ) ) {
			write ( (byte[]) o);
		} else if ( o.getClass().equals( int[].class ) )
			write ( (int[]) o);
		else if ( o.getClass().equals( char[].class ) )
			write ( (char[]) o);
		else if ( o.getClass().equals( boolean[].class ) )
			write ( (boolean[]) o);
		else if ( o.getClass().equals( long[].class ) )
			write ( (long[]) o);
		else if ( o.getClass().equals( double[].class ) )
			write ( (double[]) o);
		else if ( o.getClass().equals( float[].class ) )
			write ( (float[]) o);
		else if ( o.getClass().equals( String[].class ) )
			write ( (String[]) o);
		else if ( o.getClass().equals ( Byte[].class )
					 || o.getClass().equals ( Integer[].class )
					 || o.getClass().equals ( Character[].class )
					 || o.getClass().equals ( Float[].class )
					 || o.getClass().equals ( Double[].class )
					 || o.getClass().equals ( Long[].class ) ) {
			// unwrap arrays of wrapper classes
			writeWrapperArray ( o );
		} else if ( o instanceof Serializable )
			writeSerializable ( (Serializable) o );
		else
			throw new ParameterTypeNotImplementedException( o.getClass().getName() );
	}
	
	

	/**
	 * Writes a coded byte to the outputstream
	 *
	 * @param    b                   a  byte
	 *
	 * @exception   IOException
	 *
	 */
	public abstract void write ( byte b ) throws IOException, ParameterTypeNotImplementedException;
	
	/**
	 * Writes a coded int to the outputstream
	 *
	 * @param    i                   an int
	 *
	 * @exception   IOException
	 *
	 */
	public abstract void write ( int i ) throws IOException, ParameterTypeNotImplementedException;
	
	/**
	 * writes a coded short to the output stream
	 *
	 * @param    s                   a  short
	 *
	 * @exception   IOException
	 * @exception   ParameterTypeNotImplementedException
	 *
	 */
	public abstract void write ( short s ) throws IOException, ParameterTypeNotImplementedException;
	
	/**
	 * Writes a coded char to the outputstream
	 *
	 * @param    c                   a  char
	 *
	 * @exception   IOException
	 *
	 */
	public abstract void write ( char c ) throws IOException, ParameterTypeNotImplementedException;
	
	/**
	 * Writes a coded boolean to the outputstream
	 *
	 * @param    b                   a  boolean
	 *
	 * @exception   IOException
	 *
	 */
	public abstract void write ( boolean b ) throws IOException, ParameterTypeNotImplementedException;
	
	/**
	 * Writes a coded long to the outputstream
	 *
	 * @param    b                   a  long
	 *
	 * @exception   IOException
	 *
	 */
	public abstract void write ( long b ) throws IOException, ParameterTypeNotImplementedException;
	
	/**
	 * Writes a coded double to the outputstream
	 *
	 * @param    b                   a  double
	 *
	 * @exception   IOException
	 *
	 */
	public abstract void write ( double b ) throws IOException, ParameterTypeNotImplementedException;
	
	/**
	 * Writes a coded float to the outputstream
	 *
	 * @param    f                   a  float
	 *
	 * @exception   IOException
	 *
	 */
	public abstract void write ( float f ) throws IOException, ParameterTypeNotImplementedException;
	
	/**
	 * Writes a coded String to the outputstream
	 *
	 * @param    s                   a  String
	 *
	 * @exception   IOException
	 *
	 */
	public abstract void write ( String s ) throws IOException, ParameterTypeNotImplementedException;
	
	/**
	 * Writes a coded byte array to the outputstream
	 *
	 * @param    bytes               a  byte[]
	 *
	 * @exception   IOException
	 *
	 */
	public abstract void write ( byte[] bytes ) throws IOException, ParameterTypeNotImplementedException;

	/**
	 * Writes a coded short array to the outputstream
	 *
	 * @param    shorts               a  short[]
	 *
	 * @exception   IOException
	 *
	 */
	public abstract void write ( short[] shorts ) throws IOException, ParameterTypeNotImplementedException;

	/**
	 * Writes a coded integer array to the outputstream
	 *
	 * @param    integers            an int[]
	 *
	 * @exception   IOException
	 *
	 */
	public abstract void write ( int[] integers ) throws IOException, ParameterTypeNotImplementedException;
	
	/**
	 * Writes a coded char array to the outputstream
	 *
	 * @param    characters          a  char[]
	 *
	 * @exception   IOException
	 *
	 */
	public abstract void write ( char[] characters ) throws IOException, ParameterTypeNotImplementedException;
	
	/**
	 * Writes a coded long array to the outputstream
	 *
	 * @param    longs               a  long[]
	 *
	 * @exception   IOException
	 *
	 */
	public abstract void write ( long[] longs ) throws IOException, ParameterTypeNotImplementedException;

	/**
	 * Writes a coded double array to the outputstream
	 *
	 * @param    doubles             a  double[]
	 *
	 * @exception   IOException
	 *
	 */
	public abstract void write ( double [] doubles ) throws IOException, ParameterTypeNotImplementedException;
	
	/**
	 * Writes a coded float array to the outputstream
	 *
	 * @param    floats              a  float[]
	 *
	 * @exception   IOException
	 *
	 */
	public abstract void write ( float[] floats ) throws IOException, ParameterTypeNotImplementedException;
	
	/**
	 * Writes a coded String array to the outputstream
	 *
	 * @param    strings              a  String[]
	 *
	 * @exception   IOException
	 *
	 */
	public abstract void write ( String[] strings ) throws IOException, ParameterTypeNotImplementedException;
	
	
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
	public abstract void writeWrapperArray ( Object o ) throws IOException, ParameterTypeNotImplementedException;
	
	/**
	 * Writes a coded Serializable object stream as byte array to the outputstream
	 *
	 * @param    o                   a  Serializable
	 *
	 * @exception   IOException
	 * @exception   ParameterTypeNotImplementedException
	 *
	 */
	public abstract void writeSerializable ( Serializable o ) throws IOException, ParameterTypeNotImplementedException;
	
	/**
	 * Writes a null value as coded parameter to the output stream
	 *
	 * @exception   IOException
	 * @exception   ParameterTypeNotImplementedException
	 *
	 */
	public abstract void writeNull () throws IOException, ParameterTypeNotImplementedException;
	
	/**
	 * Method called by the using class to start the encoding
	 *
	 * @exception   IOException
	 *
	 */
	public abstract void header (int count ) throws IOException, ParameterTypeNotImplementedException;
	
	/**
	 * Method called by the using class to finish the coding
	 *
	 * @exception   IOException
	 *
	 */
	public abstract void footer () throws IOException, ParameterTypeNotImplementedException;
	
	
	/**
	 * unwrap an array of a wrapper class and transform it into an array of the
	 * native class
	 *
	 * @param    arr                 an Object
	 *
	 * @return   array of a native class
	 *
	 * @exception IllegalArgumentException 	given array is not an array of a wrapper class
	 *
	 */
	public static Object unwrapArray ( Object arr ) {
		Object result;
		
		if ( arr.getClass().equals ( Byte[].class ) ) {
			result = new byte[ java.lang.reflect.Array.getLength(arr) ];
		} else if ( arr.getClass().equals ( Integer[].class ) ) {
			result = new int[ java.lang.reflect.Array.getLength(arr)];
		} else if ( arr.getClass().equals ( Long[].class ) ) {
			result = new long [ java.lang.reflect.Array.getLength(arr) ];
		} else if ( arr.getClass().equals ( Character[].class ) ) {
			result = new char [ java.lang.reflect.Array.getLength(arr) ];
		} else if ( arr.getClass().equals ( Float[].class ) ) {
			result = new float [ java.lang.reflect.Array.getLength(arr) ];
		} else if ( arr.getClass().equals ( Double[].class ) ) {
			result = new double [ java.lang.reflect.Array.getLength(arr) ];
		} else
			throw new IllegalArgumentException ( "Given parameter is not an array of a wrapper class!" );
		
		for ( int i=0; i<java.lang.reflect.Array.getLength(arr); i++ )
			java.lang.reflect.Array.set ( result, i, java.lang.reflect.Array.get( arr, i ) );
		
		return result;
	}
	
}

