package i5.las2peer.nodeAdminConnector.multipart;

import java.io.IOException;

public class MalformedFormDataException extends IOException {

	private static final long serialVersionUID = 1L;

	public MalformedFormDataException(String message) {
		super(message);
	}

	public MalformedFormDataException(Throwable cause) {
		super(cause);
	}

	public MalformedFormDataException(String message, Throwable cause) {
		super(message, cause);
	}

}
