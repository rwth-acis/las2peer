package i5.las2peer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import rice.environment.Environment;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.IdFactory;
import rice.pastry.commonapi.PastryIdFactory;

public class GeneralTests {

	@Test
	public void test() {

		String testString = "siouefiuhghd rghdriguh";

		Environment envA = new Environment();
		IdFactory idfA = new PastryIdFactory(envA);
		Id a = idfA.buildId(testString);

		System.out.println("ID: " + a);

		for (int i = 0; i < 50; i++) {
			Environment envB = new Environment();
			IdFactory idfB = new PastryIdFactory(envB);

			Id b = idfB.buildId(testString);

			assertEquals(a, b);
		}

	}

	@Test
	public void stringTest() {

		String testee = "zeile1\nund2\nund 3";

		String transformed = testee.replaceAll("\\n", "\t\n");

		assertEquals("zeile1\t\nund2\t\nund 3", transformed);
	}

}
