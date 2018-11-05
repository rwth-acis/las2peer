package i5.las2peer.registryGateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class RegistryTest {
    Registry testee;

    @Before
    public void setup() {
        this.testee = new Registry();
    }
}
