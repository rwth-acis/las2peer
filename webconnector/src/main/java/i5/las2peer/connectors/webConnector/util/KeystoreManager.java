package i5.las2peer.connectors.webConnector.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.tools.CryptoTools;

public class KeystoreManager {

	private static final L2pLogger logger = L2pLogger.getInstance(KeystoreManager.class);

	private static final long VALIDITY_MILLIS = TimeUnit.DAYS.toMillis(3 * 365);

	public static KeyStore loadOrCreateKeystore(String keystoreFilename, String hostname, char[] keystorePassword)
			throws Exception {
		KeyStore ks = KeyStore.getInstance("JKS");
		try {
			ks.load(new FileInputStream(keystoreFilename), keystorePassword);
		} catch (FileNotFoundException e) {
			logger.log(Level.INFO, "Keystore '" + keystoreFilename + "' not found");
			createKeystore(ks, keystoreFilename, keystorePassword, hostname);
		}
		return ks;
	}

	private static void createKeystore(KeyStore ks, String keystoreFilename, char[] password, String hostname)
			throws Exception {
		ks.load(null, password);
		// generate self signed CA certificate
		KeyPair caKeys = generateKeyPair();
		String caCommonName = "Node Local las2peer Root CA";
		X500Name caName = new X500Name("CN=" + caCommonName + ", O=las2peer, OU=RWTH-ACIS");
		X509Certificate caCert = createCertificate(caName, caKeys.getPublic(), caName, caKeys.getPrivate(), hostname,
				true);
		ks.setKeyEntry(caCommonName, caKeys.getPrivate(), password, new X509Certificate[] { caCert });
		// generate connector certificate signed by CA
		KeyPair keys = generateKeyPair();
		X500Name name = new X500Name("CN=" + hostname + ", O=las2peer, OU=RWTH-ACIS");
		X509Certificate cert = createCertificate(name, keys.getPublic(), caName, caKeys.getPrivate(), hostname, false);
		ks.setKeyEntry(hostname, keys.getPrivate(), password, new X509Certificate[] { cert, caCert });
		// write keystore to file
		try (FileOutputStream fos = new FileOutputStream(keystoreFilename)) {
			ks.store(fos, password);
		}
		logger.log(Level.INFO, "Created Keystore at '" + keystoreFilename + "'");
	}

	private static KeyPair generateKeyPair() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance(CryptoTools.getAsymmetricAlgorithm());
		generator.initialize(CryptoTools.getAsymmetricKeySize());
		return generator.generateKeyPair();
	}

	private static X509Certificate createCertificate(X500Name subject, PublicKey subjectKey, X500Name issuer,
			PrivateKey issuerKey, String hostname, boolean isCa) throws Exception {
		Date notBefore = new Date();
		Date notAfter = new Date(notBefore.getTime() + VALIDITY_MILLIS);
		BigInteger serial = new BigInteger(64, new SecureRandom());
		X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuer, serial, notBefore, notAfter, subject,
				subjectKey);
		builder.addExtension(Extension.subjectAlternativeName, false,
				new GeneralNames(new GeneralName(GeneralName.dNSName, hostname)));
		if (isCa) {
			builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));
		}
		ContentSigner signer = new JcaContentSignerBuilder(CryptoTools.getSignatureMethod()).build(issuerKey);
		return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
	}

	public static void writeCertificateToPEMStream(Certificate certificate, OutputStreamWriter outputStreamWriter)
			throws IOException, CertificateEncodingException {
		outputStreamWriter.write("-----BEGIN CERTIFICATE-----\n");
		outputStreamWriter.write(Base64.getMimeEncoder().encodeToString(certificate.getEncoded()));
		outputStreamWriter.write("\n-----END CERTIFICATE-----\n");
	}

	public static void writeCertificateToPEMFile(Certificate certificate, String filename)
			throws IOException, CertificateEncodingException {
		try (FileWriter fw = new FileWriter(filename)) {
			writeCertificateToPEMStream(certificate, fw);
		}
	}

}
