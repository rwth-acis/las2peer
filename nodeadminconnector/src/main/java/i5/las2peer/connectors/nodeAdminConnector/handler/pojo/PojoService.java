package i5.las2peer.connectors.nodeAdminConnector.handler.pojo;

public class PojoService {

	private final String name;
	private final String version;
	private final String swagger;
	private final String frontend;

	public PojoService(String name, String version) {
		this(name, version, "", "");
	}

	public PojoService(String name, String version, String swagger, String frontend) {
		this.name = name;
		this.version = version;
		this.swagger = swagger;
		this.frontend = frontend;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public String getSwagger() {
		return swagger;
	}

	public String getFrontend() {
		return frontend;
	}

}
