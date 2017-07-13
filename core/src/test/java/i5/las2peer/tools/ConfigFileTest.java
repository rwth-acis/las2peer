package i5.las2peer.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import i5.las2peer.tools.helper.ConfigFile;

public class ConfigFileTest {

	@Test
	public void test() {
		try {

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testSectionNames() {
		try {
			String str = "[test]\n   [test2]\n";
			ConfigFile conf = new ConfigFile(new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8)));
			Assert.assertTrue(conf.hasSection("test"));
			Assert.assertTrue(conf.hasSection("test2"));
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			conf.store(baos);
			byte[] bytes = baos.toByteArray();
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			ConfigFile back = new ConfigFile(bais);
			Assert.assertTrue(back.hasSection("test"));
			Assert.assertTrue(back.hasSection("test2"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	private enum test {
		TEST_ENUM;
	}

	@Test
	public void testValues() {
		try {
			String str = "val1 = 1\nval2=2\nval3=1=1\nval4 = 1 = 1\nval5 \\= 5";
			ConfigFile conf = new ConfigFile(new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8)));
			conf.put("enum", test.TEST_ENUM);
			Assert.assertEquals("1", conf.get("val1"));
			Assert.assertEquals("2", conf.get("val2"));
			Assert.assertEquals("1=1", conf.get("val3"));
			Assert.assertEquals("1 = 1", conf.get("val4"));
			Assert.assertEquals(Arrays.asList("val5 = 5"), conf.getAll());
			Assert.assertEquals("TEST_ENUM", conf.get("enum"));
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			conf.store(baos);
			byte[] bytes = baos.toByteArray();
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			ConfigFile back = new ConfigFile(bais);
			Assert.assertEquals("1", back.get("val1"));
			Assert.assertEquals("2", back.get("val2"));
			Assert.assertEquals("1=1", back.get("val3"));
			Assert.assertEquals("1 = 1", back.get("val4"));
			Assert.assertEquals(Arrays.asList("val5 = 5"), back.getAll());
			Assert.assertEquals("TEST_ENUM", back.get("enum"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
