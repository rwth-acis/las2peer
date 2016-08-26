package i5.las2peer.api;

import i5.las2peer.persistency.Envelope;

/**
 * This interface can be used to implement a handler that receives successfully fetched envelopes from the network.
 */
public interface StorageEnvelopeHandler {

	/**
	 * This method is called if the envelope was successfully fetched from the network.
	 *
	 * @param result The envelope that was retrieved from the network.
	 */
	public void onEnvelopeReceived(Envelope result);

}
