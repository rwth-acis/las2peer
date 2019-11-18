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

    public boolean equalsSender(String sender)
    {
        return this.sender.equals(sender);
    }

    public boolean equalsReceiver(String receiver)
    {
        return this.receiver.equals(receiver);
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

    @Override   
    public boolean equals(Object obj) {
      if (!(obj instanceof SenderReceiverDoubleKey))
        return false;
      SenderReceiverDoubleKey ref = (SenderReceiverDoubleKey) obj;
      return this.sender.equals(ref.sender) && 
          this.receiver.equals(ref.receiver);
    }

    @Override
    public int hashCode() {
        return sender.hashCode() ^ receiver.hashCode();
    }

    @Override
    public String toString() {
        return "Sender: " + sender + " | " + "Receiver: " + receiver;
    }
}