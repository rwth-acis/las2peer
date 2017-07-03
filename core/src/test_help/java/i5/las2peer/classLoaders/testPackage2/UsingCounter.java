package i5.las2peer.classLoaders.testPackage2;

import java.util.ResourceBundle;

import i5.las2peer.classLoaders.testPackage1.CounterClass;

public class UsingCounter {

	public static int countCalls() {
		CounterClass.inc();
		return CounterClass.getCounter();
	}

	public static int getUsedVersion() {
		return CounterClass.getVersion();
	}

	public static int getPropertyValue() {
		ResourceBundle rb = ResourceBundle.getBundle("i5.las2peer.classLoaders.testPackage1.test");
		return Integer.valueOf(rb.getString("integer"));
	}
}
