package i5.las2peer.communication;

import java.io.Serializable;

import i5.las2peer.api.p2p.ServiceNameVersion;

public class ServiceDiscoveryContent implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * indicates if its a request or a response
	 */
	private boolean request;

	/**
	 * indicates if an exact version match is required
	 */
	private boolean exact;

	/**
	 * the service agent's id
	 */
	private String agentId;

	/**
	 * the service's name and version
	 */
	private ServiceNameVersion service;

	/**
	 * creates a request
	 * 
	 * @param requestedService
	 * @param exact
	 */
	public ServiceDiscoveryContent(ServiceNameVersion requestedService, boolean exact) {
		this.request = true;
		this.service = requestedService;
		this.exact = exact;
	}

	/**
	 * creates a response
	 * 
	 * @param agentId
	 * @param serviceNameVersion
	 */
	public ServiceDiscoveryContent(String agentId, ServiceNameVersion serviceNameVersion) {
		this.agentId = agentId;
		this.request = false;
		this.service = serviceNameVersion;
	}

	/**
	 * check if its a request
	 * 
	 * @return Returns true if this is a request
	 */
	public boolean isRequest() {
		return request;
	}

	/**
	 * Gets the service agent id for this discovery
	 * 
	 * @return Returns the service agent id
	 */
	public String getAgentId() {
		return agentId;
	}

	/**
	 * Gets the service that is running
	 * 
	 * @return Returns the service name and version
	 */
	public ServiceNameVersion getService() {
		return service;
	}

	/**
	 * checks if the service is accepted by this request
	 * 
	 * @param service
	 * 
	 * @return Returns true if the service is accepted
	 */
	public boolean accepts(ServiceNameVersion service) {
		if (this.exact) {
			return this.service.equals(service);
		} else {
			return service.fits(this.service);
		}
	}

}
