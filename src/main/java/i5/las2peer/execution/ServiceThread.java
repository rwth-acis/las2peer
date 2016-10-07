package i5.las2peer.execution;


public class ServiceThread extends Thread {

	private final ExecutionContext context;

	public ServiceThread(ExecutionContext context, Runnable r) {
		super(r);
		this.context = context;
	}

	public ExecutionContext getContext() {
		return this.context;
	}

	public static ExecutionContext getCurrentContext() {
		Thread t = Thread.currentThread();

		if (!(t instanceof ServiceThread)) {
			throw new IllegalStateException("Not executed in a ServiceThread environment!");
		}

		return ((ServiceThread) t).getContext();
	}

}
