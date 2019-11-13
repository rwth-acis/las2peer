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
	private BigInteger noTransactionsSent;
	private BigInteger noTransactionsRcvd;
	private BigInteger profileIndex; 

	public UserProfileData(
		String owner, 
		byte[] userName, 
		BigInteger cumulativeScore, 
		BigInteger noTransactionsSent, 
		BigInteger noTransactionsReceived,
		BigInteger profileIndex
	) {
		this.owner = owner;
		this.userName = userName;
		this.cumulativeScore = cumulativeScore;
		this.noTransactionsSent = noTransactionsSent;
		this.noTransactionsRcvd = noTransactionsReceived;
		this.profileIndex = profileIndex;
	}

	public String getOwner() {
		return this.owner;
	}

	public String getUserName() {
		return Util.recoverString(this.userName);
	}

	public BigInteger getNoTransactionsSent() {
		return this.noTransactionsSent;
	}

	public BigInteger getNoTransactionsRcvd() {
		return this.noTransactionsRcvd;
	}

	public BigInteger getCumulativeScore() {
		return this.cumulativeScore;
	}

	public BigInteger getProfileIndex() {
		return this.profileIndex;
	}

	@Override
	public String toString() {
		return "UserProfileData("+
			"owner: " + this.getOwner() + 
			", userName: " + this.getUserName() + 
			", cumulativeScore: " + this.getCumulativeScore().toString() + 
			", noTransactionsSent: " + this.getNoTransactionsSent().toString() +
			", noTransactionsRcvd: " + this.getNoTransactionsRcvd().toString() +
			", profileIndex: " + this.getProfileIndex().toString() +
		")";
	}
}
