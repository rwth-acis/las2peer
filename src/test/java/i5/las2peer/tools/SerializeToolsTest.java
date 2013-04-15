package i5.las2peer.tools;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.Test;

public class SerializeToolsTest {

	@Test
	public void testKeySerializiation () throws NoSuchAlgorithmException, SerializationException, IOException {
		KeyGenerator kg = KeyGenerator.getInstance( "AES");
		kg.init(256);
		
		SecretKey k =  kg.generateKey();
		
		byte[] serialized = SerializeTools.serialize( k );
		SecretKey dk = SerializeTools.deserializeKey(serialized);
		
		assertEquals ( k, dk);
	}
	
	@Test
	public void testSerialization () throws SerializationException, IOException {
		String testString = "ein kleiner Test";
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream ( baos );
		oos.writeObject( testString );
		oos.close();
		byte[] noKey = baos.toByteArray(); 
		
		Object o = SerializeTools.deserialize( noKey );
		assertEquals ( testString, o );
		
		try {
			SerializeTools.deserializeKey(noKey);
			fail ( "SerializationException should have been thrown");
		} catch ( SerializationException e ) {
			// that's intended
		}
		
	}

}
