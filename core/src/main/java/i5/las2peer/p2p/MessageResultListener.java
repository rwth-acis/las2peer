package i5.las2peer.p2p;

import java.util.Date;
import java.util.Vector;

import i5.las2peer.communication.Message;

/**
 * A MessageResultListener is a simple collector for one result of a message sending operation in a {@link Node}. The
 * listener will be called by the node, when the result of the message or any exception in between has arrived. Since a
 * message may be delivered as multi- or broadcast, multiple results (and exceptions) are possible.
 * 
 * The original sending part may do something else while waiting for the message result or use the
 * {@link #waitForAllAnswers()} or {@link #waitForOneAnswer} methods.
 * 
 * The <i>notify...</i> hook may be used to react directly to events concerning this listener, e.g. in a closure like
 * manner.
 * 
 * 
 *
 */
public class MessageResultListener {

	/**
	 * a simple message status flag
	 * 
	 * 
	 *
	 */
	public enum Status {
		WAITING,
		SUCCESS,
		EXCEPTION,
		TIMEOUT
	}

	// private int randomId = new Random().nextInt();

	private Status status = Status.WAITING;
	private Vector<Exception> exceptions = new Vector<>();
	private Vector<Message> answers = new Vector<>();

	private int numberOfRecipients = 1;

	private long startedAt;

	/**
	 * the timeout for messages
	 */
	private long timeoutMs;

	/**
	 * timeout after the first result was received
	 */
	private long timeoutMoreMs = 100;

	private boolean inWaitMethod = false;

	/**
	 * simple constructor
	 * 
	 * @param timeoutMs A timeout for the result
	 */
	public MessageResultListener(long timeoutMs) {
		startedAt = new Date().getTime();
		this.timeoutMs = timeoutMs;
	}

	/**
	 * simple constructor
	 * 
	 * @param timeoutMs A timeout for the result
	 * @param timeoutMoreMs A timeout after first result
	 */
	public MessageResultListener(long timeoutMs, long timeoutMoreMs) {
		startedAt = new Date().getTime();
		this.timeoutMs = timeoutMs;
		this.timeoutMoreMs = timeoutMoreMs;
	}

	/**
	 * simple constructor
	 * 
	 * @param timeoutMs A timeout for the result
	 */
	public MessageResultListener(int timeoutMs) {
		this((long) timeoutMs);
	}

	/**
	 * increase the number of recipients
	 */
	public void addRecipient() {
		addRecipients(1);
	}

	/**
	 * increase the number of recipients
	 * 
	 * @param number Amount of recipients to add
	 */
	public void addRecipients(int number) {
		synchronized (this) {
			numberOfRecipients += number;
		}
	}

	/**
	 * set an exception that has been thrown during delivery or processing of the message
	 * 
	 * will cause a notifyException call
	 * 
	 * @param e An exception to collect
	 */
	public void collectException(Exception e) {
		synchronized (this) {
			if (status == Status.WAITING) {
				status = Status.EXCEPTION;
			}
			exceptions.add(e);

			notifyException(e);
			notifyAll();
		}
	}

	/**
	 * set the result of the message, will cause a notifySuccess() call
	 * 
	 * @param answer A message to collect as answer
	 */
	public synchronized void collectAnswer(Message answer) {
		synchronized (this) {
			answers.addElement(answer);

			if (status == Status.WAITING) {
				status = Status.SUCCESS;
			}

			notifySuccess();

			notifyAll();
		}
	}

	/**
	 * @return true, if the message processing is finished
	 */
	public synchronized boolean isFinished() {
		synchronized (this) {
			return (numberOfRecipients <= answers.size() + exceptions.size()) || status == Status.TIMEOUT;
		}
	}

	/**
	 * get all exceptions which have been registered so far
	 * 
	 * @return array with all exception registered up to this moment
	 */
	public Exception[] getExceptions() {
		return exceptions.toArray(new Exception[0]);
	}

	/**
	 * @return array with all collected results
	 */
	public Message[] getResults() {
		return answers.toArray(new Message[0]);
	}

	/**
	 * @return true, if the message resulted in a success
	 */
	public boolean isSuccess() {
		return status == Status.SUCCESS;
	}

	/**
	 * @return true, if the message timed out before delivery
	 */
	public boolean isTimedOut() {
		return status == Status.TIMEOUT;
	}

	/**
	 * get the age in milliseconds of this listener
	 * 
	 * @return age in milliseconds
	 */
	public long getAgeMs() {
		return new Date().getTime() - startedAt;
	}

