package i5.las2peer.mobsos;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;


/**
 * Simple stream bases implementation of a {@link NodeObserver}, e.g. for creating log files
 * For id based log file names, it is possible to buffer a set of log messages and given the name of
 * the log file afterwards. 
 * 
 * This is useful, e.g. because on pastry nodes, the first event will occur before the id of the node is set 
 * (by the pastry implementation itself)
 * 
 * @author Holger Jan&szlig;en
 *
 */
public class NodeStreamLogger extends NodeObserver {

	/**
	 * the PrintStream to write the log entries to 
	 */
	private PrintStream printer ;
	
	/**
	 * simple date formatter for the time stamps of each log entry
	 */
	private DateFormat dateFormat = DateFormat.getDateTimeInstance();
	
	/**
	 * a buffer for log entries occurring before the actual log filename was known.
	 */
	private StringBuffer pending = new StringBuffer ();
	
	
	/**
	 * flag, if this log has been closed
	 */
	private boolean isClosed = false;
	
	/**
	 * create a logger, first logging to a string buffer an writing everything 
	 * after setting a log file name
	 * 
	 * this method may be necessary e.g. for a {@link i5.las2peer.p2p.PastryNodeImpl}, which gets its
	 * node id after joining the p2p network and not at startup as the simple {@link i5.las2peer.p2p.LocalNode}
	 * implementation of a {@link i5.las2peer.p2p.Node}  
	 */
	public NodeStreamLogger () {
		this.printer = null;
	}
	
	
	/**
	 * create a logger for the given log file 
	 * 
	 * @param filename
	 * @throws FileNotFoundException
	 */
	public NodeStreamLogger ( String filename ) throws FileNotFoundException {
		this ( new FileOutputStream(filename, true));
	}
	
	/**
	 * create a logger for the given file descriptor
	 * @param file
	 * @throws FileNotFoundException
	 */
	public NodeStreamLogger ( File file ) throws FileNotFoundException {
		this ( new FileOutputStream ( file, true));
	}
	
	/**
	 * create a logger for the given output stream
	 * @param output
	 */
	public NodeStreamLogger ( OutputStream output ) {
		this ( new PrintStream ( output ));
	}
	
	/**
	 * create a logger for the given PrintStream (e.g. System.out)
	 * @param printer
	 */
	public NodeStreamLogger ( PrintStream printer ) {
		this.printer = printer;
		
		printer.println();
		
		printHeader();
	}
	
	
	/**
	 * If no logfile or outputstream was given at constructor phase, every log message will be logged
	 * to a stringbuffer. This method may be used to specify a log file afterwards.
	 * 
	 *  All buffered log entries will be written directly to this file. 
	 *  
	 * @param filename
	 * @throws FileNotFoundException
	 */
	public void setOutputFile ( String filename ) throws FileNotFoundException {
		if ( printer != null )
			throw new IllegalStateException("alread set an output stream!");
		
		printer = new PrintStream ( new FileOutputStream ( filename, true ) );
		
		printHeader();
		
		printer.print(pending);
		pending = null;
	}
	
	/**
	 * close this log
	 * @throws IOException
	 */
	public void close() throws IOException {
		this.isClosed = true;
		printer.close();
	}
	
	/**
	 * print a header to this log
	 */
	public void printHeader () {
		if ( isClosed )
			throw new IllegalStateException ( "This log is closed!");
		
		printer.println(
			"timestamp\ttimespan\tevent code\tsource node\tsource agent\torigin node\torigin agent\tadditional remarks\n"+
			"---------\t--------\t----------\t-----------\t------------\t-----------\t------------\t------------------"		
		);
	}
	
	@Override
	protected void writeLog(long timestamp, long timespan, Event e,
			String sourceNode, Long sourceAgentId, String originNode,
			Long originAgentId, String remarks) {
		
		if ( isClosed )
			throw new IllegalStateException ( "This log is closed!");
		
		StringBuffer logLine = new StringBuffer (  dateFormat.format ( new Date(timestamp) ) + "\t" );
		
		if ( timespan != -1 )
			logLine.append ( timespan ).append("\t");
		else
			logLine.append ( appendPart(null));
		
		logLine.append( e + " (" + e.getCode() + ")\t" );
		
	
		logLine.append ( appendPart ( sourceNode ));			
		logLine.append ( appendPart ( sourceAgentId ));			
		logLine.append ( appendPart ( originNode ));			
		
		logLine.append ( appendPart ( originAgentId ));			
		logLine.append ( appendPart ( remarks ));
		
		
		if ( printer == null)
			pending.append ( logLine + "\n");
		else
			printer.println(logLine);
	}
	
	/**
	 * simple method for one log line entry -- null will be printed as "-"
	 * all values will be followed by a tab char
	 * @param o
	 * @return
	 */
	private static String appendPart ( Object o ) {
		if ( o == null)
			return "-\t";
		else
			return "" + o + "\t";
	}

	
	
}
