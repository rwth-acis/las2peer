package i5.las2peer.communication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.execution.RMITask;

import java.io.Serializable;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import i5.las2peer.security.BasicAgentStorage;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.testing.MockAgentFactory;


public class MessageTest {

	@Test
	public void testSimpleMessage() {
		try {
			String content = "eine Nachricht, die jetzt deutlich länger sein sollte, weil ich doch mal sehen will, ob jetzt die Länge vom String ein kleines Problem darstellt. Im Moment habe ich nähmlich komische IllegalBlocksizeExceptions, nachdem ich die id statt auf 0 auf einen zufälligen Long-Wert gesetzt habe...";

			UserAgentImpl eve = MockAgentFactory.getEve();
			UserAgentImpl adam = MockAgentFactory.getAdam();
			BasicAgentStorage storage = new BasicAgentStorage();
			storage.registerAgents(eve, adam);

			eve.unlock("evespass");

			Message m = new Message(eve, adam, content);

			System.out.println(eve.toXmlString());
			System.out.println(adam.toXmlString());

			String xml = m.toXmlString();
			System.out.println(xml);

			eve.lockPrivateKey();
			adam.unlock("adamspass");

			Message m2 = Message.createFromXml(xml);
			m2.open(adam, storage);
			String retrieved = (String) m2.getContent();

			System.out.println("empfangen: " + retrieved);

			m2.verifySignature();
			assertEquals(content, retrieved);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	// @Test
	// actually fails, because implemeted before timestamp and timeout
	public void testAgentIdChecks() {
		try {
			String sAgentA = "<las2peer:agent type=\"user\">" + "<id>4882835596055779038</id>"
					+ "<publickey encoding=\"base64\">rO0ABXNyABRqYXZhLnNlY3VyaXR5LktleVJlcL35T7OImqVDAgAETAAJYWxnb3JpdGhtdAASTGphdmEvbGFuZy9TdHJpbmc7WwAHZW5jb2RlZHQAAltCTAAGZm9ybWF0cQB+AAFMAAR0eXBldAAbTGphdmEvc2VjdXJpdHkvS2V5UmVwJFR5cGU7eHB0AANSU0F1cgACW0Ks8xf4BghU4AIAAHhwAAABJjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAL0wtzx4UeaogMc0AUZ0izqqwdQG9ulSZs9C8Ep+ARsjRzbJ4DcLNoUlGaAOciLMIljXybKn25HRLilqirmnwnx5vex1pwnj8/4lsnjhjvc35TXc2iuJqFfJKnXkxD6jFftZeuPHLOa2uMvn0qiwaJ8fpiqIrmQ3Q2T3h8+5etbKNU/rQms1OvS56BlOb3fXBu9sj6o6sRZk1aFDRwGuhY4en6Ol61B2+k09jc7tDi6NzEY3kgmHIT9oy8Ih3Ys8Yq2SqjPjiNiMqAEMxQyAf1Q1eXJMkOGHZr5dhi0FFInl4Q4t2xy9FNxu9IT7Qb3gec6Cqw1rO2PdGN7IxL7pYIUCAwEAAXQABVguNTA5fnIAGWphdmEuc2VjdXJpdHkuS2V5UmVwJFR5cGUAAAAAAAAAABIAAHhyAA5qYXZhLmxhbmcuRW51bQAAAAAAAAAAEgAAeHB0AAZQVUJMSUM=</publickey>"
					+ "<privatekey encrypted=\"AES\" keygen=\"PBKDF2WithHmacSHA1\">"
					+ "<salt encoding=\"base64\">IzeErQ3ByrBUqqfbYhnomw==</salt>"
					+ "<data encoding=\"base64\">gCEkUbkEytg0MV0cNl6vmNi7/hO/qbE60/PfUIkoWlYw0jBILRjGfAS1s4tRvpCty3CW3w0LyCizohpP+mAeGWP+VsuDUTOSe7dob5JIHetcB3G7yM1ljx5f9mxA25biCLXJhboI6iRziGnh49pcwsB97GlcBk/Ft0d71Nk6/Tnh4li5cFQVotDn8S7aopPFBVglLh6TzRxqIuFaNtjePSUTM1HNROj+kqPvwTCCiuD8bUItaEgbuFDFLQnpUyEzu+d8q/b32pK4trdtMHahSfelmrGx4MnV9eBG0MwriKDXKnlQBBAYrMaCv7Tj8xE7ai4/aigYmDc7mslryTDikfZPtDFvIdCQea/soPz/RiNsem29cGengWW5xOnA/rWO8C65/uogBIbegb0oRQ/3+GpqG/zdZGNSzIDCVg6WaEzZgBrUrnXZPvqx973aDvxaOfLJ2GpG9uVCxQ/jC1XLJPxYTG0TS5QM08XZW2ysaJNECKtkA1XJWuD0LrikEkLtE0jjzMxGYhJHEbrOUHgo6z2VjD3plK8DioXVqUFulmQhWneo4MnLOXZY1+OO9P4xHsBT7KYHPzwCq5JU86oAOR5Hg1gMw7wz6Kw3uUdUmpbROAcn9YsTr1Q+WM3lqipoA+JT1JfvkDw2i3Zxx3KGPonpc4Ey7BNLi/m1GTmUlTG4lCkMYyZh+f3jbzLt5q/cEYDlxGYTLk0Z9o7B7qI8i5H4bLx67ZUXq282IcPIF6noptQWwhtbqFAwsgam0/IKPaR5rkpj7Aa8kQRbIf/Xy5dd2QWFySxBj3m7IIVU8R7bm1LLJQ8vf28TyQfCjn/+utADtqZ4ngx3mGzEQ8tEK+K3xhL4oqF6CrqUMcJz1JIbZq6ZOYQA6jVltAZbIFl416sKpD5x34IXfMGbe3yG0WgpQIsy2GwGsYrZK40U+buSOWQGXTBd1TFOEGSPIB9EgTn5HitPnhrUKrUeJufc3peTaoQmZA35AlEPqt5Ocq67SIs8tJZ1HeeMvMfmcv6Kt+4dF7FRtDZvOY0bC+7UWUH48pDwv/oZSUR5Hfj9AblKocP68wgHpqTQNNud3ZWesy2r7gMgJdNjfwkfRdcQufhjjMWThqdYQ+ioDPagQ4Nov34TsJRaUCh1XKL34NhVcVaB/QQ+805Grk6tUZt+BXj3g1ys4r4N1NUDlB7Js3NrVtnhjNKDHRyStn7KX+rwW3KaQ9EbWneGELg7S4hIFBT1LLTU0R3Q1h3C56ulFuONWk13PX7a4/LwOCSQv+SV1HdKrrIFPMswY4G5A2M+ssDN+MkQ2L2OzUrmss+PQk/V1uEw90gxfg42fKZT5Zmu9gtReJY6KXi9qTjXZTQxAOSyLyQWXutEEMDrhoFMvCHjwCgCp+l41q30t6KaMCn9nXl8GzxFrVMBgZk864NYkML10YIrnBU7YW37BE1TauvRqUUXsQDgPhY/qXd5xkCRlDux0DdfQjuwligr5cafrNTouhHsg0z2+paT7lQUPu9ZqZpgt12vhj3UTgnuMYFISezRxdnE+wxObfVk54/GZGlNfxAb2qVJlCMs2vBoOT4YCXvt8MwbENM1HKI2hKDnuPrckMuvRVtPEa6t6de8nnWzkWLfcztm6VnIJJ3JHFZvuNXiB6I3JFiIgCIQCgApvZyoLAmLiDAv0nRWYpzsPZNCgdWGxmskKidN+04PVTeJnRrEdNnI19boYYAON4RKTUiZtNd+W16/CfXh65AAuHrL/Cfr4o4sUDQxMV03Fn49JN4DhGjKMf4h8wXLYaa305FfoGuI7tWMc7aWrFpp/rhhI3K1G/y2wiVq0dF1a9m0v02krgV95VP1mxCGHLelTdA84ZVa+U/Z8zA5bsSP1pT7shxayLf7UKhP7cUnnFMuJ01e7OX6i9teUd0yizHNKem3pPRI0rgEqkbB6jin7HYNHHTh4TE1nMaFODj4DvnS9jINk+TypTHt9yLEfqQ5</data>"
					+ "</privatekey>" + "</las2peer:agent>";

			String sAgentB = "<las2peer:agent type=\"user\">" + "<id>2497775129358630574</id>"
					+ "<publickey encoding=\"base64\">rO0ABXNyABRqYXZhLnNlY3VyaXR5LktleVJlcL35T7OImqVDAgAETAAJYWxnb3JpdGhtdAASTGphdmEvbGFuZy9TdHJpbmc7WwAHZW5jb2RlZHQAAltCTAAGZm9ybWF0cQB+AAFMAAR0eXBldAAbTGphdmEvc2VjdXJpdHkvS2V5UmVwJFR5cGU7eHB0AANSU0F1cgACW0Ks8xf4BghU4AIAAHhwAAABJjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKtp+CcB12xEE7vMYYVFXa7GA82u62utKU6Z3k+3las3QgElOPsz9HQ9UqFnUkhuBkkbP2sMN69ItZgYoOVT6R4ubK/CvoQOOUpTbb1XsjgWRb+i+7EhMOAw1MYLXv2Y4sV/8gxhKEfg34iAXp5j0Jxfx+8g3w+aat0++VBEDMGfSsd7Qj8Qb8wSB8cqYegJ1YD6GgCkzrCOfXjlx7cW08gGh7TVFbsQt8rlHwrueirqDxuW6GBfvpMpYdmLbFqdeWg83YV3gbRhPpEXuHnGYgODog05oSl7Rbmg2sTwN5OThL2Bm0wU75hcUfdZQnz8XFsNSbcpK5BjrmhzStWPklUCAwEAAXQABVguNTA5fnIAGWphdmEuc2VjdXJpdHkuS2V5UmVwJFR5cGUAAAAAAAAAABIAAHhyAA5qYXZhLmxhbmcuRW51bQAAAAAAAAAAEgAAeHB0AAZQVUJMSUM=</publickey>"
					+ "<privatekey encrypted=\"AES\" keygen=\"PBKDF2WithHmacSHA1\">"
					+ "<salt encoding=\"base64\">kIHIrygeQFmAl4c6WPhJRQ==</salt>"
					+ "<data encoding=\"base64\">myP37H/puUbtL8ctLENyrd/+MN1facg0Hjf0C39+YVElxJ5sbpsCQQ0KGeyVuTLqo2PQ3QNDTDXH4Duey56DplKlkgGTIUnuSAdnAX2Tz9W52DmDxNRcWyf9pR8N4f1bVhVctPhVJOVJBf/na9QjVwZyKOYdGVPpwPZdUqY1+XOil6YlfkXqFSKtdYYnOPf2wQATiSwVH9vFwWhLAvx9iZo8qzjknVU63xrCPal1VuIjjdjPXRAmSgPQFy4MVTMGywyzmjXOubNRQ+q7QRIfs4XIa9U6pxHThwONk/7uuOhtvxpOzyOnGhkA5HkKBOEExopkkcmIhfsJR79T+ccTrlM3l9X+lCqk3FOQ7R/gazEGlOzyWXdusaSPUsgMFxxoPH4xzfcTbX+iKdSCosSlHE9Rz8v7MjNQABWmoPGv7N1dUqNVw49BzOdBfuosQHHKsLuM0vkG+j8H29fztyL4S1U+IXZrwOYyax4VKQp8fgyvtzZcEamhiyOCL3szfsLuclrF3Dr0hfo10i+waR+mUGKh0/s/DtMj48MoSoctb6k19PesqB38xulpAycepeE+lOzbbKIPSPeZ/BlfcI+vU1jnfUJ6HtyIy/EGWKHnMUla0VeJXPY0V+VcYLaFNrdxSlbX5PF1NoxJsqEO1tKCmbWwyYZBnG7NlALjcxyEj4Tbza1HIXdDimGyQFZXhfHBDPELTVwFna+WzFbXUabzxh1/vvQm2OLdwrDhsNTo6gA4/6XfJ2y4ICGKnNx7EcQQwV3UclPMS5FOqHxOSDqg6ZfQO+y/qmo2ZEnGH3t94cGZDbQvJsWksJTZXwqF+X14Lr+SbIZTygDtRa4yybvfZ7oGQm6RNEg1MniMUNg08I5zAMXpxyxno1Hvz7VUNT6Yo/4A1iPXIN0+rUzT41/Mone6yGmMQslMS6slnmws7oCcpbx53Z52FCPy8juC9cmGgpqtnd9RI0ad4RKikpI3tHdeiIvI4CWoa3IzFijKHUKDaWge7XPknRmo/jHj00nOANXkRPIL2W4thoqvGQZ8n6gypl0BuBwPsKVr3JBIFrrEHk7z/qzsBHyi3hJr2iV1IAs+f2ybFusIkZTssJJVXlXhPm0PJEQxW5943xzAm4pXnoS9spcEzj7pJPY1rTMDagXPD2qLFNYkI8VhmuejmDM5sh6AAXdWm4O5vt295wduJt21qdMy3StAWNpeZP0n5MAibGFEGZspthkHzi9c16/YLtnuJBEZCOpGM0jD59xTxgQxMMbg64Jl1J1P7DDAa9j0GJ4Roz0Ud0+clfz7ZuO8FFqgvTghIFWrqNb0f+wGv66tZboINes63QaMv3wkKwVvafI2EBBXPFdWBHrz44CJP7iFjEPu6Q2bCj+Ynh4VWyq1m8zdk1SnmUymeYIUfzmgP2gMtpVHk1Rj6N9KrCvN35CFzX/fGUpMXhqVe9lsPxR48WcHQoEDyMsjzfw5oY8H4VRYjBsCmGwvKlgd/qbMwxN9yOcbnUtmaH9M6x2ax+ng/F8150Wv76kx9uOQvwLvoP93ddTURHqYhGM5k2zZYYwZlC+PD9ujkvsuu/3bPFoghOfCd3fdWhpEGa5g+M7jA5L/yoSd/wqceFTxLNnoYKwKycdeqthlFsGTg9R02bDdJCBOK2aBHOvjUcfUEX7ufMDiB1gAKhNPxQwaJ40pIjLKvh6OcGs8ZHyldPSH8zV4M74MgSMR+oBnqJ1B5W+OWR1Pdn5Nkw7kHL/Npk9qYlj9iIMTUe8AS7gTnMyJq7FSJUFm6RdY3bgRAuGhIZnxfarJWx6COxHVs4WkPhT5cv6gBLko4Z2yW+ktFxTb6D0vRJYB0ltIrkVU+E08JKWDBbH6lbLDVgpK6JI2Vt28UrVJ/CS8RqUfhFtIsnqx0xcyZeZregEMcrZfZojnfEHPEVD+EQ4E8hoMGESSdD1mLwqS5Kt4qcuNWNoGZV6lwGI/puoW4vMbz88Hi3ul</data>"
					+ "</privatekey>" + "</las2peer:agent>";

			String sAgentB2 = "<las2peer:agent type=\"user\">" + "<id>10234</id>"
					+ "<publickey encoding=\"base64\">rO0ABXNyABRqYXZhLnNlY3VyaXR5LktleVJlcL35T7OImqVDAgAETAAJYWxnb3JpdGhtdAASTGphdmEvbGFuZy9TdHJpbmc7WwAHZW5jb2RlZHQAAltCTAAGZm9ybWF0cQB+AAFMAAR0eXBldAAbTGphdmEvc2VjdXJpdHkvS2V5UmVwJFR5cGU7eHB0AANSU0F1cgACW0Ks8xf4BghU4AIAAHhwAAABJjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKtp+CcB12xEE7vMYYVFXa7GA82u62utKU6Z3k+3las3QgElOPsz9HQ9UqFnUkhuBkkbP2sMN69ItZgYoOVT6R4ubK/CvoQOOUpTbb1XsjgWRb+i+7EhMOAw1MYLXv2Y4sV/8gxhKEfg34iAXp5j0Jxfx+8g3w+aat0++VBEDMGfSsd7Qj8Qb8wSB8cqYegJ1YD6GgCkzrCOfXjlx7cW08gGh7TVFbsQt8rlHwrueirqDxuW6GBfvpMpYdmLbFqdeWg83YV3gbRhPpEXuHnGYgODog05oSl7Rbmg2sTwN5OThL2Bm0wU75hcUfdZQnz8XFsNSbcpK5BjrmhzStWPklUCAwEAAXQABVguNTA5fnIAGWphdmEuc2VjdXJpdHkuS2V5UmVwJFR5cGUAAAAAAAAAABIAAHhyAA5qYXZhLmxhbmcuRW51bQAAAAAAAAAAEgAAeHB0AAZQVUJMSUM=</publickey>"
					+ "<privatekey encrypted=\"AES\" keygen=\"PBKDF2WithHmacSHA1\">"
					+ "<salt encoding=\"base64\">kIHIrygeQFmAl4c6WPhJRQ==</salt>"
					+ "<data encoding=\"base64\">myP37H/puUbtL8ctLENyrd/+MN1facg0Hjf0C39+YVElxJ5sbpsCQQ0KGeyVuTLqo2PQ3QNDTDXH4Duey56DplKlkgGTIUnuSAdnAX2Tz9W52DmDxNRcWyf9pR8N4f1bVhVctPhVJOVJBf/na9QjVwZyKOYdGVPpwPZdUqY1+XOil6YlfkXqFSKtdYYnOPf2wQATiSwVH9vFwWhLAvx9iZo8qzjknVU63xrCPal1VuIjjdjPXRAmSgPQFy4MVTMGywyzmjXOubNRQ+q7QRIfs4XIa9U6pxHThwONk/7uuOhtvxpOzyOnGhkA5HkKBOEExopkkcmIhfsJR79T+ccTrlM3l9X+lCqk3FOQ7R/gazEGlOzyWXdusaSPUsgMFxxoPH4xzfcTbX+iKdSCosSlHE9Rz8v7MjNQABWmoPGv7N1dUqNVw49BzOdBfuosQHHKsLuM0vkG+j8H29fztyL4S1U+IXZrwOYyax4VKQp8fgyvtzZcEamhiyOCL3szfsLuclrF3Dr0hfo10i+waR+mUGKh0/s/DtMj48MoSoctb6k19PesqB38xulpAycepeE+lOzbbKIPSPeZ/BlfcI+vU1jnfUJ6HtyIy/EGWKHnMUla0VeJXPY0V+VcYLaFNrdxSlbX5PF1NoxJsqEO1tKCmbWwyYZBnG7NlALjcxyEj4Tbza1HIXdDimGyQFZXhfHBDPELTVwFna+WzFbXUabzxh1/vvQm2OLdwrDhsNTo6gA4/6XfJ2y4ICGKnNx7EcQQwV3UclPMS5FOqHxOSDqg6ZfQO+y/qmo2ZEnGH3t94cGZDbQvJsWksJTZXwqF+X14Lr+SbIZTygDtRa4yybvfZ7oGQm6RNEg1MniMUNg08I5zAMXpxyxno1Hvz7VUNT6Yo/4A1iPXIN0+rUzT41/Mone6yGmMQslMS6slnmws7oCcpbx53Z52FCPy8juC9cmGgpqtnd9RI0ad4RKikpI3tHdeiIvI4CWoa3IzFijKHUKDaWge7XPknRmo/jHj00nOANXkRPIL2W4thoqvGQZ8n6gypl0BuBwPsKVr3JBIFrrEHk7z/qzsBHyi3hJr2iV1IAs+f2ybFusIkZTssJJVXlXhPm0PJEQxW5943xzAm4pXnoS9spcEzj7pJPY1rTMDagXPD2qLFNYkI8VhmuejmDM5sh6AAXdWm4O5vt295wduJt21qdMy3StAWNpeZP0n5MAibGFEGZspthkHzi9c16/YLtnuJBEZCOpGM0jD59xTxgQxMMbg64Jl1J1P7DDAa9j0GJ4Roz0Ud0+clfz7ZuO8FFqgvTghIFWrqNb0f+wGv66tZboINes63QaMv3wkKwVvafI2EBBXPFdWBHrz44CJP7iFjEPu6Q2bCj+Ynh4VWyq1m8zdk1SnmUymeYIUfzmgP2gMtpVHk1Rj6N9KrCvN35CFzX/fGUpMXhqVe9lsPxR48WcHQoEDyMsjzfw5oY8H4VRYjBsCmGwvKlgd/qbMwxN9yOcbnUtmaH9M6x2ax+ng/F8150Wv76kx9uOQvwLvoP93ddTURHqYhGM5k2zZYYwZlC+PD9ujkvsuu/3bPFoghOfCd3fdWhpEGa5g+M7jA5L/yoSd/wqceFTxLNnoYKwKycdeqthlFsGTg9R02bDdJCBOK2aBHOvjUcfUEX7ufMDiB1gAKhNPxQwaJ40pIjLKvh6OcGs8ZHyldPSH8zV4M74MgSMR+oBnqJ1B5W+OWR1Pdn5Nkw7kHL/Npk9qYlj9iIMTUe8AS7gTnMyJq7FSJUFm6RdY3bgRAuGhIZnxfarJWx6COxHVs4WkPhT5cv6gBLko4Z2yW+ktFxTb6D0vRJYB0ltIrkVU+E08JKWDBbH6lbLDVgpK6JI2Vt28UrVJ/CS8RqUfhFtIsnqx0xcyZeZregEMcrZfZojnfEHPEVD+EQ4E8hoMGESSdD1mLwqS5Kt4qcuNWNoGZV6lwGI/puoW4vMbz88Hi3ul</data>"
					+ "</privatekey>" + "</las2peer:agent>";

			UserAgentImpl a = UserAgentImpl.createFromXml(sAgentA);
			UserAgentImpl b = UserAgentImpl.createFromXml(sAgentB);
			UserAgentImpl b2 = UserAgentImpl.createFromXml(sAgentB2);

			BasicAgentStorage storage = new BasicAgentStorage();
			storage.registerAgent(a);

			String messageXml = "<las2peer:message from=\"4882835596055779038\" to=\"2497775129358630574\" generated=\""
					+ new Date().getTime() + "\" timeout=\"10000\">"
					+ "<content encryption=\"RSA \" encoding=\"base64\">ffERcSgVxV/xQecFT3g2V6qhMlV7WOQrQ7Zv5p6fbD+h5mOsze3Ab14NDgijmy+COndwJn4eKe3vTWBuooJof5u9pO9pDMwyrefAtln8/ZCTo0ZABBmHmHPa19dvvyQD/+cn4f4o3QLvdoUUr9PZQxaoKH2dAqVNJNsI1GagBrhhsFR4PrA3E/ai7Dk63Zb09as6ICcy2ffl4zQFDjMFKse85C5maNKhhp+zlE4YBkItGukpHnitu2n/3IsCYG5KHjMZy7u8vJRKtGpqtDqVLo8nTlTiTAWVlv+KJq026BN/Mu/mYE0vE3y/65oDg3jnpBCDOL2DRo7Etk/o+q8Cig==</content>"
					+ "<signature encoding=\"base64\" method=\"SHA1withRSA\">JQ75fwuY0p5FHp5Q7SuyKop1leutzfNW/56C8JljXSHqwHrFSKO257SoQuIqwRjilVtdFxqe0aEu01J7wSR3QinRJrPeYhqDZNARR1ZiAkg+NJnAjII8eQpuTDasvDpJR6RYOTNJGFXuU4F4+mBbWdp/1XFaHrB8qHpSZ/TnRbRwIdCEXdkiCTnVHxKLkwUgUEUIqC65/r21FZ3Yyts1ZA9W0GrjdM5bcZPcPDYG7TjCR72xYuzuAEvsdrqo+bwUxnBkbkUClXClnVX71uxaPJ/qS5u4U5ojycUd3yBDHdOpLbkpVzwguARaOMHZY5rxcDwzJupaq2GI7qObU9oZwg==</signature>"
					+ "</las2peer:message>";

			String messageXml2 = "<las2peer:message from=\"4882835596055779038\" to=\"" + b2.getIdentifier()
					+ "\" generated=\"" + new Date().getTime() + "\" timeout=\"10000\">"
					+ "<content encryption=\"RSA \" encoding=\"base64\">ffERcSgVxV/xQecFT3g2V6qhMlV7WOQrQ7Zv5p6fbD+h5mOsze3Ab14NDgijmy+COndwJn4eKe3vTWBuooJof5u9pO9pDMwyrefAtln8/ZCTo0ZABBmHmHPa19dvvyQD/+cn4f4o3QLvdoUUr9PZQxaoKH2dAqVNJNsI1GagBrhhsFR4PrA3E/ai7Dk63Zb09as6ICcy2ffl4zQFDjMFKse85C5maNKhhp+zlE4YBkItGukpHnitu2n/3IsCYG5KHjMZy7u8vJRKtGpqtDqVLo8nTlTiTAWVlv+KJq026BN/Mu/mYE0vE3y/65oDg3jnpBCDOL2DRo7Etk/o+q8Cig==</content>"
					+ "<signature encoding=\"base64\" method=\"SHA1withRSA\">JQ75fwuY0p5FHp5Q7SuyKop1leutzfNW/56C8JljXSHqwHrFSKO257SoQuIqwRjilVtdFxqe0aEu01J7wSR3QinRJrPeYhqDZNARR1ZiAkg+NJnAjII8eQpuTDasvDpJR6RYOTNJGFXuU4F4+mBbWdp/1XFaHrB8qHpSZ/TnRbRwIdCEXdkiCTnVHxKLkwUgUEUIqC65/r21FZ3Yyts1ZA9W0GrjdM5bcZPcPDYG7TjCR72xYuzuAEvsdrqo+bwUxnBkbkUClXClnVX71uxaPJ/qS5u4U5ojycUd3yBDHdOpLbkpVzwguARaOMHZY5rxcDwzJupaq2GI7qObU9oZwg==</signature>"
					+ "</las2peer:message>";

			Message m = Message.createFromXml(messageXml);
			Message m2 = Message.createFromXml(messageXml2);

			b.unlock("passb");
			b2.unlock("passb");

			m.open(b, storage);

			try {
				m2.open(b2, storage);
				fail("SecurityException should have been thrown!");
			} catch (L2pSecurityException e) {
				assertTrue(e.getMessage().contains("another recipient"));
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testTimeout() {
		try {
			UserAgentImpl a = UserAgentImpl.createUserAgent("passa");
			UserAgentImpl b = UserAgentImpl.createUserAgent("passb");

			a.unlock("passa");
			b.unlock("passb");

			Date d = new Date();
			Message m = new Message(a, b, "ein test");
			Date d2 = new Date();

			assertTrue(d.getTime() <= m.getTimestamp());
			assertTrue(m.getTimestamp() <= d2.getTime());

			assertEquals(Message.DEFAULT_TIMEOUT, m.getTimeoutDate().getTime() - m.getTimestampDate().getTime());

			String xml = m.toXmlString();

			System.out.println(xml);

			assertTrue(xml.contains("generated=\""));
			assertTrue(xml.contains("timeout=\""));

			Message fromXml = Message.createFromXml(xml);

			assertEquals(m.getTimestamp(), fromXml.getTimestamp());
			assertEquals(m.getTimeoutDate(), fromXml.getTimeoutDate());

			m = new Message(a, b, "ein test", 500);
			assertFalse(m.isExpired());
			assertEquals(500, m.getTimeoutDate().getTime() - m.getTimestamp());

			Thread.sleep(1000);
			assertTrue(m.isExpired());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testResponseConstructor() {
		try {
			UserAgentImpl a = UserAgentImpl.createUserAgent("passa");
			UserAgentImpl b = UserAgentImpl.createUserAgent("passb");

			a.unlock("passa");
			b.unlock("passb");
			BasicAgentStorage storage = new BasicAgentStorage();
			storage.registerAgents(a, b);

			Message m = new Message(a, b, "some content");
			m.open(b, storage);

			Message testee = new Message(m, "some answer");

			assertEquals(a.getIdentifier(), m.getSender().getIdentifier());
			assertEquals(b.getIdentifier(), m.getRecipient().getIdentifier());

			assertTrue(m.getRecipientId() == testee.getSenderId());
			assertTrue(m.getSenderId() == testee.getRecipientId());

			assertNotNull(testee.getResponseToId());
			assertEquals(m.getId(), testee.getResponseToId().longValue());

			testee.open(a, storage);
			assertEquals(a.getIdentifier(), testee.getRecipient().getIdentifier());
			assertEquals(b.getIdentifier(), testee.getSender().getIdentifier());

			String xml = testee.toXmlString();

			assertTrue(xml.contains("responseTo=\"" + m.getId() + "\""));

			Message andBack = Message.createFromXml(xml);

			assertTrue(a.getIdentifier().equalsIgnoreCase(andBack.getRecipientId()));
			assertEquals(b.getIdentifier(), andBack.getSenderId());
			assertEquals(m.getId(), andBack.getResponseToId().longValue());

			andBack.open(a, storage);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testFromString() {
		try {
			UserAgentImpl a = UserAgentImpl.createUserAgent("passa");
			UserAgentImpl b = UserAgentImpl.createUserAgent("passb");

			BasicAgentStorage storage = new BasicAgentStorage();
			storage.registerAgents(a, b);

			a.unlock("passa");
			b.unlock("passb");

			String content = "some content";
			Message m = new Message(a, b, content);

			try {
				m.getContent();
				fail("L2pSecurityException expected");
			} catch (L2pSecurityException e) {
				// intended
			}
			m.open(b, storage);
			assertEquals(content, m.getContent());

			String xml = m.toXmlString();

			Message andBack = Message.createFromXml(xml);

			assertEquals(m.getSender().getIdentifier(), andBack.getSenderId());
			assertTrue(m.getRecipient().getIdentifier().equalsIgnoreCase(andBack.getRecipientId()));

			andBack.open(b, storage);
			assertEquals(m.getContent(), andBack.getContent());
			assertEquals(content, andBack.getContent());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testOpen() {
		try {
			UserAgentImpl a = UserAgentImpl.createUserAgent("passa");
			UserAgentImpl b = UserAgentImpl.createUserAgent("passb");

			BasicAgentStorage storage = new BasicAgentStorage();
			storage.registerAgents(a, b);

			a.unlock("passa");

			Message testee = new Message(a, b, "some content");
			assertNull(testee.getSender());
			assertNull(testee.getRecipient());
			assertTrue(b.getIdentifier().equalsIgnoreCase(testee.getRecipientId()));
			assertEquals(a.getIdentifier(), testee.getSenderId());

			b.unlock("passb");
			testee.open(b, storage);

			assertNotSame(a, testee.getSender());
			assertEquals(b, testee.getRecipient());
			assertEquals(a.getIdentifier(), testee.getSender().getIdentifier());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testRMIMessage() {
		try {
			BasicAgentStorage storage = new BasicAgentStorage();
			UserAgentImpl eve = MockAgentFactory.getEve();
			// class loading will be bypassed, so the version specified is not used
			ServiceAgentImpl service = ServiceAgentImpl
					.createServiceAgent(ServiceNameVersion.fromString("i5.las2peer.api.TestService@1.0"), "a pass");
			storage.registerAgents(eve, service);

			eve.unlock("evespass");
			Message m = new Message(eve, service,
					new RMITask(ServiceNameVersion.fromString("i5.las2peer.api.TestService@1.0"), "inc",
							new Serializable[] { new Integer(10) }));

			String xml = m.toXmlString();

			Message back = Message.createFromXml(xml);
			
			service.unlock("a pass");
			back.open(service, storage);

			RMITask content = (RMITask) back.getContent();

			assertEquals(new Integer(10), content.getParameters()[0]);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testSendingNodeId() {
		try {
			UserAgentImpl eve = MockAgentFactory.getEve();
			UserAgentImpl adam = MockAgentFactory.getAdam();

			eve.unlock("evespass");

			Message message = new Message(eve, adam, "A content String");
			message.setSendingNodeId(new Long(100));

			String xml = message.toXmlString();

			Message andBack = Message.createFromXml(xml);

			assertEquals(new Long(100), andBack.getSendingNodeId());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testPrintMessage() {
		try {
			UserAgentImpl eve = MockAgentFactory.getEve();
			UserAgentImpl adam = MockAgentFactory.getAdam();

			eve.unlock("evespass");

			Message m = new Message(eve, adam, "a simple content string");

			String xml = m.toXmlString();

			System.out.println("------ XML message output ------");
			System.out.println(xml);
			System.out.println("------ / XML message output ------");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testTopicMessage() {
		try {
			UserAgentImpl a = UserAgentImpl.createUserAgent("passa");
			UserAgentImpl b = UserAgentImpl.createUserAgent("passb");

			BasicAgentStorage storage = new BasicAgentStorage();
			storage.registerAgents(a, b);

			a.unlock("passa");
			b.unlock("passb");

			// constructor
			Message m = new Message(a, 123L, "some content");
			assertEquals(m.getRecipientId(), null);
			assertTrue(m.isTopic());

			// serialization
			Message m2 = Message.createFromXml(m.toXmlString());
			assertEquals(m.getSenderId(), m2.getSenderId());
			assertEquals(m2.getRecipientId(), null);
			assertTrue(m2.isTopic());

			// open
			m2.open(b, storage);
			assertEquals(m2.getSender(), a);
			assertTrue(m2.isOpen());
			assertEquals(m2.getContent(), "some content");

			// close
			m2.close();
			assertFalse(m2.isOpen());

			// clone
			Message m3 = m2.clone();
			m3.open(b, storage);
			assertFalse(m2.isOpen());
			assertTrue(m3.isOpen());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
