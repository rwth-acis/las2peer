package i5.las2peer.connectors.nodeAdminConnector;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.logging.Level;

import javax.security.auth.x500.X500Principal;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.tools.CryptoTools;
import sun.misc.BASE64Encoder;
import sun.security.provider.X509Factory;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.BasicConstraintsExtension;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.DNSName;
import sun.security.x509.GeneralName;
import sun.security.x509.GeneralNames;
import sun.security.x509.SubjectAlternativeNameExtension;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

public class KeystoreManager {

	private static final L2pLogger logger = L2pLogger.getInstance(KeystoreManager.class);

	public static KeyStore loadOrCreateKeystore(String keystoreFilenamePrefix, String hostname, char[] keystorePassword,
			boolean persistentKeystore) throws Exception {
		String keystoreFilename = keystoreFilenamePrefix + hostname + ".jks";
		KeyStore ks = KeyStore.getInstance("JKS");
		try {
			if (persistentKeystore) {
				ks.load(new FileInputStream(keystoreFilename), keystorePassword);
			} else {
				throw new FileNotFoundException(); // TODO hacky -> refactor
			}
		} catch (FileNotFoundException e) {
			logger.log(Level.INFO, "Keystore '" + keystoreFilename + "' not found");
			createKeystore(ks, keystoreFilename, keystorePassword, hostname, persistentKeystore);
		}
		return ks;
	}

	private static void createKeystore(KeyStore ks, String keystoreFilename, char[] password, String hostname,
			boolean persistKeystore) throws Exception {
		ks.load(null, password);
		// generate self signed CA certificate
		CertAndKeyGen caKeyGen = new CertAndKeyGen(CryptoTools.getAsymmetricAlgorithm(),
				CryptoTools.getSignatureMethod());
		caKeyGen.generate(CryptoTools.getAsymmetricKeySize());
		PrivateKey caPrivateKey = caKeyGen.getPrivateKey();
		CertificateExtensions caExts = new CertificateExtensions();
		X500Name caX500Name = new X500Name("CN=" + hostname + " Root CA, O=las2peer, OU=RWTH-ACIS");
		SubjectAlternativeNameExtension caNameExt = new SubjectAlternativeNameExtension(
				new GeneralNames().add(new GeneralName(caX500Name)));
		caExts.set(caNameExt.getName(), caNameExt);
		BasicConstraintsExtension bce = new BasicConstraintsExtension(true, 0);
		caExts.set(bce.getName(), bce);
		X509Certificate caCert = caKeyGen.getSelfCertificate(caX500Name, new Date(), 3 * 365 * 24 * 3600, caExts);
		ks.setKeyEntry(NodeAdminConnector.class.getSimpleName() + " Root CA", caPrivateKey, password,
				new X509Certificate[] { caCert });
		// generate connector certificate signed by CA
		CertAndKeyGen keyGen = new CertAndKeyGen(CryptoTools.getAsymmetricAlgorithm(),
				CryptoTools.getSignatureMethod());
		keyGen.generate(CryptoTools.getAsymmetricKeySize());
		X500Name x500Name = new X500Name("CN=" + hostname + ", O=las2peer, OU=RWTH-ACIS");
		CertificateExtensions exts = new CertificateExtensions();
		SubjectAlternativeNameExtension nameExt = new SubjectAlternativeNameExtension(
				new GeneralNames().add(new GeneralName(new DNSName(hostname))));
		exts.set(nameExt.getName(), nameExt);
		X509Certificate certReq = keyGen.getSelfCertificate(x500Name, new Date(), 3 * 365 * 24 * 3600, exts);
		X509Certificate cert = createSignedCertificate(certReq, caCert, caPrivateKey);
		// the alias has to be the class name here
		ks.setKeyEntry(NodeAdminConnector.class.getSimpleName(), keyGen.getPrivateKey(), password,
				new X509Certificate[] { cert, caCert });
		if (persistKeystore) {
			// write keystore to file
			FileOutputStream fos = new FileOutputStream(keystoreFilename);
			ks.store(fos, password);
			fos.close();
			logger.log(Level.INFO, "Created Keystore at '" + keystoreFilename + "'");
		}
	}

	private static X509Certificate createSignedCertificate(X509Certificate cetrificate,
			X509Certificate issuerCertificate, PrivateKey issuerPrivateKey) throws Exception {
		X500Principal issuer = issuerCertificate.getIssuerX500Principal();
		String issuerSigAlg = issuerCertificate.getSigAlgName();
		byte[] inCertBytes = cetrificate.getTBSCertificate();
		X509CertInfo info = new X509CertInfo(inCertBytes);
		info.set(X509CertInfo.ISSUER, new X500Name(issuer.getName()));
		X509CertImpl outCert = new X509CertImpl(info);
		outCert.sign(issuerPrivateKey, issuerSigAlg);
		return outCert;
	}

	public static void writeCertificateToPEMFile(Certificate certificate, String filename)
			throws IOException, CertificateEncodingException {
		FileWriter fw = new FileWriter(filename);
		fw.write(X509Factory.BEGIN_CERT + "\n");
		fw.write(new BASE64Encoder().encodeBuffer(certificate.getEncoded()));
		fw.write(X509Factory.END_CERT);
		fw.close();
	}

}
