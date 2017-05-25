package i5.las2peer.security;

import java.security.KeyPair;

import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentLockedException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.api.security.AnonymousAgent;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;

public class AnonymousAgentImpl extends UserAgentImpl implements AnonymousAgent {

	private static AnonymousAgentImpl instance;

	public static synchronized AnonymousAgentImpl getInstance() throws L2pSecurityException, CryptoException {
		if (instance == null) {
			instance = new AnonymousAgentImpl(CryptoTools.generateKeyPair(), AnonymousAgent.PASSPHRASE,
					CryptoTools.generateSalt());
		}
		return instance;
	}

	private AnonymousAgentImpl(KeyPair pair, String passphrase, byte[] salt)
			throws L2pSecurityException, CryptoException {
		super(pair, passphrase, salt);
	}

	@Override
	public void unlock(String passphrase) throws AgentAccessDeniedException, AgentOperationFailedException {
		super.unlock(AnonymousAgent.PASSPHRASE);
	}

	@Override
	public String getLoginName() {
		return AnonymousAgent.LOGIN_NAME;
	}

	@Override
	public String getEmail() {
		return AnonymousAgent.EMAIL;
	}

	@Override
	public void setLoginName(String loginName) throws AgentLockedException, IllegalArgumentException {
		throw new IllegalStateException("Can't change login name for anonymous agent");
	}

	@Override
	public void setEmail(String email) throws AgentLockedException, IllegalArgumentException {
		throw new IllegalStateException("Can't change email for anonymous agent");
	}

	@Override
	public String toXmlString() {
		throw new RuntimeException("anonymous agent should not be converted to XML");
	}

}
