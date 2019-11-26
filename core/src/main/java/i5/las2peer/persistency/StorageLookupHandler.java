package i5.las2peer.persistency;

import java.util.ArrayList;

import rice.p2p.past.PastContentHandle;

public interface StorageLookupHandler {

	public void onLookup(ArrayList<PastContentHandle> handles);

}
