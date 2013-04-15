package i5.las2peer.tools;

import i5.las2peer.tools.ColoredOutput.Color;

public class TestColoredOutput {

	public static void main ( String[] argv ) {

		ColoredOutput.bold();
		ColoredOutput.backgroundCyan();
		ColoredOutput.underline();
		ColoredOutput.fontLightBlue();
		
		System.out.println ( "some text");
	
		
		ColoredOutput.println ( "and some more", Color.Red);
		System.err.println ( "what abount std error?");

		
		ColoredOutput.useStream( System.err);
		ColoredOutput.fontGreen();
		System.err.println( "but now, it works");
				
	
		System.out.println ("\033[31mHello, World!\033[0m");	
	}
	
}
