package i5.las2peer.registry.data;

import java.math.BigInteger;

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
	private String from;
	private String input;
	private String to;

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

    public String toString() 
    {
        return "Transaction "+
            "["
                +transactionIndex+
                "|"
                +nonce+
            "] @ block #"
            +blockNumber+
            ": \n" +
        "> FROM: " + from + ", \n" +
        "> TO: " + to + ", \n" +
        "> GAS: " + gas + ", GAS PRICE: " + gasPrice + ", \n" +
        "> INPUT: " + input;
    }

}