	/**
	 * was their any exception during sending or processing the corresponding message
	 * 
	 * @return true, if any exception occurred
	 */
	public boolean hasException() {
		return exceptions.size() > 0;
	}

	/**
	 * @return number of successfully collected results
	 */
	public int getNumberOfResults() {
		return answers.size();
	}

	/**
	 * @return the number of expected results
	 */
	public int getNumberOfExpectedResults() {
		return this.numberOfRecipients;
	}

	/**
	 * default non blocking timer for waiting
	 */
	public static final int DEFAULT_TIMER = 500; // 500 ms

	/**
	 * sleep until a result has been received
	 * 
	 * @throws InterruptedException If waiting was interrupted
	 */
	public void waitForOneAnswer() throws InterruptedException {
		waitForOneAnswer(DEFAULT_TIMER);
	}

	/**
	 * sleep until a result has been received.
	 * 
	 * @param sleepTimeMs how long to sleep between checks
	 * @throws InterruptedException If waiting was interrupted
	 */
	public void waitForOneAnswer(int sleepTimeMs) throws InterruptedException {
		synchronized (this) {
			inWaitMethod = true;
			try {
				long startedAt = new Date().getTime();
				long now = startedAt;
				while ((now - startedAt) < timeoutMs && exceptions.isEmpty() && answers.isEmpty()) {
					wait(timeoutMs - (now - startedAt));
					now = new Date().getTime();
				}
			} finally {
				if (exceptions.isEmpty() && answers.isEmpty()) {
					status = Status.TIMEOUT;
					notifyTimeout();
				}
				inWaitMethod = false;
			}
		}
	}

	/**
	 * wait for all expected results
	 * 
	 * @throws InterruptedException If waiting was interrupted
	 */
	public void waitForAllAnswers() throws InterruptedException {
		waitForAllAnswers(true);
	}

	/**
	 * 
	 * @param waitForAll waits for all results if false, otherwise
	 * @throws InterruptedException If waiting was interrupted
	 */
	public void waitForAllAnswers(boolean waitForAll) throws InterruptedException {
		synchronized (this) {
			inWaitMethod = true;
			try {
				long startedAt = new Date().getTime();
				long now = startedAt;
				while ((now - startedAt) < timeoutMs
						&& (waitForAll || answers.isEmpty() || (now - startedAt) < timeoutMoreMs || !isFinished())) {
					wait(timeoutMs - (now - startedAt));
					now = new Date().getTime();
				}
			} finally {
				if (exceptions.isEmpty() && answers.isEmpty()) {
					status = Status.TIMEOUT;
					notifyTimeout();
				}
				inWaitMethod = false;
			}
		}
	}

	/**
	 * check if this method is times out, call the corresponding notification hook, if the timeout is detected here
	 * 
	 * @return true, the the message is times out
	 */
	public boolean checkTimeOut() {
		synchronized (this) {
			if (inWaitMethod || isFinished()) {
				return false;
			}
			if (isTimedOut()) {
				return true;
			}

			if (new Date().getTime() - startedAt > timeoutMs) {
				status = Status.TIMEOUT;
				notifyTimeout();

				return true;
			} else {
				return false;
			}
		}
	}

	/************** hooks for listeners ****************************/

	/**
	 * NotifiedSuccess will be called, when a result has arrived and is stored in this listener.
	 * 
	 * This hook may be overwritten at listener usage or generation e.g. for decoupled implementation of message success
	 * observation.
	 * 
	 * Also useful in a closure like generation of a Listener instance
	 * 
	 */
	public void notifySuccess() {
	}

	/**
	 * notifyException will be called, when an exception has arrived and is stored in this listener.
	 * 
	 * This hook may be overwritten at listener usage or generation e.g. for decoupled implementation of message success
	 * observation.
	 * 
	 * Also useful in a closure like generation of a Listener instance
	 * 
	 * @param exception An exception that was received
	 */
	public void notifyException(Exception exception) {
	}

	/**
	 * notifyTimeout will be called, if the underlying network has decided, that the corresponding message has timed out
	 * and passed a corresponding exception to this listener.
	 * 
	 * This hook may be overwritten at listener usage or generation e.g. for decoupled implementation of message success
	 * observation.
	 * 
	 * Also useful in a closure like generation of a Listener instance
	 * 
	 */
	public void notifyTimeout() {
	}

}
