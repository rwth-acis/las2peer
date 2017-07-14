package i5.las2peer.persistency;

import java.util.ArrayList;

public interface StoragePartsHandler {

	public void onPartsReceived(ArrayList<NetworkArtifact> parts);

}
