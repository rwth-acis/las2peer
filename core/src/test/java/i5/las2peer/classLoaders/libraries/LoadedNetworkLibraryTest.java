package i5.las2peer.classLoaders.libraries;

import org.junit.Assert;
import org.junit.Test;

public class LoadedNetworkLibraryTest {

	@Test
	public void testXMLandBack() {
		try {
			LibraryIdentifier libId = new LibraryIdentifier("testlib", "4.2.1");
			LoadedNetworkLibrary noDeps = new LoadedNetworkLibrary(null, libId, null);
			String xml = noDeps.toXmlString();
			LoadedNetworkLibrary backNoDeps = LoadedNetworkLibrary.createFromXml(null, xml);
			Assert.assertEquals(noDeps.getIdentifier().toString(), backNoDeps.getIdentifier().toString());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
