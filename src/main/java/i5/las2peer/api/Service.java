package i5.las2peer.api;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import i5.las2peer.execution.L2pServiceException;
import i5.las2peer.execution.L2pThread;
import i5.las2peer.execution.NoSuchServiceException;
import i5.las2peer.execution.NoSuchServiceMethodException;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.TimeoutException;
import i5.las2peer.security.Agent;
import i5.las2peer.security.Context;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.ServiceAgent;

/**
 * Base class for services to be hosted within the LAS2peer network.
 * 
 * <h2>Basic Implementation Hints</h2>
 * 
 * <p>
 * To implement a service simply derive this API class an implement the intended functionality. If parameters and
 * results are to be transported via the LAS2peer network use types implementing the {@link java.io.Serializable}
 * interface.
 * 
 * <p>
 * Especially implemented helper classes like parameters and results to be transported via the network should be
 * encapsulated into a separate jar archive so that using remote clients or other services may use this jar for their
 * implementation.
 * 
 * <p>
 * Please be aware, that only one instance of the service is instantiated at a LAS2peer node. There are no per user
 * instantiations as in former LAS server implementations. To access the current user, just use the helper methods of
 * this abstract class like {@link #getActiveAgent()}.
 * 
 * <p>
 * If you want to access the current user agent, the LAS2peer node or logging from outside your service class, e.g. in
 * helper classes or the like, you can make use of the {@link i5.las2peer.security.Context} class, especially of the
 * static {@link i5.las2peer.security.Context#getCurrent} method.
 * 
 * <h2>Runtime Configuration</h2>
 * 
 * <p>
 * The preferred method of providing runtime configuration of your service are simple protected fields which can be
 * configured via property files. See {@link #setFieldValues} and {@link #getProperties} These methods won't be called
 * automatically. Use these methods e.g. in the constructor of your service class.
 * 
 * <h2>Deployment and Dependencies</h2>
 * 
 * <p>
 * For dependency information please use the following (more or less OSGI compatible) Jar manifest entries:
 * 
 * <pre>
 * Import-Library=[dependency list]
 * Library-SymbolicName=[an arbitrary name}
 * Library-Version=[build-version]
 * </pre>
 * 
 * <p>
 * For own helper and 3rd party libraries, you can use any arbitrary library name consisting of alpha-numerical
 * characters. For Jars archives providing a LAS2peer service please use the package name of the service as library name
 * and name for the jar archive.
 * 
 * <p>
 * As build version please use a format of [main].[mayor].[minor]-[build] where you may leave out any of [mayor],
 * [minor] or [build].<br>
 * The dependency list consists of a comma separated list of library dependencies of the following syntax:
 * 
 * <pre>
 * library-name[;version=[version-info]][;resolution=optional]
 * </pre>
 * 
 * <p>
 * The [version-info] may state a specific version consisting at least of a main version number or define a range of
 * compatible versions. For a range you can use square brackets indicating that the given version boundary (lower or
 * higher) is not compatible with the import. Round or no brackets state, that the given version is included in the
 * range.<br>
 * The statement <i>resolution=optional</i> defines a library, that may be used but is not mandatory.
 * 
 * <p>
 * Additionally, you may define a <i>Library-Name</i> entry in the jar-manifest stating a simple human readable name for
 * the provides service.
 * 
 * <h2>(JUnit-)Testing</h2>
 * 
 * <p>
 * For unit testing of your service within a LAS2peer setting, you can use the prepared (abstract) class
 * {@link i5.las2peer.testing.LocalServiceTestCase} to derive your test case from. <br>
 * This class starts a {@link i5.las2peer.p2p.LocalNode} running your service to test on each test case.
 * 
 * <h2>Starting and Hosting a Service</h2>
 * 
 * <p>
 * To start a node hosting your service, you can use the methods of the class {@link i5.las2peer.tools.L2pNodeLauncher},
 * which is the main method of the LAS2peer-archive library as well.
 * 
 * <h2>Further Tools</h2>
 * 
 * <p>
 * For further tools like (testing) envelope XML file generators or the like, have a look into the classes of the
 * {@link i5.las2peer.tools} package. There are e.g. some command line generators for XML helper files.
 * 
 * 
 * 
 *
 */
public abstract class Service extends Configurable {

	/**
	 * The node this service is currently running at.
	 */
	private Node runningAt = null;

