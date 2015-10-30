package i5.las2peer.persistency;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;

import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base64;

import i5.las2peer.execution.L2pThread;
import i5.las2peer.p2p.ArtifactNotFoundException;
import i5.las2peer.p2p.StorageException;
import i5.las2peer.security.Agent;
import i5.las2peer.security.Context;
import i5.las2peer.security.GroupAgent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import i5.las2peer.tools.SerializationException;
import i5.las2peer.tools.SerializeTools;
import i5.las2peer.tools.SimpleTools;
import i5.las2peer.tools.XmlTools;
import i5.simpleXML.Element;
import i5.simpleXML.Parser;
import i5.simpleXML.XMLSyntaxException;

// TODO: typed content?

/**
 * An envelope provides a secure storage for any serializable content within the LAS2peer network.
 * 
 * The content will be encrypted symmetrically, the key for opening the envelope will be provided to all entitled
 * {@link i5.las2peer.security.Agent}s via asymmetrical encryption. All encrypted versions of the decryption key are
 * part of the envelope itself.
 * 
 * The serialization of the content may be implemented via simple java serialization or the {@link XmlAble} facilities
 * of las2peer.
 * 
 */
public final class Envelope implements XmlAble, Cloneable {

	/**
	 * Type of the content inside the envelope.
	 * 
	 */
	public enum ContentType {
		String, XmlAble, Serializable, Binary
	};

	private byte[] baCipherData = null;
	private byte[] baPlainData = null;

	private ContentType contentType = null;
	private Class<?> clContentClass = null;

	private Agent openedBy = null;

	private boolean bOpen = false;

	/**
	 * may this envelope be overwritten without knowledge of the previous version?
	 * 
	 */
	private boolean bOverwriteBlindly = false;

	/**
	 * may the content of this envelope be overwritten by {@link #updateContent} or just via the object returned by
	 * {@link #getContent}
	 * 
	 * true indicates possibility of direct update
	 */
	private boolean bUpdateContent = true;

	private long id = 0;

	/**
	 * timestamp of the last change
	 */
	private long timestamp = -1;

	/**
	 * timestamp of a retrieved version
	 */
	private long loadedTimestamp = -1;

	/**
	 * the key for the symmetric encryption / decryption of the contents
	 */
	private SecretKey symmetricKey = null;

	/**
	 * encrypted versions of the symmetric key for each agent with read permissions
	 */
	private Hashtable<Long, byte[]> htEncryptedKeys = new Hashtable<Long, byte[]>();

	/**
	 * encrypted versions of the symmetric key for each group agent with read permissions
	 */
	private Hashtable<Long, byte[]> htEncryptedGroupKeys = new Hashtable<Long, byte[]>();

	/**
	 * hashtable with signatures for the content
	 */
	private Hashtable<Long, byte[]> htSignatures = new Hashtable<Long, byte[]>();

	/**
	 * storage for the content, if it is fetched via {@link #getContent}
	 */
	private Serializable contentStorage;

	/**
	 * base constructor for generating instances to be filled via {@link fromXmlString} used by the factory
	 * {@link createFromXml}
	 */
	private Envelope() {
		bOpen = false;
	}

	/**
	 * create a new envelope for the given content readable by the given owner
	 * 
	 * a new envelope will always be closed at the end of the creation process
	 * 
	 * @param content
	 * @param owner
	 * 
	 * @throws EncodingFailedException
	 * @throws DecodingFailedException
	 */
	public Envelope(byte[] content, Agent owner) throws EncodingFailedException, DecodingFailedException {
		this(content, new Agent[] { owner });
	}

	/**
	 * create a new envelope for the given content readable by the given owners
	 * 
	 * a new envelope will always be closed at the end of the creation process
	 * 
	 * @param content
	 * @param owners
	 * 
	 * @throws EncodingFailedException
	 */
	public Envelope(byte[] content, Agent[] owners) throws EncodingFailedException {
		this(content, owners, new Random().nextLong());
	}

	/**
	 * create a new envelope with the given id
	 * 
	 * a new envelope will always be closed at the end of the creation process
	 * 
	 * @param content
	 * @param owners
	 * @param id
	 * 
	 * @throws EncodingFailedException
	 * @throws L2pSecurityException
	 */
	private Envelope(byte[] content, Agent[] owners, long id) throws EncodingFailedException {
		this.id = id;
		bOpen = true;

		initKey();
		initOwners(owners);

		try {
			updateContent(content);
		} catch (L2pSecurityException e) {
			// should not occur, envelope is open by design
		}

		close();
	}

	/**
	 * initialize the encryption key for the content
	 */
	private void initKey() {
		symmetricKey = CryptoTools.generateSymmetricKey();
	}

