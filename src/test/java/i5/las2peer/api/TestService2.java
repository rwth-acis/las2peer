package i5.las2peer.api;

public class TestService2 extends Service {

	public int usingOther(int i) {
		try {
			Object result = invokeServiceMethod("i5.las2peer.api.TestService", "inc", new Integer(i));

			if (result instanceof Integer)
				return ((Integer) result).intValue();
			else
				return -1;
		} catch (Exception e) {
			e.printStackTrace();
			return -200;
		}
	}
}
