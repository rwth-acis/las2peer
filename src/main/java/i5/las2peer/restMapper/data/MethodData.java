package i5.las2peer.restMapper.data;

import i5.las2peer.restMapper.RESTMapper;

/**
 * Saves important method data
 *
 */
public class MethodData {

	private String name = "";
	private String[] consumes;
	private String[] produces;
	private Class<?> type;
	private ParameterData[] parameters;

	/**
	 * constructor
	 * 
	 * @param name name of the method
	 * @param type return type of the method
	 * @param consumes
	 * @param produces
	 * @param parameters array of method parameter information
	 * @throws ClassNotFoundException
	 */
	public MethodData(String name, String type, String[] consumes, String[] produces, ParameterData[] parameters)
			throws ClassNotFoundException {
		this.name = name;

		this.type = RESTMapper.getClassType(type);
		for (int i = 0; i < consumes.length; i++) {
			consumes[i] = consumes[i].trim();// more robust
		}
		this.consumes = consumes;
		if (parameters == null)
			this.parameters = new ParameterData[] {};
		else
			this.parameters = parameters;

		this.produces = produces;
	}

	/**
	 * returns method name useful for e.g. HashMap keys
	 */
	@Override
	public String toString() {
		return name;
	}

	/**
	 * 
	 * @return method name
	 */
	public String getName() {
		return name;
	}

	/**
	 * 
	 * @return return type of method
	 */
	public Class<?> getType() {
		return type;
	}

	/**
	 * 
	 * @return array of parameter information
	 */
	public ParameterData[] getParameters() {
		return parameters;
	}

	/**
	 *
	 * @return array of accepted MIME-Types
	 */
	public String[] getConsumes() {
		return consumes;
	}

	/**
	 *
	 * @return created MIME-Type
	 */
	public String[] getProduces() {
		return produces;
	}

}