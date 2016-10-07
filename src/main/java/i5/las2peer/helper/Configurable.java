package i5.las2peer.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

/**
 * Base (abstract) super type for classes that may be configurable via property files.
 *
 * The basic idea is, that the runtime system will look for .property files in the current runtime directory or the
 * subdirectories ./config, ./etc and ./properties for a file named as the sub class of Configurable (including the
 * package name).
 *
 * All protected (non-static) fields can be set via the {@link #setFieldValues} method. This won't be done
 * automatically, since subclasses may want to add some extra configuration behavior.
 *
 */
public abstract class Configurable {

	/**
	 * Tries to find a (service) class specific property file
	 *
	 * The following dirs will be checked:
	 * <ul>
	 * <li>./</li>
	 * <li>./config/</li>
	 * <li>./etc/</li>
	 * <li>./properties/</li>
	 * </ul>
	 *
	 * The name of the property file is the name of the implementing service class followed by .properties.
	 *
	 * @return hashtable with all property entries
	 */
	protected Hashtable<String, String> getProperties() {
		Hashtable<String, String> result = new Hashtable<String, String>();

		String propFile = findPropertyFile();
		if (propFile == null) {
			return result;
		}

		try {
			Properties props = new Properties();
			props.load(new FileInputStream(propFile));

			for (Object propname : props.keySet()) {
				result.put((String) propname, (String) props.get(propname));
			}
		} catch (FileNotFoundException e) {
			System.err.println("Unable to open property file " + propFile);
		} catch (IOException e) {
			System.err.println("Error opening property file " + propFile + ": " + e.getMessage());
		}

		return result;
	}

	/**
	 * Sets a field to the given value.
	 * 
	 * @param f The class field that is set.
	 * @param value The new value for the given class field.
	 *
	 * @throws IllegalAccessException if this Field object is enforcing Java language access control and the underlying
	 *             field is either inaccessible or final.
	 * @throws InstantiationException if this Class represents an abstract class, an interface, an array.
	 */
	@SuppressWarnings("unchecked")
	public void setField(Field f, String value) throws IllegalAccessException, InstantiationException {
		Class<?> fieldType = f.getType();

		if (fieldType.equals(String.class)) {
			f.set(this, value);
		} else if (fieldType.equals(long.class) || fieldType.equals(Long.class)) {
			f.set(this, Long.parseLong(value));
		} else if (fieldType.equals(int.class) || fieldType.equals(Integer.class)) {
			f.set(this, Integer.parseInt(value));
		} else if (fieldType.equals(short.class) || fieldType.equals(Short.class)) {
			f.set(this, Short.parseShort(value));
		} else if (fieldType.equals(boolean.class) || fieldType.equals(Boolean.class)) {
			f.set(this, Boolean.parseBoolean(value));
		} else if (fieldType.equals(float.class) || fieldType.equals(Float.class)) {
			f.set(this, Float.parseFloat(value));
		} else if (fieldType.equals(double.class) || fieldType.equals(Double.class)) {
			f.set(this, Double.parseDouble(value));
		} else {
			String[] values = value.split("\\s*,\\s*");// how to split the value string!
			if (fieldType.isArray()) {
				fieldType = fieldType.getComponentType();

				if (fieldType.equals(String.class)) {
					f.set(this, values);
				} else if (fieldType.equals(long.class) || fieldType.equals(Long.class)) {
					Long[] vl = new Long[values.length];
					for (int i = 0; i < values.length; i++) {
						vl[i] = Long.parseLong(values[i]);
					}
					f.set(this, vl);
				} else if (fieldType.equals(int.class) || fieldType.equals(Integer.class)) {
					Integer[] vl = new Integer[values.length];
					for (int i = 0; i < values.length; i++) {
						vl[i] = Integer.parseInt(values[i]);
					}
					f.set(this, vl);
				} else if (fieldType.equals(short.class) || fieldType.equals(Short.class)) {
					Short[] vl = new Short[values.length];
					for (int i = 0; i < values.length; i++) {
						vl[i] = Short.parseShort(values[i]);
					}
					f.set(this, vl);
				} else if (fieldType.equals(boolean.class) || fieldType.equals(Boolean.class)) {
					Boolean[] vl = new Boolean[values.length];
					for (int i = 0; i < values.length; i++) {
						vl[i] = Boolean.parseBoolean(values[i]);
					}
					f.set(this, vl);
				} else if (fieldType.equals(float.class) || fieldType.equals(Float.class)) {
					Float[] vl = new Float[values.length];
					for (int i = 0; i < values.length; i++) {
						vl[i] = Float.parseFloat(values[i]);
					}
					f.set(this, vl);

				} else if (fieldType.equals(double.class) || fieldType.equals(Double.class)) {
					Double[] vl = new Double[values.length];
					for (int i = 0; i < values.length; i++) {
						vl[i] = Double.parseDouble(values[i]);
					}
					f.set(this, vl);
				} else {
					System.err.println("Unknown class of field: " + f.getName() + ": " + f.getType());
				}
			} else {
				if (Collection.class.isAssignableFrom(fieldType)) {
					ParameterizedType fcoltype = (ParameterizedType) f.getGenericType();
					Class<?> genericType = (Class<?>) fcoltype.getActualTypeArguments()[0];
					if (genericType.equals(String.class)) {
						Collection<String> col = (Collection<String>) fieldType.newInstance();
						for (String value2 : values) {
							col.add(value2);
						}
						f.set(this, col);
					} else if (genericType.equals(Long.class)) { // primitive generics not possible anyway

						Collection<Long> col = (Collection<Long>) fieldType.newInstance();
						for (String value2 : values) {
							col.add(Long.parseLong(value2));
						}
						f.set(this, col);
					} else if (genericType.equals(Integer.class)) {
						Collection<Integer> col = (Collection<Integer>) fieldType.newInstance();
						for (String value2 : values) {
							col.add(Integer.parseInt(value2));
						}
						f.set(this, col);
					} else if (genericType.equals(Short.class)) {
						Collection<Short> col = (Collection<Short>) fieldType.newInstance();
						for (String value2 : values) {
							col.add(Short.parseShort(value2));
						}
						f.set(this, col);
					} else if (genericType.equals(Boolean.class)) {
						Collection<Boolean> col = (Collection<Boolean>) fieldType.newInstance();
						for (String value2 : values) {
							col.add(Boolean.parseBoolean(value2));
						}
						f.set(this, col);
					} else if (genericType.equals(Float.class)) {
						Collection<Float> col = (Collection<Float>) fieldType.newInstance();
						for (String value2 : values) {
							col.add(Float.parseFloat(value2));
						}
						f.set(this, col);
					} else if (genericType.equals(Double.class)) {
						Collection<Double> col = (Collection<Double>) fieldType.newInstance();
						for (String value2 : values) {
							col.add(Double.parseDouble(value2));
						}
						f.set(this, col);
					} else {
						System.err.println("Unknown class of field: " + f.getName() + ": " + f.getType());
					}

				} else {
					System.err.println("Unknown class of field: " + f.getName() + ": " + f.getType());
				}

			}

		}
	}