	/**
	 * initialize all owners of this envelope
	 * 
	 * used by the constructors
	 * 
	 * @param owners
	 * @throws EncodingFailedException
	 */
	private void initOwners(Agent[] owners) throws EncodingFailedException {
		htEncryptedKeys = new Hashtable<Long, byte[]>();
		htEncryptedGroupKeys = new Hashtable<Long, byte[]>();
		for (Agent owner : owners) {
			addReader(owner);
		}
	}

	/**
	 * create a new Envelope for the given String as data
	 * 
	 * a new envelope will always be closed at the end of the creation process
	 * 
	 * @param content
	 * @param owner
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws EncodingFailedException
	 * @throws DecodingFailedException
	 */
	public Envelope(String content, Agent owner)
			throws UnsupportedEncodingException, EncodingFailedException, DecodingFailedException {
		this(content, new Agent[] { owner });
	}

	/**
	 * create a new Envelope for the given String as data
	 * 
	 * a new envelope will always be closed at the end of the creation process
	 * 
	 * @param content
	 * @param owners
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws EncodingFailedException
	 */
	public Envelope(String content, Agent[] owners) throws UnsupportedEncodingException, EncodingFailedException {
		this(content, owners, new Random().nextLong());
	}

	/**
	 * create a new envelope with the given id
	 * 
	 * a new envelope will always be closed at the end of the creation process
	 * 
	 * @param content
	 * @param owners
	 * @param id
	 * @throws UnsupportedEncodingException
	 * @throws EncodingFailedException
	 */
	private Envelope(String content, Agent[] owners, long id)
			throws UnsupportedEncodingException, EncodingFailedException {
		this.id = id;
		bOpen = true;

		initKey();
		initOwners(owners);

		try {
			updateContent(content);
		} catch (L2pSecurityException e) {
			// should not occur, envelope is open by design
		}

		close();
	}

	/**
	 * create an envelope for XmlAble content
	 * 
	 * a new envelope will always be closed at the end of the creation process
	 * 
	 * @param content
	 * @param owners
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws EncodingFailedException
	 */
	public Envelope(XmlAble content, Agent[] owners) throws UnsupportedEncodingException, EncodingFailedException {
		this(content, owners, new Random().nextLong());
	}

	/**
	 * create an envelope for XmlAble content
	 * 
	 * a new envelope will always be closed at the end of the creation process
	 * 
	 * used privately in the factory for unique class envelopes
	 * 
	 * @param content
	 * @param owners
	 * @param id
	 * @throws EncodingFailedException
	 * @throws UnsupportedEncodingException
	 */
	private Envelope(XmlAble content, Agent[] owners, long id)
			throws UnsupportedEncodingException, EncodingFailedException {
		this.id = id;
		this.bOpen = true;
		initKey();
		initOwners(owners);

		try {
			updateContent(content);
		} catch (L2pSecurityException e) {
			// should not occur, since the envelope is open by design
		}

		close();
	}

	/**
	 * creates an envelope for serializiable content
	 * 
	 * a new envelope will always be closed at the end of the creation process
	 * 
	 * @param content
	 * @param owner
	 * 
	 * @throws EncodingFailedException
	 * @throws SerializationException
	 * @throws DecodingFailedException
	 */
	public Envelope(Serializable content, Agent owner) throws EnvelopeException, SerializationException {
		this(content, new Agent[] { owner });
	}

	/**
	 * creates an envelope for serializable content
	 * 
	 * a new envelope will always be closed at the end of the creation process
	 * 
	 * @param content
	 * @param owners
	 * 
	 * @throws EncodingFailedException
	 * @throws SerializationException
	 */
	public Envelope(Serializable content, Agent[] owners) throws EncodingFailedException, SerializationException {
		this(content, owners, new Random().nextLong());
	}

	/**
	 * creates an envelope for serializable content
	 * 
	 * a new envelope will always be closed at the end of the creation process
	 * 
	 * used privately in the factory for unique class envelopes
	 * 
	 * @param content
	 * @param owners
	 * @param id
	 * @throws EncodingFailedException
	 * @throws SerializationException
	 */
	private Envelope(Serializable content, Agent[] owners, long id)
			throws EncodingFailedException, SerializationException {
		this.id = id;
		this.bOpen = true;

		initKey();
		initOwners(owners);

		try {
			updateContent(content);
		} catch (L2pSecurityException e) {
			// should not occur, since the envelope is open by design
		}

		close();
	}

	/**
	 * get the id of this envelope
	 * 
	 * @return id of the envelope
	 */
	public long getId() {
		return id;
	}

