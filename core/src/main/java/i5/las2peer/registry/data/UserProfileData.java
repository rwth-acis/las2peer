package i5.las2peer.registry.data;

import i5.las2peer.registry.Util;

import java.math.BigInteger;

/**
 * Contains reputation transaction data exactly as stored in blockchain.
 */
public class UserProfileData {
	private String owner;
	private byte[] userName;
	private BigInteger cumulativeScore;
	private BigInteger noTransactions;

	public UserProfileData(String owner, byte[] userName, BigInteger cumulativeScore, BigInteger noTransactions) {
		this.owner = owner;
		this.userName = userName;
		this.cumulativeScore = cumulativeScore;
		this.noTransactions = noTransactions;
	}

	public String getOwner() {
		return Util.recoverString(this.owner);
	}

	public String getuserName() {
		return Util.recoverString(this.userName);
	}

	public BigInteger getNoTransactions() {
		return this.noTransactions;
	}

	public BigInteger getCumulativeScore() {
		return this.cumulativeScore;
	}

	@Override
	public String toString() {
		return "UserProfileData("+
			"owner: " + this.getOwner() + 
			", userName: " + this.getuserName() + 
			", cumulativeScore: " + this.getCumulativeScore().toString() + 
			", noTransactions: " + this.getNoTransactions().toString() +
		")";
	}
}
