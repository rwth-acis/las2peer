package i5.las2peer.registry.data;

import i5.las2peer.registry.Util;
import i5.las2peer.registry.contracts.ReputationRegistry.TransactionAddedEventResponse;

import java.math.BigInteger;

/**
 * Contains reputation transaction data exactly as stored in blockchain.
 */
public class ReputationTransactionData {
	private String sender;
	private String subject;
	private BigInteger grading;
	private BigInteger subjectNewGrade;

	public ReputationTransactionData(String sender, String subject, BigInteger grading, BigInteger subjectNewGrade) {
		this.sender = sender;
		this.subject = subject;
		this.grading = grading;
		this.subjectNewGrade = subjectNewGrade;
	}

	public ReputationTransactionData(TransactionAddedEventResponse transaction)
	{
		this.sender = Util.recoverString(transaction.sender);
		this.subject = Util.recoverString(transaction.recipient);
		this.grading = transaction.grade;
		this.subjectNewGrade = transaction.recipientNewScore;
	}

	public String getSender() {
		return Util.recoverString(this.sender);
	}

	public String getSubject() {
		return Util.recoverString(this.subject);
	}

	public BigInteger getSubjectNewGrade() {
		return this.subjectNewGrade;
	}

	public BigInteger getGrading() {
		return this.grading;
	}

	@Override
	public String toString() {
		return "ReputationTransactionData("+
			"Sender: " + this.getSender() + 
			", Subject: " + this.getSubject() + 
			", Grading: " + this.getGrading().toString() + 
			", Subject New Grade: " + this.getSubjectNewGrade().toString() +
		")";
	}
}
