package i5.las2peer.restMapper.data;

import java.util.Comparator;

/**
 * @author Alexander
 */
public class InvocationDataComperator implements Comparator<InvocationData> {
	@Override
	public int compare(InvocationData a, InvocationData b) {
		if (a.getMatchLevel() < b.getMatchLevel())
			return -1;
		else if (a.getMatchLevel() < b.getMatchLevel())
			return 0;
		else
			return 1;
	}
}
