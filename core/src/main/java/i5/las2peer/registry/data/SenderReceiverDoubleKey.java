package i5.las2peer.registry.data;

public class SenderReceiverDoubleKey {
    private String sender;
    private String receiver;

    public SenderReceiverDoubleKey(String sender, String receiver) {
        this.setSender(sender);
        this.setReceiver(receiver);
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }
}