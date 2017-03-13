package i5.las2peer.p2p;

import i5.las2peer.api.persistency.EnvelopeException;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.persistency.EnvelopeVersion;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.tools.CryptoException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Responsible for mapping service aliases to service names and resolving paths to service names.
 *
 */
public class ServiceAliasManager {

	private static final String PREFIX = "SERVICE_ALIAS-";
	private static final int MAX_PATH_LEVEL = 10;
	private static final String SEPERATOR = "/";
	private static final String BLANK = "BLANK";

	public class AliasResolveResponse {
		String serviceName;
		int numMatchedParts;

		public AliasResolveResponse(String serviceName, int numMatchedParts) {
			this.serviceName = serviceName;
			this.numMatchedParts = numMatchedParts;
		}

		public String getServiceName() {
			return this.serviceName;
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

		String serviceName = agent.getServiceNameVersion().getName();

		// preprocess path
		List<String> split = splitPath(alias);
		alias = String.join(SEPERATOR, split);
		if (split.size() > MAX_PATH_LEVEL) {
			new AliasConflictException("Alias is too long.");
		}

		// check for conflicts
		try {
			String currentEntry = getEntry(alias);
			if (!currentEntry.equals(serviceName)) { // if service name is not the same, it's an error
				throw new AliasConflictException("Alias has already been taken.");
			} else {
				return; // otherwise we're done
			}
		} catch (EnvelopeException | CryptoException | L2pSecurityException | SerializationException e) {
			// alias can be registered
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
			} catch (EnvelopeException | CryptoException | L2pSecurityException | SerializationException e) {
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
			createEntry(agent, alias, serviceName);
		} catch (IllegalArgumentException | EnvelopeException | SerializationException | CryptoException e) {
			throw new AliasConflictException("Storage error.", e);
		}
	}

	/**
	 * Resolves a path to a service alias.
	 * 
	 * @param path the path
	 * @return the service name
	 * @throws AliasNotFoundException if the path cannot be resolves to a service name
	 */
	public AliasResolveResponse resolvePathToServiceName(String path) throws AliasNotFoundException {
		List<String> split = splitPath(path);

		int level = 0;
		String currentKey = null;
		while (level < split.size() && level < MAX_PATH_LEVEL) {
			// construct key
			if (currentKey == null) {
				currentKey = split.get(level);
			} else {
				currentKey += SEPERATOR + split.get(level);
			}

			String currentEntry = null;
			try {
				currentEntry = getEntry(currentKey);
			} catch (EnvelopeException | CryptoException | L2pSecurityException | SerializationException e) {
				throw new AliasNotFoundException("Path does not exist.", e);
			}

			if (!currentEntry.equals(BLANK)) {
				return new AliasResolveResponse(currentEntry, level + 1);
			}

			// in the case of BLANK go one level deeper
			level++;
		}

		if (level == MAX_PATH_LEVEL) {
			throw new AliasNotFoundException("Given path is too long.");
		}

		throw new AliasNotFoundException("Given path does not fit any alias.");
	}

	private List<String> splitPath(String path) {
		path = path.toLowerCase().trim();

		ArrayList<String> pathSplit = new ArrayList<String>(Arrays.asList(path.split(SEPERATOR)));
		pathSplit.removeIf(item -> item == null || "".equals(item));

		return pathSplit;
	}

	private String getEntry(String key) throws EnvelopeNotFoundException, EnvelopeException, CryptoException,
			L2pSecurityException, SerializationException {
		EnvelopeVersion env = node.fetchEnvelope(PREFIX + key);
		String content = (String) env.getContent(node.getAnonymous());
		return content;
	}

	private void createEntry(AgentImpl agent, String key, String value) throws EnvelopeException,
			IllegalArgumentException, SerializationException, CryptoException {
		EnvelopeVersion envName = node.createEnvelope(PREFIX + key.toLowerCase(), value, agent, node.getAnonymous());
		node.storeEnvelope(envName, agent);
	}
}
