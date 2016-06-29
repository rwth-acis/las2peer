package i5.las2peer.communication;

import i5.las2peer.p2p.ServiceNameVersion;

import java.io.Serializable;

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
	private long agentId;

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
	public ServiceDiscoveryContent(long agentId, ServiceNameVersion serviceNameVersion) {
		this.agentId = agentId;
		this.request = false;
		this.service = serviceNameVersion;
	}

	/**
	 * check if its a request
	 * 
	 * @return
	 */
	public boolean isRequest() {
		return request;
	}

	/**
	 * service agent id
	 * 
	 * @return
	 */
	public long getAgentId() {
		return agentId;
	}

	/**
	 * the service that is running
	 * 
	 * @return
	 */
	public ServiceNameVersion getService() {
		return service;
	}

	/**
	 * checks if the service is accepted by this request
	 * 
	 * @param service
	 * 
	 * @return
	 */
	public boolean accepts(ServiceNameVersion service) {
		if (this.exact)
			return this.service.equals(service);
		else
			return service.fits(this.service);
	}

}
