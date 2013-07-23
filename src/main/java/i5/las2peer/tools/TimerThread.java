package i5.las2peer.tools;

/**
 * A simple thread calling the worker method {@link #waitTimeMs} periodically.
 *  
 * @author Holger Jan&szlig;en
 *
 */
public abstract class TimerThread extends Thread {
	
	private long waitTimeMs = 10000; // 10sec
	
	private boolean running = false;
	private boolean stopped = false;

	public abstract void tick();
	
	
	/**
	 * create a new PeriodicalTimer calling {@link #tick} every 10 seconds
	 */
	public TimerThread () {
		setDaemon (true );
	}
	
	/**
	 * create a new PeriodicalTimer calling {@link #tick} after each interval of the given time
	 * 
	 * @param waitTimeMs
	 */
	public TimerThread ( long waitTimeMs ) {
		this();
		this.waitTimeMs = waitTimeMs;
	}

	/**
	 * the actual work is done in the {@link #tick} method
	 */
	@Override
	public void run () {
		synchronized ( this ) {
			try {
				running = true;
			
				do {
					try {
						wait( waitTimeMs );
						tick();
					} catch (InterruptedException e) {
						stopped = true;
					}
					
				} while ( ! stopped );
				
				running = false;
			} finally {
				running = false;
			}
		}		
			
	}
	
	
	/**
	 * stop the timer thread
	 */
	public void stopTimer() {
		synchronized ( this ) {
			stopped = true;
			
			this.notifyAll();
		}
	}
	
	/**
	 * has the timer been started
	 * @return true, if the run method is currently active
	 */
	public boolean isRunning () {
		return running;
	}
	
	/**
	 * is the timer finished, i.e. has it been stopped
	 * @return true, if the run method was stopped with the {@link #stopTimer} method
	 */
	public boolean isStopped () {
		return stopped;
	}
	

}
