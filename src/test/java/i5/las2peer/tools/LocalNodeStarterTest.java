package i5.las2peer.tools;

import static org.junit.Assert.*;

import org.junit.Test;

public class LocalNodeStarterTest {

	@Test
	public void test() {
		String pack = "bla.xxx.awedw";
		String cls = "daiwdw";
		
		String complete = pack + "." + cls;
		
		assertEquals ( pack, complete.substring(0, complete.lastIndexOf('.')) );
		assertEquals ( cls, complete.substring(complete.lastIndexOf('.')+1) );
	}

}
