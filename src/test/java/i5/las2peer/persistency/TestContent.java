package i5.las2peer.persistency;

import java.io.Serializable;

public class TestContent implements Serializable {

	private static final long serialVersionUID = 1796830585973405485L;

	// @serial
	private String s;

	// @serial
	private int i;

	protected TestContent(String s, int i) {
		this.s = s;
		this.i = i;
	}

	protected String getString() {
		return s;
	}

	protected int getInt() {
		return i;
	}

}
