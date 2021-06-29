package i5.las2peer.registry.data;

import i5.las2peer.registry.Util;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.serialization.SerializeTools;

import java.security.PublicKey;

// do we really need this?
/**
 * Contains user data exactly as stored in blockchain.
 *
 * In the getters the fields will be converted to useful values.
 */
public class GroupData {
	private byte[] name;
	private byte[] agentId;
	private byte[] publicKey;
	private String ownerAddress;

	public GroupData(byte[] name, byte[] agentId, byte[] publicKey, String ownerAddress) {
		this.name = name;
		this.agentId = agentId;
		this.publicKey = publicKey;
		this.ownerAddress = ownerAddress;
	}

	public String getName() {
		return Util.recoverString(this.name);
	}

	public String getAgentId() {
		return Util.recoverString(this.agentId);
	}

	public String getOwnerAddress() {
		return this.ownerAddress;
	}

	/** serialized (or otherwise encoded) form of public key */
	public byte[] getRawPublicKey() {
		return this.publicKey;
	}

	public PublicKey getPublicKey() throws SerializationException {
		return (PublicKey) SerializeTools.deserialize(this.publicKey);
	}

	@Override
	public String toString() {
		try {
			return "GroupData(name: " + this.getName() + ", agent ID: " + this.getAgentId() + ", owner address: " + this.getOwnerAddress() + ", pubKey: " + this.getPublicKey().toString() + ")";
		} catch (SerializationException e) {
			return "GroupData(name: " + this.getName() + ", agent ID: " + this.getAgentId() + ", owner address: " + this.getOwnerAddress() + ", pubKey: [unreadable! this isn't good!])";
		}
	}
}
