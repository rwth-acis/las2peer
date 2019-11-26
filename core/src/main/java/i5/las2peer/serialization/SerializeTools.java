package i5.las2peer.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.Base64;

import javax.crypto.SecretKey;

/**
 * <i>Static</i> class as collection of serialization and deserialization methods.
 * 
 * 
 *
 */
public class SerializeTools {

	/**
	 * serialize a single object into a byte array
	 * 
	 * @param s
	 * @return serialized content as binary (byte array)
	 * @throws SerializationException
	 */
	public static byte[] serialize(Serializable s) throws SerializationException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(s);
			oos.close();
			return baos.toByteArray();
		} catch (IOException e) {
			throw new SerializationException("IO Exception!", e);
		}
	}

	/**
	 * serialize the given Serializable object and encode the resulting byte array into Base64
	 * 
	 * @param s
	 * @return base64 encoded String
	 * @throws SerializationException
	 */
	public static String serializeToBase64(Serializable s) throws SerializationException {
		return Base64.getEncoder().encodeToString(serialize(s));
	}

	/**
	 * deserialize a single Object from a byte array
	 * 
	 * @param bytes
	 * @return deserialized object
	 * @throws SerializationException
	 */
	public static Serializable deserialize(byte[] bytes) throws SerializationException {
		return deserialize(bytes, null);
	}

	/**
	 * deserialize a single Object from a byte array
	 * 
	 * @param bytes
	 * @param clsLoader
	 * @return deserialized object
	 * @throws SerializationException
	 */
	public static Serializable deserialize(byte[] bytes, ClassLoader clsLoader) throws SerializationException {
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			ObjectInputStream ois = new ObjectInputStream(bais) {
				@Override
				protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
					if (clsLoader != null) {
						String name = desc.getName();
						try {
							return Class.forName(name, false, clsLoader);
						} catch (ClassNotFoundException ex) {
							return super.resolveClass(desc);
						}
					} else {
						return super.resolveClass(desc);
					}
				}
			};
			return (Serializable) ois.readObject();
		} catch (IOException e) {
			throw new SerializationException("IO problems", e);
		} catch (ClassNotFoundException e) {
			throw new SerializationException("Class not found ?!?!", e);
		}
	}

	/**
	 * decodes a given base64 encoded string and deserializes it into a java object
	 * 
	 * @param base64
	 * @return deserialized object
	 * @throws SerializationException
	 */
	public static Serializable deserializeBase64(String base64) throws SerializationException {
		return deserializeBase64(base64, null);
	}

	/**
	 * decodes a given base64 encoded string and deserializes it into a java object
	 * 
	 * @param base64
	 * @param clsLoader
	 * @return deserialized object
	 * @throws SerializationException
	 */
	public static Serializable deserializeBase64(String base64, ClassLoader clsLoader) throws SerializationException {
		return deserialize(Base64.getDecoder().decode(base64), clsLoader);
	}

	/**
	 * try to deserialize a single Key from the given byte array
	 * 
	 * @param bytes
	 * @return deserialized key
	 * @throws SerializationException
	 */
	public static SecretKey deserializeKey(byte[] bytes) throws SerializationException {
		try {
			return (SecretKey) deserialize(bytes);
		} catch (ClassCastException e) {
			throw new SerializationException("Not a Key!", e);
		}
	}
}
