package i5.las2peer.persistency;

import java.io.Serializable;

public class MetadataEnvelope implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String envelopeIdentifier;
	private final long envelopeVersion;
	private final int numOfEnvelopeParts;

	public MetadataEnvelope(String envelopeIdentifier, long envelopeVersion, int numOfEnvelopeParts) {
		this.envelopeIdentifier = envelopeIdentifier;
		this.envelopeVersion = envelopeVersion;
		this.numOfEnvelopeParts = numOfEnvelopeParts;
	}

	public String getEnvelopeIdentifier() {
		return envelopeIdentifier;
	}

	public long getEnvelopeVersion() {
		return envelopeVersion;
	}

	public int getEnvelopeNumOfParts() {
		return numOfEnvelopeParts;
	}

	@Override
	public String toString() {
		return MetadataArtifact.getMetadataIdentifier(envelopeIdentifier, envelopeVersion);
	}

}