	/**
	 * Executes a service method.
	 * 
	 * @param method
	 * 
	 * @return result of the method invocation
	 * 
	 * @throws SecurityException
	 * @throws NoSuchServiceMethodException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws L2pSecurityException
	 */
	public Object execute(String method) throws SecurityException, NoSuchServiceMethodException,
			IllegalArgumentException, IllegalAccessException, InvocationTargetException, L2pSecurityException {
		return execute(method, new Object[0]);
	}

	/**
	 * Executes a service method.
	 * 
	 * @param method
	 * @param parameters
	 * 
	 * @return result of the method invocation
	 * 
	 * @throws SecurityException
	 * @throws NoSuchServiceMethodException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws L2pSecurityException
	 */
	public Object execute(String method, Object... parameters) throws NoSuchServiceMethodException,
			IllegalArgumentException, IllegalAccessException, InvocationTargetException, L2pSecurityException {
		Method m = searchMethod(method, parameters);
		
		// TODO check if mainagent is unlocked
		
		return m.invoke(this, parameters);
	}

	/**
	 * Invokes the method of any other service (using the agent of the current service).
	 * 
	 * @param service
	 * @param method
	 * @param parameters
	 * 
	 * @return result of the method invocation
	 * 
	 * @throws InterruptedException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws NoSuchServiceMethodException
	 * @throws L2pSecurityException
	 * @throws AgentNotKnownException
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws L2pServiceException
	 * @throws TimeoutException
	 */
	public Object invokeServiceMethod(String service, String method, Serializable... parameters)
			throws L2pServiceException, SecurityException, IllegalArgumentException, AgentNotKnownException,
			L2pSecurityException, NoSuchServiceMethodException, IllegalAccessException, InvocationTargetException,
			InterruptedException, TimeoutException {
		return this.invokeServiceMethod(false,service,method,parameters);
	}
	
	/**
	 * Invokes the method of any other service.
	 * 
	 * @param sendMainAgent if true, the mainAgent of the current Context is used for invocation; otherwise, the service agent is used
	 * @param service the service class
	 * @param method the service method
	 * @param parameters list of parameters
	 * @return the return value of the invoked method
	 * @throws L2pServiceException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws AgentNotKnownException
	 * @throws L2pSecurityException
	 * @throws NoSuchServiceMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	public Object invokeServiceMethod(boolean sendMainAgent, String service, String method, Serializable... parameters)
			throws L2pServiceException, SecurityException, IllegalArgumentException, AgentNotKnownException,
			L2pSecurityException, NoSuchServiceMethodException, IllegalAccessException, InvocationTargetException,
			InterruptedException, TimeoutException {
		// System.out.println ( "\t\t---> invoking Service Method " + service + "/" + method );
		Agent callingAgent = null;
		if (sendMainAgent)
			callingAgent = getContext().getMainAgent();
		else
			callingAgent = getAgent();
		
		Object result = null;
		if (getContext().getLocalNode().hasLocalAgent(ServiceAgent.serviceClass2Id(service)))
			result = getContext().getLocalNode().invokeLocally(callingAgent.getId(), service, method, parameters);
		else
			result = getContext().getLocalNode().invokeGlobally(callingAgent, service, method, parameters);

		return result;
	}

	/**
	 * Searches the service method fitting to the given parameter classes.
	 *
	 *
	 * @param methodName
	 * @param params
	 *
	 * @return a Method
	 *
	 * @throws L2pSecurityException
	 * @throws i5.las2peer.execution.NoSuchServiceMethodException
	 *
	 */
	@SuppressWarnings("rawtypes")
	public Method searchMethod(String methodName, Object[] params)
			throws L2pSecurityException, i5.las2peer.execution.NoSuchServiceMethodException {
		Class[] acActualParamTypes = new Class[params.length];
		Class<? extends Service> thisClass = this.getClass();

		for (int i = 0; i < params.length; i++) {
			acActualParamTypes[i] = params[i].getClass();
		}

		Method found = null;

		try {
			found = thisClass.getMethod(methodName, acActualParamTypes);
		} catch (java.lang.NoSuchMethodException nsme) {
			// ok, simple test did not work - check for existing methods
			for (Method toCheck : thisClass.getMethods()) {

				if (toCheck.getName().equals(methodName)) {
					Class[] acCheckParamTypes = toCheck.getParameterTypes();
					if (acCheckParamTypes.length == acActualParamTypes.length) {
						boolean bPossible = true;
						for (int i = 0; i < acActualParamTypes.length && bPossible; i++) {
							if (!acCheckParamTypes[i].isInstance(params[i])) {
								// param[i] is not an instance of the formal parameter type
								if (!(acCheckParamTypes[i].isPrimitive()
										&& ServiceHelper.getWrapperClass(acCheckParamTypes[i]).isInstance(params[i]))
										&& !(ServiceHelper.isWrapperClass(acCheckParamTypes[i]) && ServiceHelper
												.getUnwrappedClass(acCheckParamTypes[i]).isInstance(params[i])))
									// and not wrapped or unwrapped either! -> so not more possibilities to match!
									bPossible = false;
							}
							// else is possible! -> check next param
						} // for ( all formal parameters)

						if (bPossible) {
							// all actual parameters match the formal ones!
							// maybe more than one matches
							if (found != null) {
								// find the more specific one
								for (int i = 0; i < acCheckParamTypes.length; i++) {
									if (acCheckParamTypes[i].equals(found.getParameterTypes()[i])) {
										// nothing to do!
									} else if (ServiceHelper.isSubclass(acCheckParamTypes[i],
											found.getParameterTypes()[i])) {
										// toCheck is more specific
										found = toCheck;
									} else if (ServiceHelper.isSubclass(found.getParameterTypes()[i],
											acCheckParamTypes[i])) {
										// found is more specific -> don't touch
										break;
									} else if (acCheckParamTypes[i].isInterface()) {
										// nothing to do (prefer classes instead of interfaces )
										break;
									} else if (found.getParameterTypes()[i].isInterface()) {
										// new one better (prefer classes instead of interfaces )
										found = toCheck;
									} // something to do with wrappers?
								}
							} else
								found = toCheck;
						}
					} // if ( parameter length matches)
				} // if ( method name fits )
			} // for (all known methods)
		}

		if (found == null)
			throw new NoSuchServiceMethodException(this.getClass().getCanonicalName(), methodName,
					getParameterString(params));

		if (Modifier.isStatic(found.getModifiers()))
			throw new NoSuchServiceMethodException(this.getClass().getCanonicalName(), methodName,
					getParameterString(params));

		return found;
	} // searchMethod