	/**
	 * Sets all field values from the classes property file. This method uses {@link #getProperties} to get the value
	 * stored in the classes property file to set all fields with the name of the properties.
	 *
	 * Note that the name "monitor" is reserved to switch the monitoring on and off.
	 */
	protected void setFieldValues() {
		setFieldValues(null);
	}

	/**
	 * Sets all field values from the classes property file. This method uses {@link #getProperties} to get the value
	 * stored in the classes property file to set all fields with the name of the properties. Also checks, if the field
	 * contains the value for the monitoring switch and sets it if so.
	 * 
	 * @param except All fields mentioned in <i>except</i> will be left out.
	 */
	protected void setFieldValues(Set<String> except) {
		Hashtable<String, String> props = getProperties();

		for (String key : props.keySet()) {
			if ((except != null && except.contains(key))) {
				continue;
			}
			try {
				Field f = getField(getClass(), key);
				if (!Modifier.isFinal(f.getModifiers()) && !Modifier.isStatic(f.getModifiers())) {
					f.setAccessible(true);
					setField(f, props.get(key));
					System.out.println("Class: " + getClass().getSimpleName() + " using: " + key + " -> "
							+ props.get(key));
				}
			} catch (NoSuchFieldException e) {
				System.err.println("field not found: " + key);
			} catch (IllegalAccessException e) {
				System.err.println("illegal access: " + key);
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Looks recursively in all superclasses for a field.
	 * 
	 * @param cls The class.
	 * @param key Key of he field.
	 * @return A declared field.
	 * @throws NoSuchFieldException See {@link Class#getDeclaredField}
	 * @throws SecurityException See {@link Class#getDeclaredField}
	 */
	private Field getField(Class<?> cls, String key) throws NoSuchFieldException, SecurityException {
		try {
			return cls.getDeclaredField(key);
		} catch (NoSuchFieldException | SecurityException e) {
			if (cls.getSuperclass() != null) {
				try {
					return getField(cls.getSuperclass(), key);
				} catch (Exception e2) {
				}
			}
			throw e;
		}
	}

	/**
	 * Looks for a property file named after the service class calling this method.
	 *
	 * @return name of a property file
	 */
	private String findPropertyFile() {
		String filename = getClass().getName() + ".properties";
		String[] testDirs = new String[] { "config/", "etc/", "properties/" };

		for (String dir : testDirs) {
			try {
				if (new File(dir + filename).exists()) {
					return dir + filename;
				}
			} catch (Exception e) {
				// XXX logging
			}
		}

		return null;
	}
}
