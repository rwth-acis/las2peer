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
 */
public class ColoredOutput {

	private static PrintStream useStream = System.out;

	private static boolean turnedOn = false;

	public interface ShellCode {
		String getShellCode();
	}

	/**
	 * An enumeration of possible foreground (text) colors.
	 */
	public enum ForegroundColor implements ShellCode {
		Default(39),
		Black(30),
		Red(31),
		Green(32),
		Yellow(33),
		Blue(34),
		Magenta(35),
		Cyan(36),
		LightGrey(37),
		DarkGrey(90),
		LightRed(91),
		LightGreen(92),
		LightYellow(93),
		LightBlue(94),
		LightMagenta(95),
		LightCyan(96),
		White(97);

		private int code;

		private ForegroundColor(int code) {
			this.code = code;
		}

		public String getShellCode() {
			return String.valueOf(code);
		}
	}

	/**
	 * An enumeration of possible background colors.
	 */
	public enum BackgroundColor implements ShellCode {
		Default(49),
		Black(40),
		Red(41),
		LightRed(101),
		Green(42),
		LightGreen(102),
		Yellow(43),
		LightYellow(103),
		Blue(44),
		LightBlue(104),
		Magenta(45),
		LightMagenta(105),
		Cyan(46),
		LightCyan(106),
		LightGrey(47),
		DarkGrey(100),
		White(107);

		private int code;

		private BackgroundColor(int code) {
			this.code = code;
		}

		public String getShellCode() {
			return String.valueOf(code);
		}
	}

	/**
	 * An enumeration of possible text formatting options.
	 */
	public enum Formatting implements ShellCode {
		ResetAll(0),
		Bold(1),
		Dim(2),
		Underlined(3),
		Blink(4),
		Reverse(5),
		Hidden(6),
		ResetBold(21),
		ResetDim(22),
		ResetUnderlined(24),
		ResetBlink(25),
		ResetReverse(27),
		ResetHidden(28);

		private int code;

		private Formatting(int code) {
			this.code = code;
		}

		@Override
		public String getShellCode() {
			return String.valueOf(code);
		}
	}

	/**
	 * This method returns the String colored/formatted with the given shell codes.
	 * 
	 * @param input A {@link String} that should be colored/formatted.
	 * @param codes A bunch of {@link ShellCode}s that should be applied on the {@code input} {@link String}.
	 * @return Returns the colored/formatted String or the given {@code input} {@link String} if coloring ist disabled.
	 */
	public static String colorize(String input, ShellCode... codes) {
		if (!turnedOn) {
			return input;
		}
		StringBuilder sb = new StringBuilder("\033[");
		boolean first = true;
		for (ShellCode code : codes) {
			if (!first) {
				sb.append(";");
			}
			sb.append(code.getShellCode());
			first = false;
		}
		sb.append("m").append(input).append("\033[0m");
		return sb.toString();
	}

