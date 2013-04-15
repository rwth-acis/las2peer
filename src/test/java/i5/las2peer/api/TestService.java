package i5.las2peer.api;

import java.util.Hashtable;

/**
 * a simple service for testing (R)MI and service start
 * 
 * @author Holger Janssen
 * @version $Revision: 1.3 $, $Date: 2013/02/21 12:18:21 $
 *
 */
public class TestService extends Service {

	private int testInt1 = -1;
	private String testString1 = "test1";
	
	protected String testString2 = "test2";
	protected int testInt2 = -10;
	
	
	public Integer getInt () {
		return 10;
	}
	
	public Integer inc ( int i ) {
		return i+1;
	}
	
	public Integer inc ( Integer i ) {
		return i+2;
	}
	
	
	public String subclass ( Exception e )  {
		return e.getMessage();
	}
	
	protected void protectedMethod () {}
	
	@SuppressWarnings("unused")
	private void privateMethod () {}
	
	
	public Hashtable<String, String> getProps () {
		setFieldValues();
		return getProperties();
	}
	
	public static void staticMethod() {}
	
	
	public int getTestInt1 () { return testInt1; } 
	public int getTestInt2 () { return testInt2; } 
	public String getTestString1 () { return testString1; } 
	public String getTestString2 () { return testString2; } 
	
}
