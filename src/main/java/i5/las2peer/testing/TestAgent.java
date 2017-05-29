package i5.las2peer.testing;

import java.security.KeyPair;

import javax.crypto.SecretKey;

import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.security.AgentContext;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.serialization.SerializationException;

public class TestAgent extends AgentImpl {

	protected TestAgent(KeyPair pair, SecretKey key) throws AgentOperationFailedException {
		super(pair, key);
	}

	@Override
	public String toXmlString() throws SerializationException {
		throw new SerializationException("This agent is for testing only!");
	}

	@Override
	public void receiveMessage(Message message, AgentContext c) throws MessageException {
		throw new MessageException("got a message");
	}

}
