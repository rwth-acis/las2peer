package i5.las2peer.nodeAdminConnector;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.logging.Level;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import com.sun.net.httpserver.HttpsConfigurator;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.tools.CryptoTools;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

public class KeystoreManager {

	private static final L2pLogger logger = L2pLogger.getInstance(KeystoreManager.class);

	public static HttpsConfigurator loadOrCreateKeystore(String keystoreFilename, String keystorePassword)
			throws Exception {
		char[] passwd = keystorePassword.toCharArray();
		KeyStore ks = KeyStore.getInstance("JKS");
		FileInputStream fis;
		try {
			fis = new FileInputStream(keystoreFilename);
			ks.load(fis, passwd);
		} catch (FileNotFoundException e) {
			logger.log(Level.INFO, "Keystore '" + keystoreFilename + "' not found");
			createKeystore(ks, keystoreFilename, passwd);
		}
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(ks, passwd);
		// other context names, see
		// https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#SSLContext
		SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
		sslContext.init(kmf.getKeyManagers(), null, null);
		return new HttpsConfigurator(sslContext);
	}

	private static void createKeystore(KeyStore ks, String keystoreFilename, char[] password) throws Exception {
		ks.load(null, password);
		CertAndKeyGen keyGen = new CertAndKeyGen(CryptoTools.getAsymmetricAlgorithm(),
				CryptoTools.getSignatureMethod());
		keyGen.generate(CryptoTools.getAsymmetricKeySize());
		// generate self signed certificate
		X509Certificate cert = keyGen.getSelfCertificate(new X500Name("CN=localhost, O=las2peer, OU=RWTH-ACIS"),
				3 * 365 * 24 * 3600);
		ks.setKeyEntry(NodeAdminConnector.class.getSimpleName(), keyGen.getPrivateKey(), password,
				new X509Certificate[] { cert });
		// write keystore to file
		FileOutputStream fos = new FileOutputStream(keystoreFilename);
		ks.store(fos, password);
		fos.close();
		logger.log(Level.INFO, "Created Keystore at '" + keystoreFilename + "'");
	}

}
