package i5.las2peer.execution;

import java.util.concurrent.ThreadFactory;

public class ServiceThreadFactory implements ThreadFactory {

	ExecutionContext context;

	public ServiceThreadFactory(ExecutionContext context) {
		this.context = context;
	}

	@Override
	public Thread newThread(Runnable r) {
		return new ServiceThread(context, r);
	}

}
