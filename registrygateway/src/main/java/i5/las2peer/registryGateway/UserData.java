package i5.las2peer.registryGateway;

class UserData {
	private byte[] name;
	private byte[] agentId;
	private String ownerAddress;
	private byte[] dhtSupplement;

	public UserData(byte[] name, byte[] agentId, String ownerAddress, byte[] dhtSupplement) {
		this.name = name;
		this.agentId = agentId;
		this.ownerAddress = ownerAddress;
		this.dhtSupplement = dhtSupplement;
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

	public String getDhtSupplement() {
		return Util.recoverString(this.dhtSupplement);
	}

	@Override
	public String toString() {
		return "UserData(name: " + this.getName() + ", agent ID: " + this.getAgentId() + ", owner address: " + this.getOwnerAddress() + ", dht supplement: " + this.getDhtSupplement() + ")";
	}
}
