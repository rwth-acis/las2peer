package i5.las2peer.evaluation;

import i5.las2peer.communication.Message;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.UserAgent;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import i5.las2peer.tools.SerializationException;

import java.util.Date;
import java.util.Random;

public class CryptoSymKeySize {

	
	public static final int minAsymSize = 1024+512;
	public static final int maxAsymSize = 8194;
	public static final int stepSize = 512;
	public static final int[] symSizes = new int[] { 128, 192, 256 };
	
	public static final int numberOfTests = 1;
	
	public static final int[] contentSizes = new int [] { 1024, 10*1024, 128*1024, 256*1024, 512*1024, 768*1024, 1024*1024};
	

	
	
	public static void main ( String [] argv ) throws EncodingFailedException, L2pSecurityException, SerializationException, CryptoException {
				
		// System.out.println( "no\tcontentSize\tsymSize\tasymSize\tduration"  );					
		
		// for init
		UserAgent.createUserAgent("test");
		
		int[] asymSizes = new int [ 15];
		int count = 0;
		for ( int asym = minAsymSize; asym<=maxAsymSize; asym+=stepSize) {
			asymSizes[count] = asym;
			count++;
		}
		
		long[][][][] result = new long[ symSizes.length ][asymSizes.length][contentSizes.length][numberOfTests];
		
		int max = symSizes.length * asymSizes.length * contentSizes.length * numberOfTests;
		count = 0;
		for ( int asym=0; asym<asymSizes.length; asym++ ) {	
			for ( int sym = 0; sym<symSizes.length; sym++ ) {
				CryptoTools.setSymmetricKeySize(symSizes[sym]);
				CryptoTools.setAsymmetricKeySize(asymSizes[asym]);
				UserAgent sender = UserAgent.createUserAgent("test");
				UserAgent receiver = UserAgent.createUserAgent ( "test2");
				sender.unlockPrivateKey("test");
				
				for ( int cont=0; cont<contentSizes.length; cont++ ) {
					int [] content = new int[contentSizes[cont]];
					Random rand = new Random();
					for ( int i=0; i<content.length; i++)
						content[i] = rand.nextInt();
					
					for ( int i=0; i<numberOfTests; i++) {
						count ++;
						long start = new Date().getTime();
					
						Message m = new Message ( sender, receiver, content);
						m.close();
						
						long end = new Date().getTime();
						
						result[sym][asym][cont][i] = end-start;
						
						System.err.println ( " " + count + " von " + max );
					}
	
				}
			}
		}
		
		
		// reports
		System.out.println ( "2048 asym, 512 sym");
		System.out.println ( "cont\t2048\t4096");
		
		for ( int i=0; i<contentSizes.length; i++ ) 
			for ( int j=0; j<numberOfTests; j++)
				System.out.println( "" + contentSizes[i] + "\t" + result[2][3][i][j] + "\t" + result[2][7][i][j] );
			
		System.out.println("\n\n\n");
		
		int cont = 3;
		System.out.println ( "content: " + contentSizes[cont]);
		System.out.println ( "asym\t128\t192\t256");
		for ( int i=0; i<asymSizes.length; i++ ) 
			for ( int j=0; j<numberOfTests; j++)
				System.out.println( "" + asymSizes[i] + "\t" + result[0][i][cont][j] + "\t" + result[1][i][cont][j] + "\t" + result[2][i][cont][j]  );
		
		cont = 6;
		System.out.println ( "content: " + contentSizes[cont]);
		System.out.println ( "asym\t128\t192\t256");
		for ( int i=0; i<asymSizes.length; i++ ) 
			for ( int j=0; j<numberOfTests; j++)
				System.out.println( "" + asymSizes[i] + "\t" + result[0][i][cont][j] + "\t" + result[1][i][cont][j] + "\t" + result[2][i][cont][j]  );
		
		
	}
	
	
	
	
}