	/**
	 * May this envelope be overwritten by envelopes not knowing this state?
	 * 
	 * By default, blindly overwriting is turned off. In this case, another envelope trying to replace this one, has to
	 * refer to the timestamp of this envelope suggesting to be an updated version. Otherwise a replacement will fail
	 * with an {@link i5.las2peer.security.L2pSecurityException}.
	 * 
	 * @param overwrite
	 * @throws L2pSecurityException
	 */
	public void setOverWriteBlindly(boolean overwrite) throws L2pSecurityException {
		if (!isOpen())
			throw new L2pSecurityException("evenlope has to be openend before manipulation!");

		bOverwriteBlindly = overwrite;
	}

	/**
	 * may this envelope be overwritten without reference to the existing one?
	 * 
	 * @return true, if overwrite is allowed
	 */
	public boolean getOverwriteBlindly() {
		return bOverwriteBlindly;
	}

	/**
	 * get the timestamp
	 * 
	 * @return the timestamp
	 */
	public long getReferalTimestamp() {
		return loadedTimestamp;
	}

	/**
	 * open the encryption for the given agent
	 * 
	 * @param agent
	 * 
	 * @throws DecodingFailedException
	 * @throws L2pSecurityException
	 */
	public void open(Agent agent) throws DecodingFailedException, L2pSecurityException {
		try {
			byte[] encoded;
			if (agent instanceof GroupAgent)
				encoded = htEncryptedGroupKeys.get(agent.getId());
			else
				encoded = htEncryptedKeys.get(agent.getId());

			if (encoded == null) {
				// System.out.println ( this.toXmlString() );
				throw new L2pSecurityException("agent " + agent.getId() + " has no access to this object");
			}

			symmetricKey = (SecretKey) agent.returnSecretKey(encoded);
			openedBy = agent;

			decryptData();
			bOpen = true;
		} catch (SerializationException e) {
			throw new DecodingFailedException("unable to deserialize decoded symmetric key", e);
		} catch (CryptoException e) {
			throw new DecodingFailedException("Crypto problems decoding symmetric key", e);
		}
	}

	/**
	 * tries to open the encryption inside a L2pThread and the corresponding context
	 * 
	 * @throws L2pSecurityException
	 * @throws DecodingFailedException
	 */
	public void open() throws L2pSecurityException, DecodingFailedException {
		Thread current = Thread.currentThread();

		if (!(current instanceof L2pThread))
			throw new L2pSecurityException("Not inside a L2pThread!");

		((L2pThread) current).getContext().openEnvelope(this);

		// open ( ((L2pThread) current ).getContext().getMainAgent() );
	}

	/**
	 * close the envelope, i.e. encrypt all data with the key and forget the key
	 * 
	 * @throws EncodingFailedException
	 */
	public void close() throws EncodingFailedException {
		if (baCipherData != null)
			// nothing to do
			return;

		if (contentStorage != null) {
			try {
				baPlainData = SerializeTools.serialize(contentStorage);
			} catch (SerializationException e) {
				throw new EncodingFailedException("Serialization problems", e);
			}
			contentStorage = null;
		}

		if (baPlainData == null)
			throw new NullPointerException("No data");

		if (htEncryptedKeys.size() == 0 && htEncryptedGroupKeys.size() == 0)
			throw new EncodingFailedException("The Envelope should be readable to at least one agent!");

		try {
			baCipherData = CryptoTools.encryptSymmetric(baPlainData, symmetricKey);

			baPlainData = null;
			symmetricKey = null;
			openedBy = null;
			bOpen = false;
		} catch (CryptoException e) {
			throw new EncodingFailedException("crypto problems", e);
		}
	}

	/**
	 * decrypts the encrypted data for later usage via getContent... methods
	 * 
	 * @throws DecodingFailedException
	 */
	private void decryptData() throws DecodingFailedException {
		if (baPlainData != null)
			return; // nothing to do

		if (baCipherData == null)
			throw new NullPointerException("No encrypted data found!");

		if (symmetricKey == null)
			throw new DecodingFailedException("You need to open the envelope for an agent first!");

		try {
			baPlainData = CryptoTools.decryptSymmetric(baCipherData, symmetricKey);
			baCipherData = null;
		} catch (CryptoException e) {
			throw new DecodingFailedException("crypto problems!", e);
		}
	}

	/**
	 * add the given agent to the allowed readers
	 * 
	 * @param agent
	 * 
	 * @throws EncodingFailedException
	 */
	public void addReader(Agent agent) throws EncodingFailedException {
		if (symmetricKey == null)
			throw new EncodingFailedException("Envelope has not been opened yet!");

		try {
			byte[] encodedKey = CryptoTools.encryptAsymmetric(symmetricKey, agent.getPublicKey());

			if (agent instanceof GroupAgent)
				htEncryptedGroupKeys.put(agent.getId(), encodedKey);
			else
				htEncryptedKeys.put(agent.getId(), encodedKey);
		} catch (CryptoException e) {
			throw new EncodingFailedException("crypto problems", e);
		} catch (SerializationException e) {
			throw new EncodingFailedException("unable to serialize symmetric key!", e);
		}
	}

