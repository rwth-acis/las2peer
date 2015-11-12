package i5.las2peer.api;

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
	@SuppressWarnings("rawtypes")
	public static Class getWrapperClass(Class c) {
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
	@SuppressWarnings("rawtypes")
	public static boolean isWrapperClass(Class c) {
		return c.equals(Integer.class)
				|| c.equals(Byte.class)
				|| c.equals(Character.class)
				|| c.equals(Boolean.class)
				|| c.equals(Long.class)
				|| c.equals(Float.class)
				|| c.equals(Double.class);
	}

	/**
	 * check, if the first given class is a subclass of the second one
	 * 
	 * @param subClass
	 * @param superClass
	 * 
	 * @return true, if the first parameter class is a subclass of the second one
	 */
	@SuppressWarnings("rawtypes")
	public static boolean isSubclass(Class subClass, Class superClass) {
		Class walk = subClass;
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
	@SuppressWarnings("rawtypes")
	public static Class getUnwrappedClass(Class c) {
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

}
