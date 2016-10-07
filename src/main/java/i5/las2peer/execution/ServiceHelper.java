package i5.las2peer.execution;

import i5.las2peer.api.Service;
import i5.las2peer.api.execution.ServiceMethodNotFoundException;
import i5.las2peer.security.L2pSecurityException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * static helper methods for invocation of service methods via reflection
 * 
 * 
 *
 */
public abstract class ServiceHelper {

	/**
	 * returns the wrapper class for a native class
	 *
	 * @param c a Class
	 *
	 * @return a Class
	 *
	 */
	public static Class<?> getWrapperClass(Class<?> c) {
		if (c.equals(int.class))
			return Integer.class;
		else if (c.equals(byte.class))
			return Byte.class;
		else if (c.equals(char.class))
			return Character.class;
		else if (c.equals(boolean.class))
			return Boolean.class;
		else if (c.equals(long.class))
			return Long.class;
		else if (c.equals(float.class))
			return Float.class;
		else if (c.equals(double.class))
			return Double.class;
		else
			throw new IllegalArgumentException(c.getName() + " is not a native class!");
	}

	/**
	 * checks if the given class is a wrapper class for a java native
	 *
	 * @param c a Class
	 *
	 * @return a boolean
	 *
	 */
	public static boolean isWrapperClass(Class<?> c) {
		return c.equals(Integer.class) || c.equals(Byte.class) || c.equals(Character.class) || c.equals(Boolean.class)
				|| c.equals(Long.class) || c.equals(Float.class) || c.equals(Double.class);
	}

	/**
	 * check, if the first given class is a subclass of the second one
	 * 
	 * @param subClass
	 * @param superClass
	 * @return true, if the first parameter class is a subclass of the second one
	 */
	public static boolean isSubclass(Class<?> subClass, Class<?> superClass) {
		Class<?> walk = subClass;
		while (walk != null) {
			if (walk.equals(superClass))
				return true;
			walk = walk.getSuperclass();
		}
		return false;
	}

	/**
	 * returns the native class for a wrapper
	 *
	 * @param c a Class
	 *
	 * @return a Class
	 *
	 */
	public static Class<?> getUnwrappedClass(Class<?> c) {
		if (c.equals(Integer.class))
			return int.class;
		else if (c.equals(Byte.class))
			return byte.class;
		else if (c.equals(Character.class))
			return char.class;
		else if (c.equals(Boolean.class))
			return boolean.class;
		else if (c.equals(Long.class))
			return long.class;
		else if (c.equals(Float.class))
			return float.class;
		else if (c.equals(Double.class))
			return double.class;
		else
			throw new IllegalArgumentException(c.getName() + " is not a Wrapper class!");
	}

	/**
	 * Executes a service method.
	 * 
	 * @param service
	 * 
	 * @param method the service method
	 * @return result of the method invocation
	 * @throws SecurityException
	 * @throws ServiceMethodNotFoundException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws L2pSecurityException
	 */
	public static Object execute(Service service, String method) throws SecurityException,
			ServiceMethodNotFoundException, IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, L2pSecurityException {
		return execute(service, method, new Object[0]);
	}

	/**
	 * Executes a service method.
	 * 
	 * @param service
	 * 
	 * @param method the service method
	 * @param parameters
	 * @return result of the method invocation
	 * @throws SecurityException
	 * @throws ServiceMethodNotFoundException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws L2pSecurityException
	 */
	public static Object execute(Service service, String method, Object... parameters)
			throws ServiceMethodNotFoundException, IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, L2pSecurityException {
		Method m = searchMethod(service.getClass(), method, parameters);

		return m.invoke(service, parameters);
	}

	/**
	 * Searches the service method fitting to the given parameter classes.
	 * 
	 * @param serviceClass
	 *
	 * @param methodName the service method
	 * @param params
	 *
	 * @return a Method
	 *
	 * @throws L2pSecurityException
	 * @throws ServiceMethodNotFoundException
	 *
	 */
	public static Method searchMethod(Class<? extends Service> serviceClass, String methodName, Object[] params)
			throws L2pSecurityException, ServiceMethodNotFoundException {
		Class<?>[] acActualParamTypes = new Class[params.length];

		for (int i = 0; i < params.length; i++) {
			acActualParamTypes[i] = params[i].getClass();
		}

		Method found = null;

		try {
			found = serviceClass.getMethod(methodName, acActualParamTypes);
		} catch (java.lang.NoSuchMethodException nsme) {
			// ok, simple test did not work - check for existing methods
			for (Method toCheck : serviceClass.getMethods()) {

				if (toCheck.getName().equals(methodName)) {
					Class<?>[] acCheckParamTypes = toCheck.getParameterTypes();
					if (acCheckParamTypes.length == acActualParamTypes.length) {
						boolean bPossible = true;
						for (int i = 0; i < acActualParamTypes.length && bPossible; i++) {
							if (!acCheckParamTypes[i].isInstance(params[i])) {
								// param[i] is not an instance of the formal parameter type
								if (!(acCheckParamTypes[i].isPrimitive() && ServiceHelper.getWrapperClass(
										acCheckParamTypes[i]).isInstance(params[i]))
										&& !(ServiceHelper.isWrapperClass(acCheckParamTypes[i]) && ServiceHelper
												.getUnwrappedClass(acCheckParamTypes[i]).isInstance(params[i]))) {
									// and not wrapped or unwrapped either! -> so not more possibilities to match!
									bPossible = false;
								}
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
							} else {
								found = toCheck;
							}
						}
					} // if ( parameter length matches)
				} // if ( method name fits )
			} // for (all known methods)
		}

		if (found == null) {
			throw new ServiceMethodNotFoundException(serviceClass.getCanonicalName(), methodName,
					getParameterString(params));
		}

		if (Modifier.isStatic(found.getModifiers())) {
			throw new ServiceMethodNotFoundException(serviceClass.getCanonicalName(), methodName,
					getParameterString(params));
		}

		return found;
	} // searchMethod

	/**
	 * Creates a string with all classes from an array of parameters.
	 * 
	 * @param params
	 * @return a string describing a parameter list for the given actual parameters
	 */
	public static String getParameterString(Object[] params) {
		StringBuffer result = new StringBuffer("(");
		for (int i = 0; i < params.length - 1; i++) {
			result.append(params[i].getClass().getCanonicalName()).append(", ");
		}
		if (params.length > 0) {
			result.append(params[params.length - 1].getClass().getCanonicalName());
		}
		result.append(")");
		return result.toString();
	}

}
