package i5.las2peer.tools;

import static org.junit.Assert.*;

import org.junit.Test;

public class SimpleToolsTest {

	@Test
	public void testJoin() {		
		assertEquals ( "", SimpleTools.join ( null, "abc"));
		assertEquals ( "", SimpleTools.join( new Object[0], "dkefde"));
		
		
		assertEquals ( "a, b, c", SimpleTools.join( new Object[]{"a",  'b', "c"}, ", " ));
		
		assertEquals ("10.20.30", SimpleTools.join( new Integer[]{10,20,30}, ".") );
	}
	
	@Test
	public void testRepeat () {
		assertEquals ( "", SimpleTools.repeat( "", 11));
		assertEquals ( "", SimpleTools.repeat( "adwdw", 0));
		assertEquals ( "", SimpleTools.repeat( "adwdw", -10));
		
		assertNull ( SimpleTools.repeat( null,  100 ));
		
		assertEquals ( "xxxx", SimpleTools.repeat("x",  4));
		
		assertEquals ( "101010", SimpleTools.repeat(10,  3));
	}

}
