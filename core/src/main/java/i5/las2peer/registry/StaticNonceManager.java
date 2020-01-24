package i5.las2peer.registry;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;

class StaticNonce
{
    private static ConcurrentHashMap<String, BigInteger> staticNonces = new ConcurrentHashMap<>();
    private static StaticNonce instance = null;
    // singleton pattern for global nonce access
    public static StaticNonce Manager()
    {
        if ( instance == null ) 
        {
            instance = new StaticNonce();
        }
        return instance;
    }
    private StaticNonce()
    {
        
    }

    // put if absent used to init all values which have been requested but not sent yet to -1
    public BigInteger getStaticNonce(String address) {
        if (!staticNonces.containsKey(address))
        {
            staticNonces.put(address, BigInteger.valueOf(-1));
            return BigInteger.valueOf(-1);
        }
        else
        {
            return staticNonces.get(address);
        }
    }

    public BigInteger putStaticNonce(String key, BigInteger value)
    {
        return staticNonces.put(key, value);
    }

    public BigInteger putStaticNonceIfAbsent(String key, BigInteger value) 
    {
        return staticNonces.putIfAbsent(key, value);
    }

    public BigInteger incStaticNonce(String key)
    {
        BigInteger currVal = staticNonces.get(key);
        BigInteger incVal  = currVal.add(BigInteger.ONE);

        staticNonces.put(key, incVal);
        return incVal;
    }

}