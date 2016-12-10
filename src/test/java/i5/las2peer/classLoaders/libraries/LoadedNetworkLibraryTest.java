package i5.las2peer.classLoaders.libraries;

import org.junit.Assert;
import org.junit.Test;

import i5.las2peer.classLoaders.helpers.LibraryDependency;
import i5.las2peer.classLoaders.helpers.LibraryIdentifier;

public class LoadedNetworkLibraryTest {

	@Test
	public void testXMLandBack() {
		try {
			LibraryIdentifier libId = new LibraryIdentifier("testlib", "4.2.1");
			LibraryDependency dep1 = new LibraryDependency("dep1", "1.0.0");
			LibraryDependency dep2 = new LibraryDependency("dep2", "2.0.0");
			LoadedNetworkLibrary noDeps = new LoadedNetworkLibrary(null, libId, null);
			String xml = noDeps.toXmlString();
			LoadedNetworkLibrary backNoDeps = LoadedNetworkLibrary.createFromXml(null, xml);
			Assert.assertEquals(noDeps.getIdentifier().toString(), backNoDeps.getIdentifier().toString());
			Assert.assertNull(backNoDeps.getDependencies());
			LoadedNetworkLibrary twoDeps = new LoadedNetworkLibrary(null, libId,
					new LibraryDependency[] { dep1, dep2 });
			xml = twoDeps.toXmlString();
			LoadedNetworkLibrary backTwoDeps = LoadedNetworkLibrary.createFromXml(null, xml);
			Assert.assertEquals(twoDeps.getIdentifier().toString(), backTwoDeps.getIdentifier().toString());
			Assert.assertEquals(twoDeps.getDependencies().length, backTwoDeps.getDependencies().length);
			for (int c = 0; c < twoDeps.getDependencies().length; c++) {
				Assert.assertEquals(twoDeps.getDependencies()[c].toString(),
						backTwoDeps.getDependencies()[c].toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
