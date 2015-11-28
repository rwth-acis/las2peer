package i5.las2peer.tools;

import i5.las2peer.tools.ColoredOutput.BackgroundColor;
import i5.las2peer.tools.ColoredOutput.ForegroundColor;
import i5.las2peer.tools.ColoredOutput.Formatting;
import i5.las2peer.tools.ColoredOutput.ShellCode;

public class TestColoredOutput {

	public static void main(String[] argv) {
		ColoredOutput.allOn(); // default is turned off
		ShellCode[] codes = new ShellCode[] { Formatting.Bold, BackgroundColor.Cyan, Formatting.Underlined,
				ForegroundColor.LightBlue };
		String colored = ColoredOutput.colorize("some text", codes);
		System.out.println(colored);

		// try some color overloading
		codes = new ShellCode[] { Formatting.Bold, BackgroundColor.Cyan, Formatting.Underlined,
				ForegroundColor.LightBlue, ForegroundColor.Red };
		colored = ColoredOutput.colorize("and some more", codes);
		System.out.println(colored);
		System.err.println("what abount std error?");

		colored = ColoredOutput.colorize("but now, it works", ForegroundColor.Green);
		System.err.println(colored);

		System.out.println("\033[31mHello, World!\033[0m");

		System.out.println("all good");
	}

}
