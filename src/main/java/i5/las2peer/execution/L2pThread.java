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

	private Context context;
	private RMITask task;
	private ServiceAgent agent;

	private Serializable result = null;
	private Exception exception = null;

	private boolean bFinished = false;

	/**
	 * create a new L2pThread
	 * 
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
		if (!isFinished())
			throw new NotFinishedException("Job not Finished yet");

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
	 * TODO: question: what's more secure? Give access to the service Agent or provide an invokeInternally method here?
	 * - Probably the latter one.
	 * 
	 * @return the ServiceAgent responsible for the service requested via the invocation task
	 */
	public ServiceAgent getServiceAgent() {
		return agent;
	}

}