	/**
	 * Creates a string with all classes from an array of parameters.
	 * 
	 * @param params
	 * 
	 * @return a string describing a parameter list for the given actual parameters
	 */
	public static String getParameterString(Object[] params) {
		StringBuffer result = new StringBuffer("(");
		for (int i = 0; i < params.length - 1; i++)
			result.append(params[i].getClass().getCanonicalName()).append(", ");
		if (params.length > 0)
			result.append(params[params.length - 1].getClass().getCanonicalName());
		result.append(")");
		return result.toString();
	}

	/**
	 * Gets the agent corresponding to this service.
	 * 
	 * @return the agent responsible for this service
	 * 
	 * @throws AgentNotKnownException
	 */
	public final ServiceAgent getAgent() throws AgentNotKnownException {
		return runningAt.getServiceAgent(this.getClass().getCanonicalName());
	}

	/**
	 * Gets the current execution context.
	 * 
	 * @return the context we're currently running in
	 */
	public final Context getContext() {
		return getL2pThread().getContext();
	}

	/**
	 * Gets the current l2p thread.
	 * 
	 * @return the L2pThread we're currently running in
	 */
	public final L2pThread getL2pThread() { // TODO remove public modifier to avoid illegal access
		Thread t = Thread.currentThread();

		if (!(t instanceof L2pThread))
			throw new IllegalStateException("Not executed in a L2pThread environment!");

		return (L2pThread) t;
	}

