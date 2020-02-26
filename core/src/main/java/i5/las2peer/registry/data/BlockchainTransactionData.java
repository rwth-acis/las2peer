package i5.las2peer.registry.data;

import java.math.BigInteger;

import org.web3j.protocol.core.methods.response.Transaction;

import i5.las2peer.registry.Util;

/**
 * Represents the raw ethereum blockchain transaction
 * @see https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_gettransactionbyhash
 */
public class BlockchainTransactionData {
	private BigInteger blockNumber;
	private BigInteger gas;
	private BigInteger gasPrice;
	private BigInteger nonce;
	private BigInteger transactionIndex;
    private BigInteger value;
	private String from;
	private String input;
    private String to;

    private BigInteger blockTimeStamp;

    public BlockchainTransactionData(
        BigInteger blockNumber,
        BigInteger gas,
        BigInteger gasPrice,
        BigInteger nonce,
        BigInteger transactionIndex,
        String from,
        String input,
        String to
    ) {
        this.blockNumber = blockNumber;
        this.gas = gas;
        this.gasPrice = gasPrice;
        this.nonce = nonce;
        this.transactionIndex = transactionIndex;
        this.from = from;
        this.input = input;
        this.to = to;       
    }

    public BlockchainTransactionData( Transaction t )
    {
        this.blockNumber = Util.getOrDefault( t.getBlockNumber(), BigInteger.ZERO );
        this.gas = Util.getOrDefault( t.getGas(), BigInteger.ZERO );
        this.gasPrice = Util.getOrDefault( t.getGasPrice(), BigInteger.ZERO );
        this.nonce = Util.getOrDefault( t.getNonce(), BigInteger.ZERO );
        this.transactionIndex = Util.getOrDefault( t.getTransactionIndex(), BigInteger.ZERO );
        this.from = Util.getOrDefault( t.getFrom(), "" );
        this.to = Util.getOrDefault( t.getTo(), "" );
        this.input = Util.getOrDefault( t.getInput(), "" );
        this.value = Util.getOrDefault( t.getValue(), BigInteger.ZERO );
    }
    
    public BigInteger getBlockNumber() 
    {
        return this.blockNumber;
    }
    
    public BigInteger getGas() 
    {
        return this.gas;
    }
    
    public BigInteger getGasPrice() 
    {
        return this.gasPrice;
    }
    
    public BigInteger getNonce() 
    {
        return this.nonce;
    }
    
    public BigInteger getTransactionIndex() 
    {
        return this.transactionIndex;
    }
    
    public String getFrom() 
    {
        return this.from;
    }
    
    public String getInput() 
    {
        return this.input;
    }
    
    public String getTo() 
    {
        return this.to;
    }

    public BigInteger getBlockTimeStamp() {
        return blockTimeStamp;
    }

    public BigInteger getValue() {
        return value;
    }

    public void setBlockNumber( BigInteger blockNumber )
    {
        this.blockNumber = blockNumber;
    }
    public void setGas( BigInteger gas )
    {
        this.gas = gas;
    }
    public void setGasPrice( BigInteger gasPrice )
    {
        this.gasPrice = gasPrice;
    }
    public void setNonce( BigInteger nonce )
    {
        this.nonce = nonce;
    }
    public void setTransactionIndex( BigInteger transactionIndex )
    {
        this.transactionIndex = transactionIndex;
    }
    public void setFrom( String from )
    {
        this.from = from;
    }
    public void setInput( String input )
    {
        this.input = input;
    }
    public void setTo( String to )
    {
        this.to = to;
    }
    public void setBlockTimeStamp(BigInteger blockTimeStamp) {
        this.blockTimeStamp = blockTimeStamp;
    }
    public void setValue(BigInteger value) {
        this.value = value;
    }

    public String toString() 
    {
        StringBuilder sb = new StringBuilder("Transaction ");
        sb.append("[");
        if ( transactionIndex.compareTo(BigInteger.ZERO) > 0 )
            sb.append(" #: "+transactionIndex);
        if ( nonce.compareTo(BigInteger.ZERO) > 0) 
            sb.append(" nonce: "+nonce);
        if ( blockNumber.compareTo(BigInteger.ZERO) > 0 )
            sb.append(" @ block #"+blockNumber+"");
        sb.append("]: \n");
        
        if ( from != null && from.length() > 0 ) sb.append("> FROM: " + from + ", \n" );
        if ( to != null && to.length() > 0 ) sb.append("> TO: " + to + ", \n" );
        if ( value.compareTo(BigInteger.ZERO) > 0 ) sb.append("> VALUE: " + value + ", \n" );
        if ( gas.compareTo(BigInteger.ZERO) > 0 ) sb.append("> GAS: " + gas + ", \n" );
        if ( gasPrice.compareTo(BigInteger.ZERO) > 0 ) sb.append("> GAS PRICE: " + gasPrice + ", \n" );


        return sb.toString();
    }
}