package i5.las2peer.p2p;

public class ServiceNotFoundException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ServiceNotFoundException(String message, Exception reason) {
		super(message, reason);
	}

}
