package i5.las2peer.persistency;

import rice.p2p.commonapi.Id;
import rice.p2p.past.ContentHashPastContent;
import rice.p2p.past.PastContent;
import rice.p2p.past.PastException;

public abstract class AbstractArtifact extends ContentHashPastContent {

	public static final int MAX_SIZE = 500 * 1000; // = 500 KB

	private static final long serialVersionUID = 1L;

	// TODO test different content types, String seems the fastest, also check BASE64, Serializable, byte[]
	protected final byte[] content;

	protected AbstractArtifact(Id id, byte[] content) {
		super(id);
		this.content = content;
	}

	public byte[] getContent() throws VerificationFailedException {
		verify();
		return content;
	}

	public void verify() throws VerificationFailedException {
		if (content == null) {
			return;
		}
		final int size = content.length;
		if (size > MAX_SIZE) {
			throw new VerificationFailedException(
					"Given content has " + size + " bytes and is too big for maximum size " + MAX_SIZE);
		}
	}

	@Override
	public PastContent checkInsert(Id id, PastContent existingContent) throws PastException {
		try {
			this.verify();
		} catch (VerificationFailedException e) {
			throw new PastException(e.toString());
		}
		return super.checkInsert(id, existingContent);
	}

	@Override
	public String toString() {
		return getId().toStringFull();
	}

}
