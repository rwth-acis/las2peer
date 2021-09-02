package i5.las2peer.registry;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;

import org.web3j.tx.FastRawTransactionManager;

import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.p2p.Node;

import i5.las2peer.persistency.EnvelopeVersion;
import i5.las2peer.security.UserAgentImpl;

class StaticNonce {
    private static ConcurrentHashMap<String, BigInteger> staticNonces = new ConcurrentHashMap<>();
    private static StaticNonce instance = null;
    private static Node node;
    private StaticNonceRawTransactionManager txMan;
    private static UserAgentImpl newAgent;
    protected final L2pLogger logger = L2pLogger.getInstance(StaticNonce.class);

    // singleton pattern for global nonce access
    public static StaticNonce Manager(Node theNode) {
        if (instance == null) {
            node = theNode;
            instance = new StaticNonce();

        }
        return instance;
    }

    private StaticNonce() {

    }

    // put if absent used to init all values which have been requested but not sent
    // yet to -1
    public BigInteger getStaticNonce(String address, StaticNonceRawTransactionManager txMan) {
        EnvelopeVersion agentEnvelope = null;
        BigInteger parentNonce = BigInteger.valueOf(-1l);
        try {
            logger.fine("Fetching envelope for address " + address + "with current static nonce");
            agentEnvelope = node.fetchEnvelope(address, 300000);
            parentNonce = BigInteger.valueOf(Long.valueOf(agentEnvelope.getContent().toString()));
        } catch (Exception e) {
            logger.fine("Envelope not found for address " + address);
            logger.fine(e.getMessage());
            try {
                logger.fine("Parent nonce: " + parentNonce);

                parentNonce = txMan.getNonceParent();
            } catch (Exception ex) {

                logger.severe(ex.getMessage());

            }
        }
        return parentNonce;
    }

    public synchronized BigInteger putStaticNonce(String key, BigInteger value) {
        newAgent = getNonceEnvelopeAgent();
        staticNonces.put(key, value);

        return staticNonces.put(key, value);
    }

    public synchronized BigInteger putStaticNonceIfAbsent(String key, BigInteger value) {
        newAgent = getNonceEnvelopeAgent();
        staticNonces.put(key, value);

        return staticNonces.putIfAbsent(key, value);
    }

    private synchronized UserAgentImpl getNonceEnvelopeAgent() {
        try {
            logger.fine("Get agent handling envelopes for nonces");

            String agentid = node.getAgentIdForLogin("agentforenvelopes");

            newAgent = (UserAgentImpl) node.getAgent(agentid);
            newAgent.unlock("password");
            return newAgent;

        } catch (Exception e) {
            try {
                logger.fine("Error getting agent handling envelopes for nonces");
                newAgent = UserAgentImpl.createUserAgent("password");
                newAgent.unlock("password");

                logger.fine("Creating/Initializing new agent handling envelopes for nonces");

                newAgent.setLoginName("agentforenvelopes");
                node.storeAgent(newAgent);
                return newAgent;
            } catch (Exception ee) {
                logger.severe("Error creating agent");
                logger.severe(ee.getMessage());
            }
        }
        try {
            newAgent.unlock("password");

        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
        return newAgent;
    }

    private synchronized void saveEnvelopeWithNonceForAddress(String key, BigInteger incVal2) {
        EnvelopeVersion agentEnvelope = null;
        try {
            logger.fine("Fetching envelope with nonce for address " + key);
            agentEnvelope = node.fetchEnvelope(key, 300000);
            logger.fine("Updating envelope with nonce for address " + key);

            agentEnvelope = node.createUnencryptedEnvelope(agentEnvelope, incVal2);
        } catch (Exception e) {
            logger.fine("None nonce envelope for address " + key + " present");

            try {
                logger.fine("Creating new envelope for nonce for address " + key);

                agentEnvelope = node.createUnencryptedEnvelope(key, newAgent.getPublicKey(), incVal2);

            } catch (Exception ee) {
                logger.severe(ee.getMessage());

            }
        }
        try {

            logger.fine("Save updated envelope with updated nonce value of" + incVal2.toString());
            node.storeEnvelope(agentEnvelope, newAgent);

        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
    }

    public synchronized BigInteger incStaticNonce(String key, StaticNonceRawTransactionManager txMan) {
        newAgent = getNonceEnvelopeAgent();
        BigInteger currVal = staticNonces.get(key);
        BigInteger incVal = currVal.add(BigInteger.ONE);

        BigInteger pastryNonce = getStaticNonce(key, txMan);

        logger.fine("Parent nonce is" + pastryNonce.toString());

        BigInteger incVal2 = pastryNonce.add(BigInteger.ONE);
        logger.fine("Parent nonce increasing by 1");
        logger.fine("Parent nonce is now" + incVal2.toString());
        saveEnvelopeWithNonceForAddress(key, incVal2);

        staticNonces.put(key, incVal);
        return incVal;
    }

}