	/**
	 * Notifies the service, that it has been launched at the given node.
	 * 
	 * simple startup hook that may be overwritten in subclasses
	 * 
	 * @param node
	 * @throws L2pServiceException
	 */
	public void launchedAt(Node node) throws L2pServiceException {
		runningAt = node;
		if (super.monitor) {
			try {
				runningAt.setServiceMonitoring(getAgent());
			} catch (AgentNotKnownException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Service " + this.getClass().getCanonicalName() + " has been started!");
	}

	/**
	 * Notifies the service, that it has been stopped at this node.
	 * 
	 * simple shutdown hook to be overwritten in subclasses
	 */
	public void close() {
		System.out.println("Service " + this.getClass().getCanonicalName() + " has been stopped!");
		runningAt = null;
	}

	/**
	 * @deprecated Use {@link L2pLogger#logEvent(Event, String)} with {@link Event#SERVICE_MESSAGE} instead!
	 *             <p>
	 *             Writes a log message.
	 * 
	 * @param message
	 */
	@Deprecated
	protected void logMessage(String message) {
		logMessage(0, null, message);
	}

	/**
	 * @deprecated Use {@link L2pLogger#logEvent(Event, Agent, String)} instead!
	 *             <p>
	 *             Writes a log message. The given index (1-99) can be used to differentiate between different log
	 *             messages.
	 * 
	 * @param index an index between 1 and 99
	 * @param actingUser can be set to null if unknown / not desired
	 * @param message
	 */
	@Deprecated
	protected void logMessage(int index, Agent actingUser, String message) {
		Event event = Event.SERVICE_MESSAGE; // Default
		if (index >= 1 && index <= 99) {
			event = Event.values()[Event.SERVICE_CUSTOM_MESSAGE_1.ordinal() + (index - 1)];
		}
		Agent serviceAgent = null;
		try {
			serviceAgent = this.getAgent();
		} catch (AgentNotKnownException e) {
			e.printStackTrace();
		}
		runningAt.observerNotice(event, this.getActiveNode().getNodeId(), serviceAgent, null, actingUser, message);
	}

	/**
	 * @deprecated Use {@link L2pLogger#logEvent(Event, String)} with {@link Event#SERVICE_ERROR} instead!
	 *             <p>
	 *             Writes an error log message.
	 * 
	 * @param message a custom message
	 */
	@Deprecated
	protected void logError(String message) {
		logError(0, null, message);
	}

	/**
	 * @deprecated Use {@link L2pLogger#logEvent(Event, Agent, String)} instead!
	 *             <p>
	 *             Writes an error message. The given index (1-99) can be used to differentiate between different log
	 *             messages.
	 * 
	 * @param index an index between 1 and 99
	 * @param actingUser can be set to null if unknown / not desired
	 * @param message
	 */
	@Deprecated
	protected void logError(int index, Agent actingUser, String message) {
		Event event = Event.SERVICE_ERROR; // Default
		if (index >= 1 && index <= 99) {
			event = Event.values()[Event.SERVICE_CUSTOM_ERROR_1.ordinal() + (index - 1)];
		}
		Agent serviceAgent = null;
		try {
			serviceAgent = this.getAgent();
		} catch (AgentNotKnownException e) {
			e.printStackTrace();
		}
		runningAt.observerNotice(event, this.getActiveNode().getNodeId(), serviceAgent, null, actingUser, message);
	}

	/**
	 * @deprecated Use {@link L2pLogger} instead!
	 *             <p>
	 *             Writes an exception log message Additionally the stack trace is printed.
	 * 
	 * @param e an exception
	 */
	@Deprecated
	protected void logError(Exception e) {
		logError("Exception: " + e);
		e.printStackTrace();
	}
	
	/**
	 * Gets the currently active l2p node (from the current thread context).
	 * 
	 * @return the currently active las2peer node
	 */
	protected Node getActiveNode() { // TODO deprecate
		return getL2pThread().getContext().getLocalNode();
	}
	
	/**
	 * Gets the currently active agent from the current thread context.
	 * 
	 * @return the agent currently executing the L2pThread we're in
	 */
	protected Agent getActiveAgent() { // TODO deprecate
		return getL2pThread().getContext().getMainAgent();
	}
	
	/**
	 * Access to this service agent. (security problem: just for internal use!)
	 * 
	 * @return the service agent responsible for this service
	 */
	protected ServiceAgent getMyAgent() { // TODO deprecate
		return getL2pThread().getServiceAgent();
	}

	/**
	 * Invokes a service method using the agent of this service as executing entity.
	 * 
	 * @param service
	 * @param method
	 * @param parameters
	 * 
	 * @return result of the method invocation
	 * 
	 * @throws L2pServiceException
	 * @throws L2pSecurityException
	 * @throws AgentNotKnownException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	protected Serializable invokeInternally(String service, String method, Serializable[] parameters) // TODO deprecate
			throws L2pServiceException, L2pSecurityException, AgentNotKnownException, InterruptedException,
			TimeoutException {
		try {
			return getActiveNode().invokeLocally(getMyAgent().getId(), service, method, parameters);
		} catch (NoSuchServiceException e) {
			return getActiveNode().invokeGlobally(getMyAgent(), service, method, parameters);
		}
	}

}
