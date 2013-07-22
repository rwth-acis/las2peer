package i5.las2peer.classLoaders;

/**
 * base class for class loader exceptions in the las2peer package
 * 
 * @author Holger Jan&szlig;en
 *
 */
public class ClassLoaderException extends Exception {

	private static final long serialVersionUID = 6831386965144581670L;

	protected ClassLoaderException ( String message, Throwable cause ) {
		super ( message, cause );
	}
	
	
	protected ClassLoaderException ( String message ) {
		super ( message );
	}
	
	
	protected ClassLoaderException ( Throwable cause ) {
		super ( cause );
	}
}
