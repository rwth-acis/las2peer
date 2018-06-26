package i5.las2peer.p2p;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import i5.las2peer.api.persistency.EnvelopeException;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.persistency.EnvelopeVersion;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.tools.CryptoException;

/**
 * Responsible for mapping service aliases to service names and resolving paths to service names.
 *
 */
public class ServiceAliasManager {

	private static final String PREFIX = "SERVICE_ALIAS-";
	private static final int MAX_PATH_LEVEL = 10;
	private static final int MAX_VERSIONS = 20;
	private static final String SEPERATOR = "/";
	private static final String BLANK = "BLANK";

	public class AliasResolveResponse {
		String serviceNameVersion;
		int numMatchedParts;

		public AliasResolveResponse(String serviceNameVersion, int numMatchedParts) {
			this.serviceNameVersion = serviceNameVersion;
			this.numMatchedParts = numMatchedParts;
		}

		public String getServiceNameVersion() {
			return this.serviceNameVersion;
		}

		public int getNumMatchedParts() {
			return this.numMatchedParts;
		}
	}

	private Node node;

	public ServiceAliasManager(Node node) {
		this.node = node;
	}

	/**
	 * Registers the service alias of the given service.
	 * 
	 * Note that a service alias cannot be a prefix of another service alias.
	 * 
	 * @param agent an unlocked service agent
	 * @param alias an alias, optionally seperated by {@link #SEPERATOR} and not deeper than {@link #MAX_PATH_LEVEL}.
	 * @throws AliasConflictException if a conflict occurs (a prefix or whole alias is already registered)
	 */
	public void registerServiceAlias(ServiceAgentImpl agent, String alias) throws AliasConflictException {

		if (agent.isLocked()) {
			throw new IllegalArgumentException("Only unlocked Agents can be registered!");
		}

		// if no alias exists, simply return
		if (alias == null) {
			return;
		}

		String serviceName = agent.getServiceNameVersion().getNameVersion();

		// preprocess path
		List<String> split = splitPath(alias);
		alias = String.join(SEPERATOR, split);
		if (split.size() > MAX_PATH_LEVEL) {
			new AliasConflictException("Alias is too long.");
		}

		// loop over envelopes until empty slot or desired agent is found
		int trial = 1;
		String lookup = "";
		while (trial <= MAX_VERSIONS) {
			lookup = trial == 1 ? alias : alias + "-" + trial;
			try {
				String currentEntry = getEntry(lookup);
				if (currentEntry.equals(serviceName)) {
					// alias is already registered
					return;
				}
			} catch (EnvelopeException | CryptoException | AgentAccessDeniedException | SerializationException e) {
				// alias can be registered
				break;
			}
			trial ++;
		}
		
		if (trial == MAX_VERSIONS + 1) {
			throw new AliasConflictException("Maximum number of concurrent versions (" + trial + ") reached.");
		}

		// register prefixes as BLANK
		int level = 0;
		String currentKey = null;
		while (level < split.size() - 1) {
			// construct key
			if (currentKey == null) {
				currentKey = split.get(level);
			} else {
				currentKey += SEPERATOR + split.get(level);
			}

			String currentEntry = null;
			try {
				currentEntry = getEntry(currentKey);
			} catch (EnvelopeException | CryptoException | AgentAccessDeniedException | SerializationException e) {
			}

			if (currentEntry != null && !currentEntry.equals(BLANK)) {
				throw new AliasConflictException("A prefix of the given alias is already registered.");
			} else if (currentEntry == null) {
				try {
					createEntry(agent, currentKey, BLANK);
				} catch (IllegalArgumentException | EnvelopeException | SerializationException | CryptoException e) {
					throw new AliasConflictException("Storage error.", e);
				}
			}
			// else: there is already a BLANK, nothing to do

			// in the case of BLANK go one level deeper
			level++;
		}

		// register alias
		try {
			createEntry(agent, lookup, serviceName);
		} catch (IllegalArgumentException | EnvelopeException | SerializationException | CryptoException e) {
			throw new AliasConflictException("Storage error.", e);
		}
	}

	/**
	 * Resolves a path to multiple service aliases.
	 * 
	 * @param path the path
	 * @return the service name
	 * @throws AliasNotFoundException if the path cannot be resolves to a service name
	 */
	public List<AliasResolveResponse> resolvePathToServiceNames(String path) throws AliasNotFoundException {
		List<String> split = splitPath(path);
		LinkedList<AliasResolveResponse> result = new LinkedList<AliasResolveResponse>();
		
		int level = 0;
		String currentKey = null;
		while (level < split.size() && level < MAX_PATH_LEVEL) {
			// construct key
			if (currentKey == null) {
				currentKey = split.get(level);
			} else {
				currentKey += SEPERATOR + split.get(level);
			}

			// loop over envelopes until empty slot or desired agent is found
			int trial = 1;
			String lookup = "";
			while (trial <= MAX_VERSIONS) {
				lookup = trial == 1 ? currentKey : currentKey + "-" + trial;
				try {
					String currentEntry = getEntry(lookup);
					if (!currentEntry.equals(BLANK)) {
						result.add(new AliasResolveResponse(currentEntry, level+1));						
					} else {
						break;
					}
				} catch (EnvelopeException | CryptoException | AgentAccessDeniedException | SerializationException e) {
					break;
				}
				trial ++;
			}
			
			// in the case of BLANK go one level deeper
			level++;
		}
		
		return result;
	}
	
	private List<String> splitPath(String path) {
		path = path.toLowerCase().trim();

		ArrayList<String> pathSplit = new ArrayList<>(Arrays.asList(path.split(SEPERATOR)));
		pathSplit.removeIf(item -> item == null || "".equals(item));

		return pathSplit;
	}

	private String getEntry(String key) throws EnvelopeNotFoundException, EnvelopeException, CryptoException,
			AgentAccessDeniedException, SerializationException {
		EnvelopeVersion env = node.fetchEnvelope(PREFIX + key);
		String content = (String) env.getContent();
		return content;
	}
	
	/**
	 * Only use this for testing purposes!
	 * 
	 * @param agent
	 * @param key
	 * @param value
	 * @throws IllegalArgumentException
	 * @throws EnvelopeException
	 * @throws SerializationException
	 * @throws CryptoException
	 */
	@Deprecated
	public void createEntryForTest(AgentImpl agent, String key, String value) throws IllegalArgumentException, 
			EnvelopeException, SerializationException, CryptoException {
		this.createEntry(agent, key, value);
	}

	private void createEntry(AgentImpl agent, String key, String value) throws EnvelopeException,
			IllegalArgumentException, SerializationException, CryptoException {
		EnvelopeVersion envName = node.createUnencryptedEnvelope(PREFIX + key.toLowerCase(), agent.getPublicKey(),
				value);
		node.storeEnvelope(envName, agent);
	}
}
