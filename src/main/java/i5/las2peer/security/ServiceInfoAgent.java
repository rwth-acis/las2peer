package i5.las2peer.security;

import java.io.Serializable;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

import org.apache.commons.codec.binary.Base64;

import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.p2p.ServiceList;
import i5.las2peer.p2p.ServiceNameVersion;
import i5.las2peer.p2p.ServiceNodeList;
import i5.las2peer.p2p.StorageException;
import i5.las2peer.persistency.DecodingFailedException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.persistency.EnvelopeException;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import i5.las2peer.tools.SerializationException;
import i5.las2peer.tools.SerializeTools;
import rice.pastry.NodeHandle;
import rice.pastry.PastryNode;

/**
 * 
 */

/**
 * This Agent is a public single instance agent for the solely purpose of keeping (loosely) track of all registered
 * services in the network
 */
public class ServiceInfoAgent extends PassphraseAgent {

	private static final long AGENT_ID = 6784596877L;
	// because it needs to be publicly accessible
	private static final String PUB_KEY = "rO0ABXNyABRqYXZhLnNlY3VyaXR5LktleVJlcL35T7OImqVDAgAETAAJYWxnb3JpdGhtdAASTGphdmEvbGFuZy9TdHJpbmc7WwAHZW5jb2RlZHQAAltCTAAGZm9ybWF0cQB+AAFMAAR0eXBldAAbTGphdmEvc2VjdXJpdHkvS2V5UmVwJFR5cGU7eHB0AANSU0F1cgACW0Ks8xf4BghU4AIAAHhwAAABJjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAIMfgqVrRtAHkIgyfwtaYus4ua5YY+Mxe8K9ctEq/tqhJIYE8NnV1vkf9MDdnDiMg/C8VF21UELVX8b5AYBAJriiP+sDVGi4vqbVausLIMdQ7iTVqGOAsDXkI40pmgrntw/NwcQnE9Cqqbzn8c0kHRyx1Obc5hruVdzbzRs7dWmuAPwkr5rFxsAWd/Weaj0cmM2oPae+T9IyFpUdxPwVUtNe2VKSqxEbqWNO3vVLZ2hCASJjg4+W5PqSLGd3CV2oaXy9dAzmf1X9UJfl+OYOe7aks7hmNEwP6D4eOsmrKyRmeAnagwV2A9GzE/qTOQ1grKSbkYy4XELNuRILrjFaII0CAwEAAXQABVguNTA5fnIAGWphdmEuc2VjdXJpdHkuS2V5UmVwJFR5cGUAAAAAAAAAABIAAHhyAA5qYXZhLmxhbmcuRW51bQAAAAAAAAAAEgAAeHB0AAZQVUJMSUM=";
	private static final String PRIV_KEY = "rO0ABXNyABRqYXZhLnNlY3VyaXR5LktleVJlcL35T7OImqVDAgAETAAJYWxnb3JpdGhtdAASTGphdmEvbGFuZy9TdHJpbmc7WwAHZW5jb2RlZHQAAltCTAAGZm9ybWF0cQB+AAFMAAR0eXBldAAbTGphdmEvc2VjdXJpdHkvS2V5UmVwJFR5cGU7eHB0AANSU0F1cgACW0Ks8xf4BghU4AIAAHhwAAAEwjCCBL4CAQAwDQYJKoZIhvcNAQEBBQAEggSoMIIEpAIBAAKCAQEAgx+CpWtG0AeQiDJ/C1pi6zi5rlhj4zF7wr1y0Sr+2qEkhgTw2dXW+R/0wN2cOIyD8LxUXbVQQtVfxvkBgEAmuKI/6wNUaLi+ptVq6wsgx1DuJNWoY4CwNeQjjSmaCue3D83BxCcT0KqpvOfxzSQdHLHU5tzmGu5V3NvNGzt1aa4A/CSvmsXGwBZ39Z5qPRyYzag9p75P0jIWlR3E/BVS017ZUpKrERupY07e9UtnaEIBImODj5bk+pIsZ3cJXahpfL10DOZ/Vf1Ql+X45g57tqSzuGY0TA/oPh46yasrJGZ4CdqDBXYD0bMT+pM5DWCspJuRjLhcQs25EguuMVogjQIDAQABAoIBAAIy5hL2EJLufYr7Jcw/8Ma3Bc4Zp1so+kVSvfkp+moaJ62jqZJNlrRIx+bwEG2WVaQU1GlZ4AWu5FNG27KV4NBZ0C6VuLWk23WawJc+cYdGISg2+QLqJopQ0BPO7clfB1/ZeHVcnmVyZzRGw3RINcDEbqiMbcNn2cLBYNgjEB38iaX/fTQugdvjotrCyFlfjwFFUlc2REtmd0Y8b0EtzjLFbckhk9pwjy60YMnZ/pCjXxamYInLI04ldRG2jtZGZOOscD4kETyFiJv7UUgQWfS/P4BHjdVHyrgVDQJH9vcb+Hi6Zx8AnOVDuvRYe6PuRchXBUMzYOtf53rEc4DzEKECgYEA7kcA2mGO7A9v6SNs8zbGe4XuQjJ+W2+ISm263+wkOlHe+9Zha2yeLEsziOdCbi+wBX+EYZEcDXO1Ox71/JkH8Yz7kg0tjNvxdzDatbluvdBPNvWs8vAvSt5+mSS73263jjyEw7h7z2/ESP4PK7U9Ug27sAsD2EYAg1Lsct24jBkCgYEAjOAzoDCprwnAPapx9qOXe21i9BzTDdpzIfHgzO04SYWL2SlqFt/LJTb1BPBE+YodHHnEEWpn82nNDW3cd345tECCxpwvbN2QmA8+Nvj/Pf5HdATdkUuotaDmbHT0mI/2Sk8cerjj3cH9pmWzztH8SXEvLDqCZ9v/jwnpXBPoBpUCgYEA4KO8ICZ9sfvTy/6EBsALXAUmXO5xxg5edZ0B434joX/yM4cnjTl33daAHX+5V1xKHMTdr4Y45k3B/Jzx2FUF8iqyOj2GRhhNi8tZRp4t03ICXJQ9m0PpsjIVNJg3LyyYjNZtbIAO6cA7U32CG/jgeO1Nl2irFUjZzvVsydZS2HECgYBqjKh/YE72tMlR2riXcuP/1pwhRivbLn4mDmuYk1MfSIKdnVVAN8POQeLP+Wox0uRxxScmhPRahoswvQci2bWLP7+puDemf239lInZyjpDCS/B8GwkmLThqvCc11ioizocufkwWOb/stnGIOX+Z5QJeDHVoes/4oVICpcVrXiwgQKBgQCleXyovnQENjtlra5qmkwoetqVf5Gny/If7KRw/PlHYUrixmVX4ZVCkesB/LRYEA5SIbgJZjZNIVS4iqeozsEshVuSIuhmHyo/4LIsa2wUtuHbjZeRR0MEAg880Rlg+7bFByq9HE8UN8H2HuTdSPzVYV4c58/6OjmYALz4jrDFCHQABlBLQ1MjOH5yABlqYXZhLnNlY3VyaXR5LktleVJlcCRUeXBlAAAAAAAAAAASAAB4cgAOamF2YS5sYW5nLkVudW0AAAAAAAAAABIAAHhwdAAHUFJJVkFURQ==";
	private static final String SALT = "sDNCXOTiYOyEAUwjzG3btQ==";
	private static final String DEFAULT_PASSPHRASE = "pass";
	private static String passPhrase = DEFAULT_PASSPHRASE;
	private static final String SERVICE_LIST_ENVELOPE_NAME = "ServiceInfoDataStorage";

