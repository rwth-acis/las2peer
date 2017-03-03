package i5.las2peer.communication;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import i5.las2peer.serialization.SerializationException;
import i5.las2peer.serialization.SerializeTools;

public class ListMethodsContentTest {

	@Test
	public void testSerialization() throws SerializationException {

		ListMethodsContent testee = new ListMethodsContent(false);
		testee.addMethod(ListMethodsContent.class.getMethods()[0]);
		testee.finalize();

		String ser = SerializeTools.serializeToBase64(testee);

		ListMethodsContent andBack = (ListMethodsContent) SerializeTools.deserializeBase64(ser);

		assertEquals(testee.getSortedMethodNames()[0], andBack.getSortedMethodNames()[0]);
		assertEquals(testee.getSortedMethodNames().length, andBack.getSortedMethodNames().length);

		assertEquals(testee.toXmlString(), andBack.toXmlString());
	}

}