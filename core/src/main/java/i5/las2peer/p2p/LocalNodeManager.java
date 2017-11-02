package i5.las2peer.p2p;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import i5.las2peer.api.security.AgentException;
import i5.las2peer.classLoaders.ClassManager;
import i5.las2peer.classLoaders.libraries.FileSystemRepository;
import i5.las2peer.classLoaders.policies.DefaultPolicy;
import i5.las2peer.communication.Message;
import i5.las2peer.persistency.LocalStorage;
import i5.las2peer.security.AgentImpl;

public class LocalNodeManager {

	public LocalNodeManager() {
		startPendingTimer();
	}

	/************************** factories ***************************************/

	/**
	 * 
	 * @return a new configured but not running node
	 */
	public LocalNode newNode() {
		return new LocalNode(this);
	}

	/**
	 * create a LocalNode using a FileSystemRepository at the given location
	 * 
	 * @param fileSystemRepository a path to the service directory
	 * @return Returns a LocalNode instance
	 */
	public LocalNode newNode(String fileSystemRepository) {
		return new LocalNode(this, new ClassManager(new FileSystemRepository(fileSystemRepository),
				LocalNode.class.getClassLoader(), new DefaultPolicy()));
	}

	/**
	 * create a LocalNode using a FileSystemRepository at the given locations
	 * 
	 * @param fileSystemRepositories a path to the service directories
	 * @return Returns a LocalNode instance
	 */
	public LocalNode newNode(String[] fileSystemRepositories) {
		return new LocalNode(this, new ClassManager(new FileSystemRepository(fileSystemRepositories),
				LocalNode.class.getClassLoader(), new DefaultPolicy()));
	}

	/**
	 * factory: launch a node
	 * 
	 * @return a freshly started node
	 */
	public LocalNode launchNode() {
		LocalNode result = newNode();
		try {
			result.launch();
		} catch (NodeException e) {
		}
		return result;
	}

	/**
	 * factory: launch a node an register the given agent
	 * 
	 * @param a An agent to launch
	 * @return a freshly started node hosting the given agent
	 * @throws AgentException If agent registration fails
	 */
	public LocalNode launchAgent(AgentImpl a) throws AgentException {
		LocalNode result = launchNode();
		try {
			result.registerReceiver(a);
		} catch (AgentAlreadyRegisteredException e) {
			// should not occur with a freshly generated node
		}
		return result;
	}

	/****************************** static *****************************************/

	private Hashtable<Long, LocalNode> htLocalNodes = new Hashtable<>();

	private LocalStorage storage = new LocalStorage();

	private Hashtable<String, Hashtable<Message, MessageResultListener>> htPendingMessages = new Hashtable<>();

	/**
	 * Hashtable with string representations of all known agents
	 */
	private Hashtable<String, String> htKnownAgents = new Hashtable<>();

	/**
	 * register a node for later use
	 * 
	 * @param node A node to register to
	 */
	public void registerNode(LocalNode node) {
		synchronized (htLocalNodes) {
			htLocalNodes.put(node.getNodeId(), node);
		}
	}

	/**
	 * remove a node from the central storage
	 * 
	 * @param node A node to register to
	 */
	public void unregisterNode(LocalNode node) {
		synchronized (htLocalNodes) {
			htLocalNodes.remove(node.getNodeId());
		}
	}

	/**
	 * get a node from the central storage
	 * 
	 * @param id A node id
	 * @return the node with the given id
	 */
	public LocalNode getNode(long id) {
		synchronized (htLocalNodes) {
			return htLocalNodes.get(id);
		}
	}

	/**
	 * does the given node exist in the central storage?
	 * 
	 * @param id A node id
	 * @return true, if a node of the given it is known to the registry
	 */
	public boolean hasNode(long id) {
		synchronized (htLocalNodes) {
			return getNode(id) != null;
		}
	}

	/**
	 * do a complete restart of all nodes, artifacts and messages
	 */
	public void reset() {
		htPendingMessages = new Hashtable<>();
		storage = new LocalStorage();
		htKnownAgents = new Hashtable<>();
		htLocalNodes = new Hashtable<>();

		iMessageMinWait = DEFAULT_MESSAGE_MIN_WAIT;
		iMessageMaxWait = DEFAULT_MESSAGE_MAX_WAIT;
		lPendingTimeout = DEFAULT_PENDING_TIMEOUT;

		stopCleaner();

		startPendingTimer();
	}

	/**
	 * stop the timeout cleaner thread
	 */
	public void stopCleaner() {
		synchronized (pendingTimer) {
			if (pendingTimerTask != null) {
				pendingTimerTask.cancel();
				pendingTimerTask = null;
			}
		}
	}

	/**
	 * find the first node, where the given agent is registered to
	 * 
	 * @param agentId An agent id
	 * @return id of a node hosting the given agent
	 * @throws AgentNotRegisteredException If the agent is not registered at any node
	 */
	public long findFirstNodeWithAgent(String agentId) throws AgentNotRegisteredException {
		synchronized (htLocalNodes) {

			for (long nodeId : htLocalNodes.keySet()) {
				if (htLocalNodes.get(nodeId).hasLocalAgent(agentId)) {
					return nodeId;
				}
			}

			throw new AgentNotRegisteredException(agentId);
		}
	}

