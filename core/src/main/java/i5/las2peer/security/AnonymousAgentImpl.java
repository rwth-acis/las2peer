package i5.las2peer.security;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;

import javax.crypto.SecretKey;

import i5.las2peer.api.security.AgentLockedException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.api.security.AnonymousAgent;
import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.tools.CryptoException;

public class AnonymousAgentImpl extends AgentImpl implements AnonymousAgent {
	
	public static AnonymousAgentImpl getInstance() {
		try {
			return new AnonymousAgentImpl();
		} catch (AgentOperationFailedException e) {
			throw new IllegalStateException(e);
		}
	}

	private AnonymousAgentImpl() throws AgentOperationFailedException {
		super();
	}

	@Override
	public String toXmlString() {
		throw new IllegalStateException("Anonymous agent cannot be converted to XML");
	}

	@Override
	public void receiveMessage(Message message, AgentContext c) throws MessageException {
		// do nothing
	}

	@Override
	public void unlockPrivateKey(SecretKey key) {
		// do nothing
	}

	@Override
	public void encryptPrivateKey(SecretKey key) {
		// do nothing
	}

	@Override
	public boolean isLocked() {
		return false;
	}

	@Override
	public String getIdentifier() {
		return AnonymousAgent.IDENTIFIER;
	}

	@Override
	public PublicKey getPublicKey() {
		throw new IllegalStateException("Anonymous does not have a key pair!");
	}

	@Override
	public SecretKey decryptSymmetricKey(byte[] crypted)
			throws AgentLockedException, SerializationException, CryptoException {
		throw new AgentLockedException("Anonymous does not have a key pair!");
	}

	@Override
	public Signature createSignature() throws InvalidKeyException, AgentLockedException, NoSuchAlgorithmException {
		throw new AgentLockedException("Anonymous does not have a key pair!");
	}

	@Override
	public byte[] signContent(byte[] plainData) throws CryptoException, AgentLockedException {
		throw new AgentLockedException("Anonymous does not have a key pair!");
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof AnonymousAgent;
	}

}