	private static final String SERVICE_NODE_LIST_PREFIX = "ServiceNodeListStorage_";

	private static ServiceInfoAgent agent = null;

	/**
	 * Agent is public and has a default passphrase
	 * 
	 * @return the passphrase
	 */
	public static String getDefaultPassphrase() {
		return DEFAULT_PASSPHRASE;
	}

	/**
	 * default constructor
	 * 
	 * @param id
	 * @param pair
	 * @param passphrase
	 * @param salt
	 * @throws L2pSecurityException
	 * @throws CryptoException
	 */
	protected ServiceInfoAgent(long id, KeyPair pair, String passphrase, byte[] salt)
			throws L2pSecurityException, CryptoException {
		super(id, pair, passphrase, salt);
	}

	/**
	 * default constructor
	 * 
	 * @param id
	 * @param pubKey
	 * @param encodedPrivate
	 * @param salt
	 */
	protected ServiceInfoAgent(long id, PublicKey pubKey, byte[] encodedPrivate, byte[] salt) {
		super(id, pubKey, encodedPrivate, salt);
	}

	/**
	 * Retrieves the single agent instance
	 * 
	 * @param passphrase
	 * @return a {@link ServiceInfoAgent}
	 * @throws CryptoException
	 * @throws L2pSecurityException
	 * @throws SerializationException
	 */
	public static ServiceInfoAgent getServiceInfoAgent(String passphrase)
			throws CryptoException, L2pSecurityException, SerializationException {

		passPhrase = passphrase;
		if (agent == null) {

			agent = new ServiceInfoAgent(AGENT_ID,
					new KeyPair((PublicKey) SerializeTools.deserializeBase64(PUB_KEY),
							(PrivateKey) SerializeTools.deserializeBase64(PRIV_KEY)),
					passphrase, Base64.decodeBase64(SALT));

		}
		if (agent.isLocked())
			agent.unlockPrivateKey(passPhrase);
		return agent;
	}

