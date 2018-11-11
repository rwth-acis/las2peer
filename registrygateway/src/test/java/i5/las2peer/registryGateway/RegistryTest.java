package i5.las2peer.registryGateway;

import org.junit.Before;
import org.junit.Test;

public class RegistryTest {
	Registry testee;

	@Before
	public void setup() throws BadEthereumCredentialsException {
		this.testee = new Registry();
	}

	@Test
	public void emptyTest() {
		assert(true);
	}
}
