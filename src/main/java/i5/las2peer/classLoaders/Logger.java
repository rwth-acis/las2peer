package i5.las2peer.classLoaders;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class Logger {

	private static PrintStream logStream = null;
	
	static {
		String logFile = System.getenv ( "CL_LOGFILE");
		if (logFile != null && !logFile.equals("")) {
			try {
				logStream = new PrintStream ( new FileOutputStream ( logFile ));
				
				System.out.println("Logging classloading to " + logFile);
			} catch (FileNotFoundException e) {
				System.out.println ( "Error opening cl logfile: " + e );
			}
		}
	}
	
	
	static void logFinding ( ClassLoader who, String classname, Boolean success ) {
		if ( logStream != null ) {
			String whoString = getWhoString ( who );
			
			logStream.println ("f\t" + success + "\t" + whoString + "\t" + classname );
		}
			
	}
	
	static void logMessage ( ClassLoader who, String classname, String message ) {
		if ( logStream != null ) {
			String whoString = getWhoString ( who );
			
			logStream.println ("f\t" + "\t" + whoString + "\t" + classname + "\t" + message );
		}
			
	}
	
	
	static void logLoading ( ClassLoader who, String classname, Boolean success, Object add ) { 
		if ( logStream != null ) {
			String whoString = getWhoString ( who );
			
			logStream.println ("l\t" + success + "\t" + whoString + "\t" + classname  + "\t" + add);
		}
	}
	
	
	static void logSubLibrary ( ClassLoader who, LibraryClassLoader libraryLoader ) {
		if ( logStream != null ) {
			String whoString = getWhoString ( who );
			logStream.println( "library load\t" + whoString + "\t" + libraryLoader.getLibrary().getIdentifier());
		}
	}
	
	
	private static String getWhoString ( ClassLoader cl  ) {
		if ( cl instanceof LibraryClassLoader )
			return "lcl" + "\t" + ((LibraryClassLoader) cl).getLibrary().getIdentifier();
		else if ( cl instanceof BundleClassLoader )
			return "bcl" + "\t" + ((BundleClassLoader) cl ).getMainLibraryIdentifier();
		else if ( cl instanceof L2pClassLoader )
			return "l2p-main"+"\t\t";
		else
			return "unkown";
	}
	
	
	
	
}