	/**
	 * Retrieves the single agent instance
	 * 
	 * @return a {@link ServiceInfoAgent}
	 * @throws CryptoException
	 * @throws L2pSecurityException
	 * @throws SerializationException
	 */
	public static ServiceInfoAgent getServiceInfoAgent()
			throws CryptoException, L2pSecurityException, SerializationException {

		return getServiceInfoAgent(passPhrase);
	}

	/**
	 * Returns an array of currently registered services
	 * 
	 * @return an array of {@link i5.las2peer.p2p.ServiceNameVersion}s
	 * @throws EnvelopeException
	 */
	public static ServiceNameVersion[] getServices() throws EnvelopeException {
		return ((ServiceList) getEnvelopeData(SERVICE_LIST_ENVELOPE_NAME, ServiceList.class)).getServices();
	}

	/**
	 * Reads {@link i5.las2peer.p2p.ServiceList} from the Envelope
	 * 
	 * @return {@link i5.las2peer.p2p.ServiceList}
	 * @throws EnvelopeException
	 */
	private static Serializable getEnvelopeData(String envelopeName, Class<? extends Serializable> dataCls)
			throws EnvelopeException {
		Envelope env = fetchEnvelope(envelopeName, dataCls);
		Serializable data = null;
		if (env == null)
			throw new EnvelopeException("Envelope could not be found, nor created!");
		try {
			env.open(getServiceInfoAgent());
			data = env.getContent(dataCls);
			env.close();
		} catch (DecodingFailedException e) {
			throw new EnvelopeException("Envelope could not be decoded!", e);
		} catch (L2pSecurityException e) {
			throw new EnvelopeException("Security Exception", e);
		} catch (SerializationException e) {
			throw new EnvelopeException("Serialization Exception", e);
		} catch (CryptoException e) {
			throw new EnvelopeException("Crypto Exception", e);
		}
		return data;
	}

	/**
	 * UpdatesEnvelope with new {@link i5.las2peer.p2p.ServiceList}
	 * 
	 * @param data
	 * @throws EnvelopeException
	 */
	private static void setEnvelopeData(String envelopeName, Class<?> dataCls, Serializable data, Node node)
			throws EnvelopeException, AgentException, L2pSecurityException {
		try {
			getServiceInfoAgent(); // init (paranoia)
		} catch (Exception e) {
			// do nothing
		}
		if (!node.hasAgent(agent.getId())) {
			node.registerReceiver(agent);
		}
		agent.notifyRegistrationTo(node);

		Envelope env = fetchEnvelope(envelopeName, dataCls);

		if (env == null)
			throw new EnvelopeException("Envelope could not be found, nor created!");
		try {
			env.open(getServiceInfoAgent());
			env.updateContent(data);
			env.setOverWriteBlindly(true);
			env.store(agent);
			env.close();

		} catch (DecodingFailedException e) {
			throw new EnvelopeException("Envelope could not be decoded", e);
		} catch (L2pSecurityException e) {
			throw new EnvelopeException("Security Exception", e);
		} catch (SerializationException e) {
			throw new EnvelopeException("Data could not be serialized", e);
		} catch (StorageException e) {
			throw new EnvelopeException("Envelope could not be stored", e);
		} catch (CryptoException e) {
			throw new EnvelopeException("Crypto Execption", e);
		}

	}

	/**
	 * Gets the Envelope from the Network
	 * 
	 * @return
	 */
	private static Envelope fetchEnvelope(String envelopeName, Class<?> cls) {
		Envelope env = null;
		try {
			env = Envelope.fetchClassIdEnvelope(getServiceInfoAgent(), cls, envelopeName);
		} catch (Exception e) {
			env = createNewEnvelope(envelopeName, cls);
		}
		return env;
	}