	/**
	 * @deprecated Use {@link ForegroundColor} instead!
	 *             <p>
	 *             A color used by the {@link ColoredOutput} methods.
	 */
	@Deprecated
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
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} instead!
	 *             <p>
	 *             switch the stream to use for output
	 * 
	 * @param stream
	 */
	@Deprecated
	public static void useStream(PrintStream stream) {
		useStream = stream;
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} instead!
	 *             <p>
	 *             write a bash code
	 * 
	 * @param code
	 */
	@Deprecated
	public static void sendCode(int code) {
		if (!turnedOn)
			return;

		print("\033[" + code + "m");
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} instead!
	 *             <p>
	 *             write a color code to the shell
	 * 
	 * @param c
	 */
	@Deprecated
	public static void sendCode(Color c) {
		if (!turnedOn)
			return;

		if (c.getCode() >= 1000)
			sendCode(1, c.getCode() - 1000);
		else
			sendCode(c.getCode());
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} instead!
	 *             <p>
	 *             write several bash codes at once
	 * 
	 * @param codes
	 */
	@Deprecated
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
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link Formatting#ResetAll} instead!
	 *             <p>
	 *             simple reset the console to standard (colors and shape)
	 */
	@Deprecated
	public static void reset() {
		sendCode(0);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link Formatting#Blink} instead!
	 *             <p>
	 *             write blinking text
	 */
	@Deprecated
	public static void blink() {
		sendCode(5);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link Formatting#ResetBlink} instead!
	 *             <p>
	 *             turn off blinking text
	 */
	@Deprecated
	public static void noBlink() {
		sendCode(25);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link Formatting#Bold} instead!
	 *             <p>
	 *             write bold text
	 */
	@Deprecated
	public static void bold() {
		sendCode(1);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link Formatting#ResetBold} instead!
	 *             <p>
	 *             turn off bold text
	 */
	@Deprecated
	public static void noBold() {
		sendCode(22);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link Formatting#Underlined} instead!
	 *             <p>
	 *             write underlined text
	 */
	@Deprecated
	public static void underline() {
		sendCode(4);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link Formatting#Reverse} instead!
	 *             <p>
	 *             write inverted (swap back- and foreground color)
	 */
	@Deprecated
	public static void inverted() {
		sendCode(7);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link ForegroundColor#Black} instead!
	 *             <p>
	 *             switch to black font color
	 */
	@Deprecated
	public static void fontBlack() {
		sendCode(Color.Black);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link ForegroundColor#Red} instead!
	 *             <p>
	 *             switch to red font color
	 */
	@Deprecated
	public static void fontRed() {
		sendCode(Color.Red);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link ForegroundColor#Green} instead!
	 *             <p>
	 *             switch to green font color
	 */
	@Deprecated
	public static void fontGreen() {
		sendCode(Color.Green);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} another {@link ForegroundColor} instead!
	 *             <p>
	 *             switch to brown font color
	 */
	@Deprecated
	public static void fontBrown() {
		sendCode(Color.Brown);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link ForegroundColor#Blue} instead!
	 *             <p>
	 *             switch to blue font color
	 */
	@Deprecated
	public static void fontBlue() {
		sendCode(Color.Blue);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link ForegroundColor#Magenta}
	 *             instead!
	 *             <p>
	 *             switch to magenta font color
	 */
	@Deprecated
	public static void fontMagenta() {
		sendCode(Color.Magenta);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link ForegroundColor#Cyan} instead!
	 *             <p>
	 *             switch to cyan font color
	 */
	@Deprecated
	public static void fontCyan() {
		sendCode(Color.Cyan);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link ForegroundColor#LightGrey}
	 *             instead!
	 *             <p>
	 *             switch to light grey font color
	 */
	@Deprecated
	public static void fontLightGrey() {
		sendCode(Color.LightGrey);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link ForegroundColor#DarkGrey}
	 *             instead!
	 *             <p>
	 *             switch to dark grey font color
	 */
	@Deprecated
	public static void fontDarkGrey() {
		sendCode(Color.DarkGrey);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link ForegroundColor#LightRed}
	 *             instead!
	 *             <p>
	 *             switch to light red font color
	 */
	@Deprecated
	public static void fontLightRed() {
		sendCode(Color.LightRed);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link ForegroundColor#LightGreen}
	 *             instead!
	 *             <p>
	 *             switch to light green font color
	 */
	@Deprecated
	public static void fontLightGreen() {
		sendCode(Color.LightGreen);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link ForegroundColor#Yellow} instead!
	 *             <p>
	 *             switch to yellow font color
	 */
	@Deprecated
	public static void fontYellow() {
		sendCode(Color.Yellow);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link ForegroundColor#LightBlue}
	 *             instead!
	 *             <p>
	 *             switch to light blue font color
	 */
	@Deprecated
	public static void fontLightBlue() {
		sendCode(Color.LightBlue);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link ForegroundColor#LightMagenta}
	 *             instead!
	 *             <p>
	 *             switch to light magenta font color
	 */
	@Deprecated
	public static void fontLightMagenta() {
		sendCode(Color.LightMagenta);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link ForegroundColor#LightCyan}
	 *             instead!
	 *             <p>
	 *             switch to lighty cyan font color
	 */
	@Deprecated
	public static void fontLightCyan() {
		sendCode(Color.LightCyan);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link ForegroundColor#White} instead!
	 *             <p>
	 *             switch to white font color
	 */
	@Deprecated
	public static void fontWhite() {
		sendCode(1, 37);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link ForegroundColor#Default}
	 *             instead!
	 *             <p>
	 *             reset to terminal default
	 */
	@Deprecated
	public static void fontReset() {
		sendCode(39);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link BackgroundColor#Black} instead!
	 *             <p>
	 *             switch background to black
	 */
	@Deprecated
	public static void backgroundBlack() {
		sendCode(40);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link BackgroundColor#Red} instead!
	 *             <p>
	 *             switch background to red
	 */
	@Deprecated
	public static void backgroundRed() {
		sendCode(41);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link BackgroundColor#Green} instead!
	 *             <p>
	 *             switch background to green
	 */
	@Deprecated
	public static void backgroundGreen() {
		sendCode(42);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link BackgroundColor#Yellow} instead!
	 *             <p>
	 *             switch background to yellow
	 */
	@Deprecated
	public static void backgroundYellow() {
		sendCode(43);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link BackgroundColor#Blue} instead!
	 *             <p>
	 *             switch background to blue
	 */
	@Deprecated
	public static void backgroundBlue() {
		sendCode(44);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link BackgroundColor#Magenta}
	 *             instead!
	 *             <p>
	 *             switch background to magenta
	 */
	@Deprecated
	public static void backgroundMagenta() {
		sendCode(45);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link BackgroundColor#Cyan} instead!
	 *             <p>
	 *             switch background to cyan
	 */
	@Deprecated
	public static void backgroundCyan() {
		sendCode(46);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link BackgroundColor#White} instead!
	 *             <p>
	 *             switch background to white
	 */
	@Deprecated
	public static void backgroundWhite() {
		sendCode(47);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link BackgroundColor#Default}
	 *             instead!
	 *             <p>
	 *             reset background to terminal default
	 */
	@Deprecated
	public static void resetBackground() {
		sendCode(49);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link ForegroundColor#Red} instead!
	 *             <p>
	 *             print a line of red text
	 * 
	 * @param text
	 */
	@Deprecated
	public static void printlnRed(String text) {
		println(text, Color.Red);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link ForegroundColor#Yellow} instead!
	 *             <p>
	 *             print a line of yellow text
	 * 
	 * @param text
	 */
	@Deprecated
	public static void printlnYellow(String text) {
		println(text, Color.Yellow);
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with {@link Formatting#Bold} instead!
	 *             <p>
	 *             print a line of bold text
	 * 
	 * @param text
	 */
	@Deprecated
	public static void printlnBold(String text) {
		bold();
		println(text);
		noBold();
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with a {@link ForegroundColor} instead!
	 *             <p>
	 *             print a text in the given color
	 * 
	 * @param text
	 * @param color
	 */
	@Deprecated
	public static void println(String text, Color color) {
		sendCode(color);
		println(text);
		reset();
	}

	/**
	 * @deprecated Use {@link ColoredOutput#colorize(String, ShellCode...)} with a {@link ForegroundColor} instead!
	 *             <p>
	 *             print some text in the given color to the given printstream
	 * 
	 * @param text
	 * @param color
	 * @param stream
	 */
	@Deprecated
	public static void println(String text, Color color, PrintStream stream) {
		PrintStream old = useStream;
		useStream(stream);
		println(text, color);
		useStream(old);
	}

	/**
	 * @deprecated This method is deprecated and will be removed in the future!
	 *             <p>
	 *             print some text followed by a newline to the selected print stream
	 * 
	 * @param text
	 */
	@Deprecated
	public static void println(Object text) {
		useStream.println(text);
	}

	/**
	 * @deprecated This method is deprecated and will be removed in the future!
	 *             <p>
	 *             print some text to the selected printstream
	 * 
	 * @param text
	 */
	@Deprecated
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

}