	/**
	 * remove an agent from the readers
	 * 
	 * @param agent
	 * 
	 * @throws L2pSecurityException
	 */
	public void removeReader(Agent agent) throws L2pSecurityException {
		if (!isOpen())
			throw new L2pSecurityException("you have to open the envelope first!");

		if (agent instanceof GroupAgent)
			htEncryptedGroupKeys.remove(agent.getId());
		else
			htEncryptedKeys.remove(agent.getId());
	}

	/**
	 * add a signature for the content
	 * 
	 * @param agent
	 * 
	 * @throws EncodingFailedException
	 */
	public void addSignature(Agent agent) throws EncodingFailedException {
		if (isClosed())
			throw new IllegalStateException("This envelope is not open!");

		if (!agent.equals(openedBy))
			throw new IllegalStateException("The given agent has not opened this envelope!");

		try {
			byte[] signature = agent.signContent(baPlainData);
			htSignatures.put(agent.getId(), signature);
		} catch (CryptoException e) {
			throw new EncodingFailedException("Crypto problems", e);
		} catch (L2pSecurityException e) {
			throw new EncodingFailedException("Security problems", e);
		}
	}

	/**
	 * remove a signature of the content
	 * 
	 * @param agent
	 * 
	 * @throws EncodingFailedException
	 */
	public void removeSignature(Agent agent) throws EncodingFailedException {
		if (!isOpen())
			throw new IllegalStateException("This envelope is not open!");
		if (isClosed())
			throw new IllegalStateException("This envelope is not open!");

		if (!openedBy.equals(agent))
			throw new EncodingFailedException("The given agent has not opened this envelope!");

		if (!isSignedBy(agent))
			throw new EncodingFailedException("The given agent has not signed this envelope!");

		htSignatures.remove(agent.getId());
	}

	/**
	 * verify a signature for the given agent
	 * 
	 * @param agent
	 * 
	 * @throws VerificationFailedException
	 */
	public void verifySignature(Agent agent) throws VerificationFailedException {
		if (isClosed())
			throw new IllegalStateException("This envelope is not open!");

		if (!isSignedBy(agent))
			throw new VerificationFailedException("the given agent has not signed this envelope!");

		byte[] signature = htSignatures.get(agent.getId());

		try {
			if (!CryptoTools.verifySignature(signature, baPlainData, agent.getPublicKey()))
				throw new VerificationFailedException("Verification failed!!");
		} catch (Exception e) {
			throw new VerificationFailedException("Verification failed due to other exception", e);
		}
	}

	/**
	 * checks, if this envelope contains a signature of the given agent
	 * 
	 * @return true, if a signature for this agent exists
	 */
	public boolean isSignedBy(Agent agent) {
		return isSignedBy(agent.getId());
	}

	/**
	 * checks, if this envelope contains a signature of the given agent
	 * 
	 * @param agentId
	 * 
	 * @return true, if a signature for this agent exists
	 */
	private boolean isSignedBy(long agentId) {
		return ((htSignatures.get(agentId)) != null);
	}

	/**
	 * may the {@link #baCipherData} method be used on this envelope?
	 * 
	 * @return true, if updateContent is enabled
	 */
	public boolean isUpdateable() {
		return bUpdateContent;
	}

	/**
	 * lock the content of this envelope, so that updates are only possible via the object returned by
	 * {@link #getContent}
	 */
	public void lockContent() {
		if (!contentType.equals(ContentType.Serializable))
			throw new IllegalArgumentException("only envelopes containing serializable content may be locked!");

		bUpdateContent = false;
	}

	/**
	 * returns the binary content of this envelope
	 * 
	 * @return content as byte array
	 * 
	 * @throws DecodingFailedException
	 */
	public byte[] getContentAsBinary() throws DecodingFailedException {
		if (baPlainData == null)
			decryptData();

		if (baPlainData == null)
			throw new NullPointerException();

		return baPlainData.clone();
	}