	/**
	 * Creates a new Envelope
	 * 
	 * @return
	 */
	private static Envelope createNewEnvelope(String envelopeName, Class<?> cls) {
		try {

			return Envelope.createClassIdEnvelope(cls.newInstance(), envelopeName, getServiceInfoAgent());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public void receiveMessage(Message message, Context c) throws MessageException {
		// XXX method stub
	}

	@Override
	public String toXmlString() {
		try {
			StringBuffer result = new StringBuffer("<las2peer:agent type=\"monitoring\">\n" + "\t<id>" + getId()
					+ "</id>\n" + "\t<publickey encoding=\"base64\">" + SerializeTools.serializeToBase64(getPublicKey())
					+ "</publickey>\n" + "\t<privatekey encrypted=\"" + CryptoTools.getSymmetricAlgorithm()
					+ "\" keygen=\"" + CryptoTools.getSymmetricKeygenMethod() + "\">\n"
					+ "\t\t<salt encoding=\"base64\">" + Base64.encodeBase64String(getSalt()) + "</salt>\n"
					+ "\t\t<data encoding=\"base64\">" + getEncodedPrivate() + "</data>\n" + "\t</privatekey>\n");

			result.append("</las2peer:agent>\n");

			return result.toString();
		} catch (SerializationException e) {
			throw new RuntimeException("Serialization problems with keys");
		}
	}

	/**
	 * Called by {@link i5.las2peer.security.ServiceAgent} when it is registered to a node Updates the contents of the
	 * internal Envelope
	 * 
	 * @param serviceAgent
	 * @throws EnvelopeException
	 */
	public void serviceAdded(ServiceAgent serviceAgent, Node node)
			throws EnvelopeException, AgentException, L2pSecurityException {
		System.out.println("Service added " + serviceAgent.getServiceClassName());
		// FIXME versions of services
		ServiceNameVersion servicenameVersion = new ServiceNameVersion(serviceAgent.getServiceClassName(), "1.0");
		ServiceList data = (ServiceList) getEnvelopeData(SERVICE_LIST_ENVELOPE_NAME, ServiceList.class);
		data.addService(servicenameVersion);
		setEnvelopeData(SERVICE_LIST_ENVELOPE_NAME, ServiceList.class, data, node);
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (ServiceNameVersion s : data.getServices()) {
			if (!first) {
				sb.append(", ");
			}
			sb.append(s.getNameVersion());
			first = false;
		}
		System.out.println("Current services: " + sb.toString());

		String nodeEnvelope = SERVICE_NODE_LIST_PREFIX + servicenameVersion.getNameVersion();
		ServiceNodeList nodesData = (ServiceNodeList) getEnvelopeData(nodeEnvelope, ServiceNodeList.class);

		if (node instanceof PastryNodeImpl) {
			// this part seems to be irrelevant for LocalNode implementations
			PastryNode pNode = ((PastryNodeImpl) node).getPastryNode();
			nodesData.addNode(pNode.getLocalHandle());
			setEnvelopeData(nodeEnvelope, ServiceNodeList.class, nodesData, node);
		}
	}

	/**
	 * Called by {@link i5.las2peer.security.ServiceAgent} when it is unregistered from a node Updates the contents of
	 * the internal Envelope
	 * 
	 * @param serviceAgent
	 * @throws EnvelopeException
	 */
	public void serviceRemoved(ServiceAgent serviceAgent, Node node)
			throws EnvelopeException, AgentException, L2pSecurityException {
		ServiceNameVersion servicenameVersion = new ServiceNameVersion(serviceAgent.getServiceClassName(), "1.0");

		String nodeEnvelope = SERVICE_NODE_LIST_PREFIX + servicenameVersion.getNameVersion();
		ServiceNodeList nodesData = (ServiceNodeList) getEnvelopeData(nodeEnvelope, ServiceNodeList.class);
		boolean listIsEmpty = nodesData.removeNode((NodeHandle) node.getNodeId());
		setEnvelopeData(nodeEnvelope, ServiceNodeList.class, nodesData, node);

		if (listIsEmpty) {
			ServiceList data = (ServiceList) getEnvelopeData(SERVICE_LIST_ENVELOPE_NAME, ServiceList.class);
			data.removeService(servicenameVersion); // TODO versions of services
			setEnvelopeData(SERVICE_LIST_ENVELOPE_NAME, ServiceList.class, data, node);
		}
	}

	/**
	 * Resets the stored {@link i5.las2peer.p2p.ServiceList}
	 * 
	 * @throws EnvelopeException
	 */
	public void resetData(Node node) throws EnvelopeException, AgentException, L2pSecurityException {
		ServiceList data = new ServiceList();
		setEnvelopeData(SERVICE_LIST_ENVELOPE_NAME, ServiceList.class, data, node);
	}

	public static ArrayList<NodeHandle> getNodes(String serviceName, String serviceVersion) {
		String envelopeName = SERVICE_NODE_LIST_PREFIX + ServiceNameVersion.toString(serviceName, serviceVersion);
		try {
			ServiceNodeList nodesData = (ServiceNodeList) getEnvelopeData(envelopeName, ServiceNodeList.class);

			return nodesData.getNodes();
		} catch (EnvelopeException e) {
			return null;
		}

	}
}
