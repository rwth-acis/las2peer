package i5.las2peer.persistency;

import java.io.Serializable;

public interface StorageArtifactHandler {

	public <T extends Serializable> void onReceive(AbstractArtifact artifact);

}
