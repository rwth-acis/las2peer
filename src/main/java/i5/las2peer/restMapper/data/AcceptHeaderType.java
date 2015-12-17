package i5.las2peer.restMapper.data;

/**
 * @author Alexander
 */
public class AcceptHeaderType {
	private String type;
	private float qvalue;

	public String getType() {
		return type;
	}

	public float getQvalue() {
		return qvalue;
	}

	public AcceptHeaderType(String type, float qvalue) {
		this.type = type;

		this.qvalue = qvalue;
	}

	/**
	 *
	 * @return int value which indicates how specific a type is (higher=more specific)
	 */
	public int getSpecificLevel() {
		int wildcards = type.length() - type.replace("*", "").length();
		int parameters = type.length() - type.replace(";", "").length();
		return parameters - wildcards;
	}
}
