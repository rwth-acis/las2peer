package i5.las2peer.testing;

import i5.las2peer.tools.SimpleTools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;


/**
 * A simple command line generator for setup directories for the {@link L2pNodeLauncher}.
 *  
 * The last node is always interactive.
 * 
 * @author Holger Jan&szlig;en
 *
 */
public class LaunchDirGeneratorService {

	
	public static void writeNodeFile ( String name, String bootstrap, int port, String[] methods ) throws IOException {
		FileOutputStream fos = new FileOutputStream ( name ) ;
		PrintStream print = new PrintStream ( fos );
	
		print.println ( port );
		print.println ( bootstrap );
		print.println ( SimpleTools.join(methods,  "\n"));
		
		fos.close();
		
		System.out.println ( "Written node " + name );
	}

	
	
	public static void writeDir ( int nodes, int servIn ) throws IOException {
		String dirName = "" + nodes + "/" +  servIn;
		
		if ( ! new File ( ""+nodes).isDirectory())
			new File ( "" + nodes ).mkdir();
		if ( ! new File ( ""+nodes + "/" + servIn).isDirectory())
			new File ( ""+nodes + "/" + servIn ).mkdir();
				
		String bootstrapPrefix = "icarus.local";
		int startPort = 9000;
		
		File dir = new File ( dirName );
		if ( ! dir.exists() )
			dir.mkdir();
		
		DecimalFormat df =   new DecimalFormat  ( "0000" );
		
		
		
		writeNodeFile ( "" + dirName + "/node-0000.node", "NEW", startPort, new String[]{"uploadAgents"});

		String bootstrap = bootstrapPrefix + ":" + startPort;
		
		String[] methods = new String[]{"waitALittle", "startTestService"};
		for (int i=1;i<=servIn; i++)
			writeNodeFile ( "" + dirName + "/node-" + df.format(i) + ".node", bootstrap, startPort+i, methods);

		int size = nodes / 20+2;
		methods = new String [size+1];
		for ( int i=0; i<size; i++) methods[i] = "waitALittle";
		methods[size] = "checkTestService(\""+nodes+"\",\""+servIn+"\")";
		
		for ( int i=servIn+1; i<nodes-1; i++ ) 
			writeNodeFile ( "" + dirName + "/node-" + df.format(i) + ".node", bootstrap, startPort+i, methods);
		
		String[] finalMethods = new String[methods.length+2];
		System.arraycopy(methods, 0,  finalMethods,  0,  methods.length);
		finalMethods[methods.length]= "waitFinished (\"" + (nodes-1) + "\")";
		finalMethods[methods.length+1] = "killAll";
		writeNodeFile ( "" + dirName + "/node-" + df.format(nodes-1) + ".node", bootstrap, startPort+nodes-1, finalMethods);		
	}
	
	
	
	public static void main ( String [] argv ) throws IOException {
		int[] nodes = new int[] { 10, 25, 50, 75, 100, 150, 200, 300, 400, 500, 750, 1000 };
		int[] servInis = new int [] {1, 2, 5, 10};
		
		for ( int cnt: nodes )
			for ( int inis: servInis )
				writeDir (cnt, inis);
	}
	
	
}
