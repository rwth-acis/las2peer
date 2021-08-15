package i5.las2peer.registry;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;

import org.web3j.tx.FastRawTransactionManager;

import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.p2p.Node;

import i5.las2peer.persistency.EnvelopeVersion;
import i5.las2peer.security.UserAgentImpl;

class StaticNonce {
    private static ConcurrentHashMap<String, BigInteger> staticNonces = new ConcurrentHashMap<>();
    private static StaticNonce instance = null;
    private static Node node;
    private StaticNonceRawTransactionManager txMan;
    private static UserAgentImpl newAgent;

    // singleton pattern for global nonce access
    public static StaticNonce Manager(Node nodee) {
        if (instance == null) {
            node = nodee;
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
            System.out.println("feeeetcchh envv ggeeeet");
            agentEnvelope = node.fetchEnvelope(address, 300000);
            System.out.println("ccooonnteeeenntt envv   ggeeeet");

            System.out.println(agentEnvelope.toString());
            System.out.println(agentEnvelope.getContent());
            System.out.println(agentEnvelope.isEncrypted());
            System.out.println(agentEnvelope.getIdentifier());
            parentNonce = BigInteger.valueOf(Long.valueOf(agentEnvelope.getContent().toString()));
            // System.out.println("creaaaatete nnnewww envv");
            // agentEnvelope = node.createUnencryptedEnvelope(agentEnvelope, 21321);
        } catch (Exception e) {
            System.out.println("feetch noott foundd envv    ggeeeet");
            System.out.println(e);
            try {
                System.out.println("txMantxMantxMantxMan");
                System.out.println(txMan);
                System.out.println(parentNonce);

                parentNonce = txMan.getNonceParent();
            } catch (Exception ex) {

                System.out.println("eeerror ssupper");

                System.out.println(ex);

            }
        }
        return parentNonce;
        // if (!staticNonces.containsKey(address)) {
        // staticNonces.put(address, BigInteger.valueOf(-1));
        // return BigInteger.valueOf(-1);
        // } else {
        // return staticNonces.get(address);
        // }
    }

    public synchronized BigInteger putStaticNonce(String key, BigInteger value) {
        return staticNonces.put(key, value);
    }

    public synchronized BigInteger putStaticNonceIfAbsent(String key, BigInteger value) {
        return staticNonces.putIfAbsent(key, value);
    }

    public synchronized BigInteger incStaticNonce(String key, StaticNonceRawTransactionManager txMan) {
        try {
            System.out.println("geeeeet ususer");

            String agentid = node.getAgentIdForLogin("loginkookkokokookak");
            System.out.println("geeeeet bbyy naammmee");
            System.out.println(agentid);

            newAgent = (UserAgentImpl) node.getAgent(agentid);
            newAgent.unlock("sss");
            System.out.println(newAgent.toString());

        } catch (Exception e) {
            try {
                System.out.println("eeeeroor geeting uuuse");
                System.out.println(e);
                newAgent = UserAgentImpl.createUserAgent("sss");
                newAgent.unlock("sss");
                System.out.println("creaate neeww");
                newAgent.setLoginName("loginkookkokokookak");
                System.out.println("seeet naame");
                System.out.println(newAgent.toString());

                node.storeAgent(newAgent);
            } catch (Exception ee) {
                System.out.println("eeeerorooorororo ssavvigng usuuususuer");
                System.out.println(ee);
            }
        }
        BigInteger currVal = staticNonces.get(key);
        BigInteger incVal = currVal.add(BigInteger.ONE);

        BigInteger pastryNonce = getStaticNonce(key, txMan);
        System.out.println("ppaarernnt nooonce iiss");
        System.out.println(pastryNonce.toString());
        BigInteger incVal2 = pastryNonce.add(BigInteger.ONE);
        System.out.println(incVal2.toString());

        EnvelopeVersion agentEnvelope = null;
        try {
            newAgent.unlock("sss");

        } catch (Exception e) {
            System.out.println("errrror creating user agent");
            System.out.println(e);
        }
        try {
            System.out.println("feeeetcchh envv");
            agentEnvelope = node.fetchEnvelope(key, 300000);
            System.out.println("ccooonnteeeenntt envv");

            System.out.println(agentEnvelope.toString());
            System.out.println(agentEnvelope.getContent());
            System.out.println(agentEnvelope.isEncrypted());
            System.out.println(agentEnvelope.getIdentifier());

            System.out.println("creaaaatete nnnewww envv");
            agentEnvelope = node.createUnencryptedEnvelope(agentEnvelope, incVal2);
        } catch (Exception e) {
            System.out.println("feetch noott foundd envv");
            System.out.println(e);

            try {
                System.out.println("creaaaatete neewww envv");

                agentEnvelope = node.createUnencryptedEnvelope(key, newAgent.getPublicKey(), incVal2);

            } catch (Exception ee) {

                System.out.println("eeeeexxceeption ooho o");
                System.out.println(ee);

            }
        }
        try {
            System.out.println(" eenv iiiddentnfi");
            System.out.println(agentEnvelope.getIdentifier());
            System.out.println(agentEnvelope.getContent());
            System.out.println(newAgent.getIdentifier());
            System.out.println(newAgent.getPublicKey());
            System.out.println(newAgent.createSignature());
            System.out.println(newAgent.getRunningAtNode());
            System.out.println("sssaaaavee eenv");
            node.storeEnvelope(agentEnvelope, newAgent);

        } catch (Exception e) {
            System.out.println("fffrrrrip");
            System.out.println(e);
        }

        staticNonces.put(key, incVal);
        return incVal;
    }

}