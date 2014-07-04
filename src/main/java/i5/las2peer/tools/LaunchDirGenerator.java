package i5.las2peer.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.Vector;


/**
 * A simple command line generator for setup directories for the {@link L2pNodeLauncher}.
 *  
 * The last node is always interactive. 
 * 
 * 
 *
 */
public class LaunchDirGenerator {

	
	public static void writeNodeFile ( String name, String bootstrap, int port, String[] methods ) throws IOException {
		FileOutputStream fos = new FileOutputStream ( name ) ;
		PrintStream print = new PrintStream ( fos );
	
		print.println ( port );
		print.println ( bootstrap );
		print.println ( SimpleTools.join(methods,  "\n"));
		
		fos.close();
		
		System.out.println ( "Written node " + name );
	}
	
	
	private static Random r = new Random ();
	
	public static String[] createCommands ( int size, int cnt, int randRange) {
		Vector<String> result = new Vector<String> ();
		
		int rand = r.nextInt (randRange);
		
		for ( int i=0; i<rand; i++)
			result.add ( "waitALittle");
		result.add ( "storeRandoms (\""+cnt+"\", \""+size+"\")");
		result.add ( "waitALittle");
		
		return result.toArray ( new String[0]);
	}
	
	
	public static void writeDir ( int nodes, int contentSize, int contentPerNode ) throws IOException {
		String dirName = "" + nodes + "/" +  contentSize + "/" + contentPerNode;
		
		if ( ! new File ( ""+nodes).isDirectory())
			new File ( "" + nodes ).mkdir();
		if ( ! new File ( ""+nodes + "/" + contentSize).isDirectory())
			new File ( ""+nodes + "/" + contentSize ).mkdir();
		if ( ! new File ( ""+nodes + "/" + contentSize + "/" + contentPerNode).isDirectory())
			new File ( ""+nodes + "/" + contentSize + "/" + contentPerNode ).mkdir();
				
		int randRange =   5 + (nodes * contentPerNode  / 200/50) * 20; 
		
		
		String bootstrapPrefix = "elika.local";
		int startPort = 9000;
		
		File dir = new File ( dirName );
		if ( ! dir.exists() )
			dir.mkdir();
		
		DecimalFormat df =   new DecimalFormat  ( "0000" );
		
		
		String[] methods = createCommands (contentSize, contentPerNode, randRange);
		writeNodeFile ( "" + dirName + "/node-0000.node", "NEW", startPort, methods );
		
		for ( int i=1; i<nodes-1; i++) {
			methods = createCommands (contentSize, contentPerNode, randRange);
			writeNodeFile ( "" + dirName + "/node-" + df.format(i) + ".node", bootstrapPrefix+":" + startPort, startPort + i, methods );
		}

		
		methods = createCommands (contentSize, contentPerNode, randRange);
		String[] finalMethods = new String[ methods.length+5 ];
		System.arraycopy(methods, 0, finalMethods, 0, methods.length);
		finalMethods[methods.length]= "waitFinished ( \""+(nodes-1)+"\")"; 
		finalMethods[methods.length+1]= "waitALittle"; 
		finalMethods[methods.length+2]= "waitALittle"; 
		finalMethods[methods.length+3]= "fetchRandoms(\"40\",\""+nodes+"\",\""+contentPerNode+"\",\"report_"+nodes+"_"+contentSize + "_" + contentPerNode+".txt\")";
		finalMethods[methods.length+4]= "killAll";
		
	
		writeNodeFile ( "" + dir + "/node-" + df.format(nodes-1) + ".node", bootstrapPrefix+":" + startPort, startPort + nodes, finalMethods );		
	}
	
	
	
	public static void main ( String [] argv ) throws IOException {
		int[] nodes = new int[] { 10, 25, 50, 75, 100, 150, 200, 300, 400, 500 };
		int[] sizes = new int [] {1000, 10000, 50000, 100000};
		int[] perNodes = new int[] {10, 20, 30, 40, 50};
		
		for ( int cnt: nodes )
			for ( int size: sizes )
				for ( int nrCon: perNodes )
					writeDir (cnt, size, nrCon);
	}
	
	
}
