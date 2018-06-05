package i5.las2peer.logging.bot;

public enum BotStatus {
	DISABLED(0),
	READY(1),
	RUNNING(2),
	TRAINING(3),
	BUSY(4);

	private int status;

	public int getStatusCode() {
		return status;
	}

	BotStatus(int status) {
		this.status = status;
	}

}
