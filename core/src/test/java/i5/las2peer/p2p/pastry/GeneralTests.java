package i5.las2peer.p2p.pastry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import rice.environment.Environment;
import rice.p2p.scribe.Topic;
import rice.pastry.commonapi.PastryIdFactory;

public class GeneralTests {

	@Test
	public void testTopicIds() {
		Environment e = new Environment();
		Topic t = new Topic(new PastryIdFactory(e), "test-12345");
		Topic t2 = new Topic(new PastryIdFactory(e), "test-12345");

		assertEquals(t, t2);
		assertTrue(t.equals(t2));
		assertTrue(t2.equals(t));

		Topic t3 = new Topic(new PastryIdFactory(e), "test-1234");

		assertFalse(t.equals(t3));
	}

}