	/**
	 * get the ids of all nodes where the given agent is running
	 * 
	 * @param agentId An agent id
	 * @return array with all ids of nodes hosting the given agent
	 */
	public Long[] findAllNodesWithAgent(String agentId) {
		synchronized (htLocalNodes) {
			HashSet<Long> hsResult = new HashSet<>();

			for (long nodeId : htLocalNodes.keySet()) {
				if (htLocalNodes.get(nodeId).hasLocalAgent(agentId)) {
					hsResult.add(nodeId);
				}
			}

			return hsResult.toArray(new Long[0]);
		}
	}

	/**
	 * get the ids of all nodes where agents listening to the given topic are running
	 * 
	 * @param topicId A topic id
	 * @return Returns a list with all node ids for the given topic
	 */
	public Long[] findAllNodesWithTopic(long topicId) {
		synchronized (htLocalNodes) {
			HashSet<Long> hsResult = new HashSet<>();

			for (long nodeId : htLocalNodes.keySet()) {
				if (htLocalNodes.get(nodeId).hasTopic(topicId)) {
					hsResult.add(nodeId);
				}
			}

			return hsResult.toArray(new Long[0]);
		}
	}

	/**
	 * store messages for agents not known to this "network" of nodes
	 * 
	 * @param message A message to store
	 * @param listener A message result listener to add the message
	 */
	protected void storeMessage(Message message, MessageResultListener listener) {
		synchronized (htPendingMessages) {
			Hashtable<Message, MessageResultListener> pending = htPendingMessages.get(message.getRecipientId());
			if (pending == null) {
				pending = new Hashtable<>();
				htPendingMessages.put(message.getRecipientId(), pending);
			}

			pending.put(message, listener);
		}
	}

	/**
	 * fetch all pending messages for the given agent
	 * 
	 * @param recipientId A recipient id to serve
	 * @param nodeId A node id to send to
	 */
	protected void deliverPendingMessages(String recipientId, long nodeId) {

		synchronized (htPendingMessages) {
			Hashtable<Message, MessageResultListener> pending = htPendingMessages.get(recipientId);

			if (pending != null) {
				for (Message m : pending.keySet()) {
					System.out.println("send pending message..." + m.getId());
					localSendMessage(nodeId, m);
				}

				htPendingMessages.remove(recipientId);
			}
		}

	}

	/**
	 * get all expired messages and notify their senders
	 */
	protected void notifExpiredMessages() {
		synchronized (htPendingMessages) {

			System.out.println("checking for expired messages");
			System.out.println("waiting for " + htPendingMessages.size() + " agents ");

			for (String agentId : htPendingMessages.keySet()) {
				Hashtable<Message, MessageResultListener> agentMessages = htPendingMessages.get(agentId);

				for (Message m : agentMessages.keySet()) {
					MessageResultListener mrl = agentMessages.get(m);
					if (mrl.checkTimeOut()) {
						System.out.println("message " + m.getId() + " is timed out!");
						agentMessages.remove(m);
					}
				}

				// remove agent entry, if empty
				if (agentMessages.size() == 0) {
					htPendingMessages.remove(agentId);
				}
			}
		}
	}

	private static final int DEFAULT_MESSAGE_MIN_WAIT = 500;
	private int iMessageMinWait = DEFAULT_MESSAGE_MIN_WAIT;
	private static final int DEFAULT_MESSAGE_MAX_WAIT = 550;
	private int iMessageMaxWait = DEFAULT_MESSAGE_MAX_WAIT;
	private static final long DEFAULT_PENDING_TIMEOUT = 20000; // 20 seconds
	private long lPendingTimeout = DEFAULT_PENDING_TIMEOUT;

	private final Timer pendingTimer = new Timer();
	private TimerTask pendingTimerTask;

	public void setPendingTimeOut(int newtimeout) {
		lPendingTimeout = newtimeout;
	}

	public int getMinMessageWait() {
		return iMessageMinWait;
	}

	public int getMaxMessageWait() {
		return iMessageMaxWait;
	}

	public void setMinMessageWait(int time) {
		iMessageMinWait = time;
	}

	public void setMaxMessageWait(int time) {
		iMessageMaxWait = time;
	}

	/**
	 * start a thread clearing up expired messages from time to time
	 */
	private void startPendingTimer() {
		synchronized (pendingTimer) {
			if (pendingTimerTask == null) {
				pendingTimerTask = new TimerTask() {
					@Override
					public void run() {
						notifExpiredMessages();
					}
				};
				pendingTimer.schedule(pendingTimerTask, 0, lPendingTimeout);
			}
		}
	}

	/**
	 * does the actual <i>sending</i> of a message in a separate thread with a configurable delay
	 * 
	 * @param nodeId A node id to send to
	 * @param message A message to send
	 */
	public void localSendMessage(final long nodeId, final Message message) {

		// it is important to close the message her,
		// since the recipient knows other versions of the involved agents
		message.close();

		new Thread(new Runnable() {
			@Override
			public void run() {
				Random r = new Random();

				int wait = iMessageMinWait + r.nextInt(iMessageMaxWait - iMessageMinWait + 1);
				try {
					Thread.sleep(wait);
				} catch (InterruptedException e1) {
				}

				try {
					getNode(nodeId).receiveMessage(message.clone());
				} catch (Exception e) {
					System.out.println("problems at node " + nodeId);
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
		}).start();
	}

	public Object[] getAllNodes() {
		return htLocalNodes.values().toArray();
	}

	public LocalStorage getStorage() {
		return storage;
	}

	public Hashtable<String, String> getKnownAgents() {
		return htKnownAgents;
	}

}