	/**
	 * returns the contents of this envelope as string
	 * 
	 * @return content as string
	 * 
	 * @throws DecodingFailedException
	 */
	public String getContentAsString() throws EnvelopeException {
		byte[] content = null;
		try {
			content = getContentAsBinary();
			return new String(content, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return new String(content);
		} catch (Exception e) {
			throw new EnvelopeException("Coding problems with interpreting the content", e);
		}
	}

	/**
	 * returns the contents of this envelope as parsed xml (root element)
	 * 
	 * @return content as parsed XML node
	 * @throws EnvelopeException
	 */
	public Element getContentAsXML() throws EnvelopeException {
		try {
			return Parser.parse(getContentAsString());
		} catch (Exception e) {
			throw new EnvelopeException("unable to parse XML content", e);
		}
	}

	/**
	 * try to return the contents of the envelope as a real object which has been serialized via XmlAble
	 * 
	 * @return content as XmlAble instance
	 * 
	 * @throws EnvelopeException
	 */
	public XmlAble getContentAsXmlAble() throws EnvelopeException {
		if (contentType != ContentType.XmlAble)
			throw new IllegalArgumentException("Not a XmlAble!");

		try {
			return XmlTools.createFromXml(getContentAsString(), clContentClass);
		} catch (Exception e) {
			throw new EnvelopeException("unable to create instance from xml content", e);
		}
	}

	/**
	 * get a list with all ids of groups entitled to read this envelope
	 * 
	 * @return array with group agent ids
	 */
	public Long[] getReaderGroups() {
		return htEncryptedGroupKeys.keySet().toArray(new Long[0]);
	}

	/**
	 * try to return the contents of the envelope as a real object which has been serialized via standard serialization
	 * 
	 * @return deserialized content via standard java serialization
	 * 
	 * @throws EnvelopeException
	 */
	public Serializable getContentAsSerializable() throws EnvelopeException {
		try {
			if (contentType != ContentType.Serializable)
				throw new IllegalArgumentException("Not a serializable!");
			return SerializeTools.deserialize(getContentAsBinary());
		} catch (Exception e) {
			throw new EnvelopeException("Content retrieval failed!", e);
		}
	}

	/**
	 * for better accessibility of typed content
	 * 
	 * @param cls
	 * @return the typed content of this envelope
	 * @throws EnvelopeException
	 */
	@SuppressWarnings("unchecked")
	public <T extends Serializable> T getContent(Class<T> cls) throws EnvelopeException {
		try {
			if (contentStorage == null)
				contentStorage = getContentAsSerializable();

			return (T) contentStorage;
		} catch (ClassCastException e) {
			throw new EnvelopeException("content is not of class " + cls, e);
		}
	}

	/**
	 * @return a xml (string) representation of this envelope
	 */
	public String toXmlString() {
		if (baPlainData != null && baCipherData == null) {
			try {
				close();
			} catch (EncodingFailedException e) {
				// TODO: exceptions for toXmlString!
				throw new RuntimeException("problems with the encoding of the envelope contents!", e);
			}
		}

		String encodedKeys = "\t<las2peer:keys encoding=\"base64\" encryption=\"" + CryptoTools.getAsymmetricAlgorithm()
				+ "\">\n";
		for (Long id : htEncryptedKeys.keySet()) {
			encodedKeys += "\t\t<las2peer:key id=\"" + id + "\">"
					+ Base64.encodeBase64String(htEncryptedKeys.get(id))
					+ "</las2peer:key>\n";
		}
		for (Long id : htEncryptedGroupKeys.keySet()) {
			encodedKeys += "\t\t<las2peer:key id=\"" + id + "\" type=\"group\">"
					+ Base64.encodeBase64String(htEncryptedGroupKeys.get(id))
					+ "</las2peer:key>\n";
		}
		encodedKeys += "\t</las2peer:keys>\n";

		String classInfo = "";
		if (contentType == ContentType.XmlAble || contentType == ContentType.Serializable)
			classInfo = " class=\"" + clContentClass.getCanonicalName() + "\"";

		String signatures = "";
		if (htSignatures.size() > 0) {
			signatures = "\t<las2peer:signatures encoding=\"base64\" method=\"" + CryptoTools.getSignatureMethod()
					+ "\">\n";
			for (Long id : htSignatures.keySet()) {
				signatures += "\t\t<las2peer:signature id=\"" + id + "\">"
						+ Base64.encodeBase64String(htSignatures.get(id))
						+ "</las2peer:signature>\n";
			}
			signatures += "\t</las2peer:signatures>\n";
		}

		return "<las2peer:envelope lastchange=\"" + timestamp + "\""
				+ " id=\"" + id + "\" blindOverwrite=\"" + bOverwriteBlindly + "\""
				+ " update=\"" + bUpdateContent + "\""
				+ ">\n"
				+ "\t<las2peer:content encoding=\"Base64\" type=\"" + contentType.toString() + "\"" + classInfo + ">\n"
				+ Base64.encodeBase64String(baCipherData)
				+ "\t</las2peer:content>\n"
				+ encodedKeys
				+ signatures
				+ "</las2peer:envelope>\n";

		// TODO: signatures?
	}

	/**
	 * 
	 * @return the agent that opened this envelop, null if the envelope is still closed
	 */
	public Agent getOpeningAgent() {
		return openedBy;
	}

	/**
	 * @return true, if this envelope is open
	 */
	public boolean isOpen() {
		return bOpen;
	}

	/**
	 * @return true, if this envelope is closed
	 */
	public boolean isClosed() {
		return !isOpen();
	}

	/**
	 * set the timestamp of the last change!
	 * 
	 * @throws L2pSecurityException
	 */
	public void touch() throws L2pSecurityException {
		if (!isOpen())
			throw new L2pSecurityException("Envelope has to be opened before touching");

		this.timestamp = new Date().getTime();
	}

	/**
	 * stores this envelope to the persistent storage of Las2peer
	 * 
	 * @throws StorageException
	 * @throws L2pSecurityException
	 */
	public void store() throws StorageException, L2pSecurityException {
		Context.getCurrent().getLocalNode().storeArtifact(this);
	}

	/**
	 * stores this envelope to the persistent storage of Las2peer
	 * 
	 * @param agent one of the owner agents
	 * @throws StorageException
	 * @throws L2pSecurityException
	 */
	public void store(Agent agent) throws StorageException, L2pSecurityException {
		if (agent.getRunningAtNode() != null) {
			agent.getRunningAtNode().storeArtifact(this);
		} else
			throw new StorageException("Agent not registered at any node");
	}

	/**
	 * factory for generating an envelope from the given xml String representation
	 * 
	 * @param xml
	 * @return envelope created from the given xml String serialization
	 * 
	 * @throws MalformedXMLException
	 */
	public static Envelope createFromXml(Element root) throws MalformedXMLException {
		Envelope result = new Envelope();
		try {
			if (!root.getName().equals("envelope"))
				throw new MalformedXMLException("not an envelope");

			if (!root.hasAttribute("id"))
				throw new MalformedXMLException("id attribute expected!");
			if (root.hasAttribute("blindOverwrite"))
				result.bOverwriteBlindly = Boolean.valueOf(root.getAttribute("blindOverwrite"));
			if (root.hasAttribute("overwrite"))
				result.bUpdateContent = Boolean.valueOf(root.getAttribute("update"));

			result.id = Long.parseLong(root.getAttribute("id"));
			result.loadedTimestamp = result.timestamp = Long.parseLong(root.getAttribute("lastchange"));

			Element content = root.getFirstChild();
			if (!content.getName().equals("content"))
				throw new MalformedXMLException("envelope content expected");
			if (!content.getAttribute("encoding").equals("Base64"))
				throw new MalformedXMLException("base 64 encoding of the content expected");

			result.contentType = stringToType(content.getAttribute("type"));
			if (content.hasAttribute("class"))
				try {
					String classname = content.getAttribute("class");
					if (classname.endsWith("[]"))
						classname = classname.substring(0, classname.length() - 2);

					result.clContentClass = Class.forName(classname);

				} catch (ClassNotFoundException e) {
					throw new MalformedXMLException("content class " + content.getAttribute("class") + " not found!");
				}

			result.baCipherData = Base64.decodeBase64(content.getFirstChild().getText());

			Element keys = root.getChild(1);
			if (!keys.getName().equals("keys"))
				throw new MalformedXMLException("not an envelope");
			if (!keys.getAttribute("encoding").equals("base64"))
				throw new MalformedXMLException(
						"base 64 encoding of the content expected - got: " + keys.getAttribute("encoding"));
			if (!keys.getAttribute("encryption").equals(CryptoTools.getAsymmetricAlgorithm()))
				throw new MalformedXMLException(
						CryptoTools.getAsymmetricAlgorithm() + " encryption of the content expected");

			for (Enumeration<Element> enKeys = keys.getChildren(); enKeys.hasMoreElements();) {
				Element key = enKeys.nextElement();
				if (!key.getName().equals("key"))
					throw new MalformedXMLException("key expected");

				long id = Long.parseLong(key.getAttribute("id"));

				if (key.hasAttribute("type") && key.getAttribute("type").equals("group"))
					result.htEncryptedGroupKeys.put(id, Base64.decodeBase64(key.getFirstChild().getText()));
				else
					result.htEncryptedKeys.put(id, Base64.decodeBase64(key.getFirstChild().getText()));
			}

			// signaturen
			if (root.getChildCount() > 2) {
				Element signatures = root.getChild(2);
				if (!signatures.getName().equals("signatures"))
					throw new MalformedXMLException("signatures expected");
				if (!signatures.getAttribute("encoding").equals("base64"))
					throw new MalformedXMLException(
							"base 64 encoding of the content expected - got: " + keys.getAttribute("encoding"));
				if (!signatures.getAttribute("method").equals(CryptoTools.getSignatureMethod()))
					throw new MalformedXMLException(CryptoTools.getSignatureMethod() + " expected as signature method");

				for (Enumeration<Element> enSigs = signatures.getChildren(); enSigs.hasMoreElements();) {
					Element sig = enSigs.nextElement();
					if (!sig.getName().equals("signature"))
						throw new MalformedXMLException("signature expected");

					long id = Long.parseLong(sig.getAttribute("id"));
					result.htSignatures.put(id, Base64.decodeBase64(sig.getFirstChild().getText()));
				}
			}

		} catch (XMLSyntaxException e) {
			throw new MalformedXMLException("problems with parsing the xml document", e);
		}

		// TODO signatures ?!?
		// further XML checks?

		return result;
	}

	/**
	 * get a locked copy of this agent
	 * 
	 * @return a locked clone of this envelope
	 * @throws EncodingFailedException
	 */
	public final Envelope cloneLocked() throws EnvelopeException {
		try {
			Envelope result = (Envelope) clone();
			result.close();
			return result;
		} catch (CloneNotSupportedException e) {
			throw new EnvelopeException("Cloning problems", e);
		}
	}

	/**
	 * @param s
	 * 
	 * @return a ContentType for the given String representation
	 */
	private static ContentType stringToType(String s) {
		if (s.equals("String"))
			return ContentType.String;
		else if (s.equals("Serializable"))
			return ContentType.Serializable;
		else if (s.equals("XmlAble"))
			return ContentType.XmlAble;
		else if (s.equals("Binary"))
			return ContentType.Binary;
		else
			throw new IllegalArgumentException("Illegal Contenttype: " + s);
	}

	/**
	 * factory for generating an envelope from the given XML String representation
	 * 
	 * @param xml
	 * @return envelope created from the given XML String serialization
	 * 
	 * @throws MalformedXMLException
	 */
	public static Envelope createFromXml(String xml) throws MalformedXMLException {
		try {
			return createFromXml(Parser.parse(xml, false));
		} catch (XMLSyntaxException e) {
			throw new MalformedXMLException("problems with parsing the xml document", e);
		}
	}

	/**
	 * get a long id for a specific class/identifier combination
	 * 
	 * // TODO: handle hash collisions?! // possible: extend identifier with counter, if a collision occurs
	 * 
	 * @param cls
	 * @param identifier
	 * 
	 * @return a (hash) ID for the given class using the given identifier
	 */
	public static long getClassEnvelopeId(String cls, String identifier) {
		return SimpleTools.longHash("cls-" + cls + "-" + identifier);
	}

	/**
	 * get a long id for a specific class/identifier combination
	 * 
	 * // TODO: handle hash collisions?! // possible: extend identifier with counter, if a collision occurs
	 * 
	 * @param cls
	 * @param identifier
	 * 
	 * @return a (hash) ID for the given class using the given identifier
	 */
	public static long getClassEnvelopeId(Class<?> cls, String identifier) {
		return getClassEnvelopeId(cls.getCanonicalName(), identifier);
	}

	/**
	 * factory: create an envelope with a definite id for the given class and identifier string
	 * 
	 * @param content
	 * @param identifier
	 * @param owners
	 * 
	 * @return a new envelope
	 * 
	 * @throws EncodingFailedException
	 * @throws SerializationException
	 * @throws UnsupportedEncodingException
	 */
	public static Envelope createClassIdEnvelope(Object content, String identifier, Agent[] owners)
			throws EncodingFailedException, SerializationException, UnsupportedEncodingException {
		if (content instanceof String)
			return new Envelope((String) content, owners, getClassEnvelopeId(content.getClass(), identifier));
		else if (content instanceof XmlAble)
			return new Envelope((XmlAble) content, owners, getClassEnvelopeId(content.getClass(), identifier));
		else if (content instanceof Serializable)
			return new Envelope((Serializable) content, owners, getClassEnvelopeId(content.getClass(), identifier));
		else if (content instanceof byte[])
			return new Envelope((byte[]) content, owners, getClassEnvelopeId(content.getClass(), identifier));
		else
			throw new IllegalArgumentException("content has to be (xml) String, Serializable, XmlAble or byte[]");
	}

	/**
	 * factory: create an envelope with a definite id for the given class and identifier string
	 * 
	 * @param content
	 * @param identifier
	 * @param owner
	 * 
	 * @return a new envelope
	 * 
	 * @throws SerializationException
	 * @throws EncodingFailedException
	 * @throws UnsupportedEncodingException
	 */
	public static Envelope createClassIdEnvelope(Object content, String identifier, Agent owner)
			throws UnsupportedEncodingException, EncodingFailedException, SerializationException {
		return createClassIdEnvelope(content, identifier, new Agent[] { owner });
	}

	/**
	 * get a previously stored envelope from the p2p network.
	 * 
	 * Requires an active Las2Peer @{link i5.las2peer.security.Context}.
	 * 
	 * @param id
	 * @return an envelope
	 * @throws ArtifactNotFoundException
	 * @throws StorageException
	 */
	public static Envelope fetch(long id) throws ArtifactNotFoundException, StorageException {
		return Context.getCurrent().getStoredObject(id);
	}

	/**
	 * Get a previously stored envelope from the p2p network.
	 * 
	 * Requires an active LAS2Peer @{link i5.las2peer.security.Context}.
	 * 
	 * @param cls type of the class
	 * @param identifier an unique identifier for the envelope
	 * @return an envelope
	 * @throws ArtifactNotFoundException
	 * @throws StorageException
	 */
	public static Envelope fetchClassIdEnvelope(Class<?> cls, String identifier)
			throws ArtifactNotFoundException, StorageException {
		return Context.getCurrent().getStoredObject(cls, identifier);
	}

	/**
	 * Get a previously stored envelope from the p2p network.
	 *
	 * Requires an active LAS2Peer @{link i5.las2peer.security.Context}.
	 * 
	 * @param agent executing agent
	 * @param cls type of the class
	 * @param identifier an unique identifier for the envelope
	 * @return an envelope
	 * @throws ArtifactNotFoundException
	 * @throws StorageException
	 */
	public static Envelope fetchClassIdEnvelope(Agent agent, Class<?> cls, String identifier)
			throws ArtifactNotFoundException, StorageException {
		long id = Envelope.getClassEnvelopeId(cls, identifier);
		if (agent.getRunningAtNode() != null)
			return agent.getRunningAtNode().fetchArtifact(id);
		else
			throw new StorageException("Agent is not registered at any node");
	}

	/**
	 * set the contained data to the given binary content
	 * 
	 * side effect: all signatures are removed
	 * 
	 * @param content
	 * @throws L2pSecurityException
	 */
	public void updateContent(byte[] content) throws L2pSecurityException {
		if (!isOpen())
			throw new L2pSecurityException("You have to open the envelope before updating the content!");
		if (!bUpdateContent)
			throw new L2pSecurityException("Content my only be altered via the object retrieved by getContent!");

		baPlainData = content;
		timestamp = new Date().getTime();
		contentType = ContentType.Binary;

		// with new content, all signatures become invalid
		htSignatures.clear();
	}

	/**
	 * set the contained data
	 * 
	 * @param content
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws L2pSecurityException
	 */
	public void updateContent(String content) throws UnsupportedEncodingException, L2pSecurityException {
		updateContent(content.getBytes("UTF-8"));
		contentType = ContentType.String;
	}

	/**
	 * set the contained data to the given serializable
	 * 
	 * @param content
	 * @throws SerializationException
	 * @throws L2pSecurityException
	 */
	public void updateContent(Serializable content) throws L2pSecurityException, SerializationException {
		updateContent(SerializeTools.serialize(content));
		contentType = ContentType.Serializable;
		clContentClass = content.getClass();
	}

	/**
	 * set the contained data to the given XmlAble
	 * 
	 * @param content
	 * @throws L2pSecurityException
	 * @throws UnsupportedEncodingException
	 */
	public void updateContent(XmlAble content) throws UnsupportedEncodingException, L2pSecurityException {
		updateContent(content.toXmlString());
		contentType = ContentType.XmlAble;
		clContentClass = content.getClass();
	}

	/**
	 * check, if the current envelope may be overwritten by the given one. If the current envelope is not signed,
	 * overwriting is permitted.
	 * 
	 * Otherwise, the new envelope must contain at least one signature of the signing agents if this envelope.
	 * 
	 * If overwriting is not allowed, a L2pSecurityException is thrown
	 * 
	 * @param envelope
	 * @throws L2pSecurityException
	 */
	public void checkOverwrite(Envelope envelope) throws L2pSecurityException {
		if (!this.getOverwriteBlindly() && this.getReferalTimestamp() != envelope.getReferalTimestamp())
			throw new OverwriteException("The new Envelope does not refer to this one!");

		if (this.htSignatures.size() == 0)
			return;

		for (long signedBy : htSignatures.keySet()) {
			if (envelope.isSignedBy(signedBy))
				return;
		}

		throw new L2pSecurityException(
				"Check for Overwriting envelope " + getId() + " failed: No needed signature is provided!");
	}

	/**
	 * get the timestamp of the last change (either before load or last update)
	 * 
	 * @return unix timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}

}