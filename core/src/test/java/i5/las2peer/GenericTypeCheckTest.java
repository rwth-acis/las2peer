package i5.las2peer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GenericTypeCheckTest {

	private class NestedGeneric<T> {

		private Class<T> type;

		public NestedGeneric(Class<T> cls) {
			type = cls;
		}

		public boolean checkType(Object arg) {
			return type.isInstance(arg);
		}
	}

	@Test
	public void test() {
		NestedGeneric<String> testee = new NestedGeneric<String>(String.class);

		assertTrue(testee.checkType("hello"));
		assertFalse(testee.checkType(1));

	}

}
