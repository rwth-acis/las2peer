package i5.las2peer.registry.data;

import java.math.BigInteger;

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