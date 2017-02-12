package i5.las2peer.testing;

import java.security.KeyPair;

import javax.crypto.SecretKey;

import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.security.Agent;
import i5.las2peer.security.AgentContext;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.tools.SerializationException;

public class TestAgent extends Agent {

	protected TestAgent(KeyPair pair, SecretKey key) throws L2pSecurityException {
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
