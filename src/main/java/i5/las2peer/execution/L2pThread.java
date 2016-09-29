package i5.las2peer.execution;

import java.io.Serializable;

import i5.las2peer.security.Context;
import i5.las2peer.security.ServiceAgent;

/**
 * a L2pThread is responsible for running a single {@link RMITask} inside a {@link i5.las2peer.p2p.Node}
 * 
 * 
 *
 */
public class L2pThread extends Thread {

	final private Context context;
	final private RMITask task;
	final private ServiceAgent agent;

	private Serializable result = null;
	private Exception exception = null;

	private boolean bFinished = false;

	/**
	 * Gets the current las2peer context.
	 * 
	 * @throws IllegalStateException called not in a las2peer execution thread
	 * @return the current context
	 */
	public static L2pThread getCurrent() {
		Thread t = Thread.currentThread();

		if (!(t instanceof L2pThread)) {
			throw new IllegalStateException("Not executed in a L2pThread environment!");
		}

		return (L2pThread) t;
	}

	/**
	 * Returns the class loader used by the service executed in this thread.
	 * 
	 * @return Returns the class loader instance.
	 */
	public static ClassLoader getServiceClassLoader() {
		return getCurrent().agent.getServiceInstance().getClass().getClassLoader();
	}

	/**
	 * create a new L2pThread
	 * 
	 * @param agent
	 * @param task
	 * @param context
	 */
	public L2pThread(ServiceAgent agent, RMITask task, Context context) {
		this.agent = agent;
		this.task = task;
		this.context = context;
	}

	/**
	 * get the task to be executed
	 * 
	 * @return the corresponding invocation task
	 */
	public RMITask getTask() {
		return task;
	}

	/**
	 * get the context in which this task has to be executed
	 * 
	 * @return the security context this task is running in
	 */
	public Context getContext() {
		return context;
	}

	/**
	 * the actual work
	 */
	@Override
	public void run() {
		try {
			result = agent.handle(task);
			bFinished = true;
		} catch (Exception e) {
			exception = e;
		}
	}

	/**
	 * is the execution of this task finished (either successfully or with an exception)
	 * 
	 * @return true, if this task (thread) is finished
	 */
	public boolean isFinished() {
		return bFinished || hasException();
	}

	/**
	 * is the execution of this task finished successfully?
	 * 
	 * @return true, if this task is finished successfully (i.e. without any exception)
	 */
	public boolean isSuccess() {
		return bFinished && !hasException();
	}

	/**
	 * did the execution result in an exception?
	 * 
	 * @return true, if any exception occurred while execution
	 */
	public boolean hasException() {
		return exception != null;
	}

	/**
	 * get the result of the execution
	 * 
	 * @return the result of the method invocation
	 * @throws NotFinishedException
	 */
	public Serializable getResult() throws NotFinishedException {
		if (!isFinished()) {
			throw new NotFinishedException("Job not Finished yet");
		}
		return result;
	}

	/**
	 * get a possibly thrown exception
	 * 
	 * @return a (possibly) occurred exception
	 */
	public Exception getException() {
		return exception;
	}

	/**
	 * access to the agent registered at the L2pNode
	 * 
	 * @return the ServiceAgent responsible for the service requested via the invocation task
	 */
	public ServiceAgent getServiceAgent() {
		return agent;
	}

}
