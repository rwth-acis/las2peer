package i5.las2peer.tools;

import java.io.PrintStream;

/**
 * A simple static class providing access to the color features of the bash console on standard out.
 * 
 * All bash codes are printed in the form of \033[xxx;xxx;xxxm the methods sendCode may be used for that.
 * 
 * The println... methods write formatted text an reset the font afterwards.
 * 
 * The other methods switch the format of later written Text via System.out.
 * 
 * 
 * 
 * 
 *
 */
public class ColoredOutput {

	private static PrintStream useStream = System.out;

	private static boolean turnedOn = false;

	/**
	 * A color used by the {@link ColoredOutput} methods.
	 * 
	 *
	 */
	public enum Color {
		Black(30),
		Red(31),
		Green(32),
		Brown(33),
		Blue(34),
		Magenta(35),
		Cyan(36),
		LightGrey(37),

		DarkGrey(1030),
		LightRed(1031),
		LightGreen(1032),
		Yellow(1033),
		LightBlue(1034),
		LightMagenta(1035),
		LightCyan(1036),
		Wihte(1037);

		private int code;

		/**
		 * simle constructor for the code value assignment
		 * 
		 * @param c
		 */
		private Color(int c) {
			code = c;
		}

		/**
		 * get the code for a color
		 * 
		 * @return a integer color code
		 */
		public int getCode() {
			return code;
		}
	}

	/**
	 * switch the stream to use for output
	 * 
	 * @param stream
	 */
	public static void useStream(PrintStream stream) {
		useStream = stream;
	}

	/**
	 * write a bash code
	 * 
	 * @param code
	 */
	public static void sendCode(int code) {
		if (!turnedOn)
			return;

		print("\033[" + code + "m");
	}

	/**
	 * write a color code to the shell
	 * 
	 * @param c
	 */
	public static void sendCode(Color c) {
		if (!turnedOn)
			return;

		if (c.getCode() >= 1000)
			sendCode(1, c.getCode() - 1000);
		else
			sendCode(c.getCode());
	}

	/**
	 * write several bash codes at once
	 * 
	 * @param codes
	 */
	public static void sendCode(int... codes) {
		if (codes == null || codes.length == 0)
			throw new IllegalArgumentException();

		String printString = "\033[" + codes[0];

		for (int i = 1; i < codes.length; i++)
			printString += ";" + codes[i];

		printString += "m";

		print(printString);
	}

	/**
	 * simple reset the console to standard (colors and shape)
	 */
	public static void reset() {
		sendCode(0);
	}

	/**
	 * write blining text
	 */
	public static void blink() {
		sendCode(5);
	}

	/**
	 * turn off blinking text
	 */
	public static void noBlink() {
		sendCode(25);
	}

	/**
	 * write bold text
	 */
	public static void bold() {
		sendCode(1);
	}

	/**
	 * turn off bold text
	 */
	public static void noBold() {
		sendCode(22);
	}

	/**
	 * write underlined text
	 */
	public static void underline() {
		sendCode(4);
	}

	/**
	 * write inverted (swap back- and foreground color
	 */
	public static void inverted() {
		sendCode(7);
	}

	/**
	 * switch to black font color
	 */
	public static void fontBlack() {
		sendCode(Color.Black);
	}

	/**
	 * switch to red font color
	 */
	public static void fontRed() {
		sendCode(Color.Red);
	}

	/**
	 * switch to green font color
	 */
	public static void fontGreen() {
		sendCode(Color.Green);
	}

	/**
	 * switch to brown font color
	 */
	public static void fontBrown() {
		sendCode(Color.Brown);
	}

	/**
	 * switch to blue font color
	 */
	public static void fontBlue() {
		sendCode(Color.Blue);
	}

	/**
	 * switch to magenta font color
	 */
	public static void fontMagenta() {
		sendCode(Color.Magenta);
	}

	/**
	 * switch to cyan font color
	 */
	public static void fontCyan() {
		sendCode(Color.Cyan);
	}

	/**
	 * switch to light grey font color
	 */
	public static void fontLightGrey() {
		sendCode(Color.LightGrey);
	}

