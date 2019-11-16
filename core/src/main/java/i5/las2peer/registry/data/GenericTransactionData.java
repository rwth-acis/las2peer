package i5.las2peer.registry.data;

import java.math.BigInteger;

/**
 * Represents the transaction with its message in the reputation smart contract
 * @see ReputationRegistry.addGenericTransaction
 */
public class GenericTransactionData {
    private BigInteger amountInWei;
    private String message;

    public GenericTransactionData(BigInteger amountInWei, String message) {
        this.setAmountInWei(amountInWei);
        this.setMessage(message);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BigInteger getAmountInWei() {
        return amountInWei;
    }

    public void setAmountInWei(BigInteger amountInWei) {
        this.amountInWei = amountInWei;
    }
}