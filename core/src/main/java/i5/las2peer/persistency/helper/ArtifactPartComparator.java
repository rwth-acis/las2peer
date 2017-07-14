package i5.las2peer.persistency.helper;

import java.util.Comparator;

import i5.las2peer.persistency.NetworkArtifact;

/**
 * This class can be used to sort a collection of {@link NetworkArtifact} according to their part index.
 */
public class ArtifactPartComparator implements Comparator<NetworkArtifact> {

	/**
	 * This is a singleton instance that should be used in general.
	 */
	public static final ArtifactPartComparator INSTANCE = new ArtifactPartComparator();

	@Override
	public int compare(NetworkArtifact left, NetworkArtifact right) {
		if (left == null || right == null) {
			throw new NullPointerException();
		}
		int lPart = left.getPartIndex();
		int rPart = right.getPartIndex();
		if (lPart < rPart) {
			return -1;
		} else if (lPart > rPart) {
			return 1;
		}
		return 0;
	}

}