	/**
	 * switch to dark grey font color
	 */
	public static void fontDarkGrey() {
		sendCode(Color.DarkGrey);
	}

	/**
	 * switch to light red font color
	 */
	public static void fontLightRed() {
		sendCode(Color.LightRed);
	}

	/**
	 * switch to light green font color
	 */
	public static void fontLightGreen() {
		sendCode(Color.LightGreen);
	}

	/**
	 * switch to yellow font color
	 */
	public static void fontYellow() {
		sendCode(Color.Yellow);
	}

	/**
	 * switch to light blue font color
	 */
	public static void fontLightBlue() {
		sendCode(Color.LightBlue);
	}

	/**
	 * switch to light magenta font color
	 */
	public static void fontLightMagenta() {
		sendCode(Color.LightMagenta);
	}

	/**
	 * switch to lighty cyan font color
	 */
	public static void fontLightCyan() {
		sendCode(Color.LightCyan);
	}

	/**
	 * switch to white font color
	 */
	public static void fontWhite() {
		sendCode(1, 37);
	}

	/**
	 * reset to terminal default
	 */
	public static void fontReset() {
		sendCode(39);
	}

	/**
	 * switch background to black
	 */
	public static void backgroundBlack() {
		sendCode(40);
	}

	/**
	 * switch background to red
	 */
	public static void backgroundRed() {
		sendCode(41);
	}

	/**
	 * switch background to green
	 */
	public static void backgroundGreen() {
		sendCode(42);
	}

	/**
	 * switch background to yellow
	 */
	public static void backgroundYellow() {
		sendCode(43);
	}

	/**
	 * switch background to blue
	 */
	public static void backgroundBlue() {
		sendCode(44);
	}

	/**
	 * switch background to magenta
	 */
	public static void backgroundMagenta() {
		sendCode(45);
	}

	/**
	 * switch background to cyan
	 */
	public static void backgroundCyan() {
		sendCode(46);
	}

	/**
	 * switch background to white
	 */
	public static void backgroundWhite() {
		sendCode(47);
	}

	/**
	 * reset background to terminal default
	 */
	public static void resetBackground() {
		sendCode(49);
	}

	/**
	 * print a line of red text
	 * 
	 * @param text
	 */
	public static void printlnRed(String text) {
		println(text, Color.Red);
	}

	/**
	 * print a line of yellow text
	 * 
	 * @param text
	 */
	public static void printlnYellow(String text) {
		println(text, Color.Yellow);
	}

	/**
	 * print a line of bold text
	 * 
	 * @param text
	 */
	public static void printlnBold(String text) {
		bold();
		println(text);
		noBold();
	}

	/**
	 * print a text in the given color
	 * 
	 * @param text
	 * @param color
	 */
	public static void println(String text, Color color) {
		sendCode(color);
		println(text);
		reset();
	}

	/**
	 * print some text in the given color to the given printstream
	 * 
	 * @param text
	 * @param color
	 * @param stream
	 */
	public static void println(String text, Color color, PrintStream stream) {
		PrintStream old = useStream;
		useStream(stream);
		println(text, color);
		useStream(old);
	}

	/**
	 * print some text followed by a newline to the selected print stream
	 * 
	 * @param text
	 */
	public static void println(Object text) {
		useStream.println(text);
	}

	/**
	 * print some text to the selected printstream
	 * 
	 * @param text
	 */
	public static void print(Object text) {
		useStream.print(text);
	}

	/**
	 * switch off all color codes
	 */
	public static void allOff() {
		turnedOn = false;
	}

	/**
	 * switch on all color codes
	 */
	public static void allOn() {
		turnedOn = true;
	}

	/**
	 * ensure that the console font is reseted at the end or the application
	 */
	static {
		String disabled = System.getenv().get("COLOR_DISABLED");

		if (disabled != null && (disabled.equals("1") || disabled.toLowerCase().equals("true")))
			allOff();
		else {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					ColoredOutput.reset();
					if (!useStream.equals(System.out)) {
						useStream(System.out);
						ColoredOutput.reset();
					}

					// ColoredBash.resetBackground();
					System.out.println();
				}
			});
		}
	}
}
