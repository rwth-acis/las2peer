package i5.las2peer.restMapper;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;

import com.fasterxml.jackson.core.JsonProcessingException;

import i5.las2peer.api.Context;
import i5.las2peer.api.Service;
import i5.las2peer.api.security.Agent;
import i5.las2peer.api.security.UserAgent;
import i5.las2peer.restMapper.annotations.ServicePath;
import io.swagger.jaxrs.Reader;
import io.swagger.models.Swagger;
import io.swagger.util.Json;

/**
 * Base class for RESTful services.
 * 
 * Provides Jersey and Swagger integration.
 * 
 */
public abstract class RESTService extends Service {

	private Application application;

	private volatile ApplicationHandler appHandler;

	/**
	 * Creates a new REST service using a Jersey ResourceConfig as application.
	 * 
	 * @throws IllegalStateException if no resources have been set up in {@link #initResources()}.
	 */
	public RESTService() {
		super();
		initResources();
		if (this.application == null) {
			throw new IllegalStateException("No resources configured!");
		}
		this.appHandler = new ApplicationHandler(this.application);
	}

	/**
	 * Register resources here using {@link #getResourceConfig()} or set a custom JAX-RS Application using
	 * {@link #setApplication(Application)}.
	 * 
	 * Resources cannot be set up afterwards.
	 */
	protected abstract void initResources();

	/**
	 * Sets a Jersey's ResourceConfig as JAX-RS Application if no application is set and returns it.
	 * 
	 * Use this to add resources to a RESTful service.
	 * 
	 * @return the resource configuration
	 * 
	 * @throws IllegalStateException if a custom Application has been set using {@link #setApplication(Application)}.
	 */
	protected ResourceConfig getResourceConfig() {
		if (this.application == null) {
			ResourceConfig resourceConfig = new ResourceConfig();
			resourceConfig.setClassLoader(this.getClass().getClassLoader());
			resourceConfig.register(JacksonFeature.class);
			resourceConfig.property("jersey.config.server.wadl.disableWadl", true);
			this.application = resourceConfig;
		}

		if (!(this.application instanceof ResourceConfig)) {
			throw new IllegalStateException("The Application is not a ResourceConfig!");
		}

		return (ResourceConfig) this.application;
	}

	/**
	 * Set a custom JAX-RS Application. Can only be called once.
	 * 
	 * @param application the JAX-RS Application
	 * 
	 * @throws IllegalStateException if the Application has already been set up using
	 *             {@link #setApplication(Application)} or {@link #getResourceConfig()}.
	 */
	protected void setApplication(Application application) {
		if (this.application != null) {
			throw new IllegalStateException("The Application can only be set once!");
		}

		this.application = application;
	}

	/**
	 * Returns the JAX-RS application.
	 * 
	 * @return the JAX-RS Application
	 */
	protected Application getApplication() {
		return this.application;
	}

	/**
	 * Handles a REST call and passes it to Jersey.
	 * 
	 * Intended for RMI calls only.
	 * 
	 * @param baseUri base URI where the service was called
	 * @param requestUri full request URI
	 * @param method HTTP method
	 * @param body request body
	 * @param headers HTTP request headers
	 * @return a RESTResponse wrapping the output from Jersey
	 */
	public final RESTResponse handle(URI baseUri, URI requestUri, String method, byte[] body,
			Map<String, List<String>> headers) {

		final ResponseWriter responseWriter = new ResponseWriter();

		final ContainerRequest requestContext = new ContainerRequest(baseUri, requestUri, method, getSecurityContext(),
				new MapPropertiesDelegate());
		requestContext.setEntityStream(new ByteArrayInputStream(body));
		requestContext.getHeaders().putAll(headers);
		requestContext.setWriter(responseWriter);

		try {
			appHandler.handle(requestContext);
		} finally {
			responseWriter.commit();
		}

		return responseWriter.getResponse();
	}

	private SecurityContext getSecurityContext() {
		Context c;
		try {
			c = Context.getCurrent();
		} catch (IllegalStateException e) { // for unit tests
			c = null;
		}
		final Context context = c;

		return new SecurityContext() {
			@Override
			public boolean isUserInRole(final String role) {
				if (getUserPrincipal() == null) { // unauthenticated / anonymous
					return false;
				} else if (getUserPrincipal().getName() == null) { // bot
					return role.equals("authenticated_bot") || role.equals("authenticated");
				} else { // authenticated user
					return role.equals("authenticated_user") || role.equals("authenticated");
				}
			}

			@Override
			public boolean isSecure() {
				return true;
			}

			@Override
			public Principal getUserPrincipal() {
				if (context == null) {
					return null;
				}

				// treat anonymous as unauthenticated
				if (context.getLocalNode().getAnonymous().equals(context.getMainAgent())) {
					return null;
				} else {
					return new Principal() {
						@Override
						public String getName() {
							// only users
							Agent agent = context.getMainAgent();
							if (!(agent instanceof UserAgent)) {
								return null;
							}

							UserAgent userAgent = (UserAgent) agent;
							return userAgent.getLoginName();
						}
					};
				}
			}

			@Override
			public String getAuthenticationScheme() {
				return null;
			}
		};
	}

	/**
	 * Gets the Swagger documentation for this service.
	 * 
	 * Intended for RMI calls only.
	 * 
	 * @return A JSON serialized Swagger object
	 * @throws JsonProcessingException thrown by Swagger
	 */
	public final String getSwagger() throws JsonProcessingException {
		Swagger swagger = new Reader(new Swagger()).read(this.application.getClasses());
		return Json.mapper().writeValueAsString(swagger);
	}

	/**
	 * Gets the alias from the service class' {@link ServicePath} annotation.
	 */
	@Override
	public final String getAlias() {
		try {
			String pathPrefix = null;
			for (Annotation classAnnotation : this.getClass().getAnnotations()) {
				if (classAnnotation instanceof ServicePath) {
					pathPrefix = ((ServicePath) classAnnotation).value();
					break;
				}
			}

			if (pathPrefix == null) {
				throw new Exception("ServicePath annotation for service class is required!");
			}

			pathPrefix = pathPrefix.trim();
			pathPrefix = pathPrefix.replaceAll("(^/)|(/$)", "");

			if (pathPrefix.length() == 0) {
				throw new Exception("ServicePath annotation for service class is required!");
			}

			return pathPrefix;
		} catch (Exception e) {
		}
		return null;
	}
}
