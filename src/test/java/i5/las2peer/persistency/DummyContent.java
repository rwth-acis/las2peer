package i5.las2peer.persistency;

import java.io.Serializable;

public class DummyContent implements Serializable {

	private static final long serialVersionUID = -3366599243320374013L;
	private StringBuffer contained = null;

	public DummyContent(String string) {
		contained = new StringBuffer(string);
	}

	public void append(String add) {
		contained = contained.append(add);
		System.out.println(contained);
	}

	public String getContent() {
		return contained.toString();
	}

	@Override
	public String toString() {
		return getContent();
	}

}
