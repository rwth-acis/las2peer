package i5.las2peer.classLoaders.testPackage1;

public class CounterClass {

	private static int counter = 0;
	
	private static int version = 200;
	
	
	public static void inc () { counter --; }
	
	public static int getCounter () { return counter; }
	
	public static int getVersion () { return version; }
	
}
