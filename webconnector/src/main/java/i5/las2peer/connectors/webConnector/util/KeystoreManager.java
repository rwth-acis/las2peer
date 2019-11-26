package i5.las2peer.connectors.webConnector.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;

import javax.security.auth.x500.X500Principal;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.tools.CryptoTools;
import sun.misc.BASE64Encoder;
import sun.security.provider.X509Factory;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.util.DerOutputStream;
import sun.security.x509.BasicConstraintsExtension;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.DNSName;
import sun.security.x509.GeneralName;
import sun.security.x509.GeneralNameInterface;
import sun.security.x509.GeneralNames;
import sun.security.x509.SubjectAlternativeNameExtension;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

public class KeystoreManager {

	private static final L2pLogger logger = L2pLogger.getInstance(KeystoreManager.class);

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
		CertAndKeyGen caKeyGen = new CertAndKeyGen(CryptoTools.getAsymmetricAlgorithm(),
				CryptoTools.getSignatureMethod());
		caKeyGen.generate(CryptoTools.getAsymmetricKeySize());
		PrivateKey caPrivateKey = caKeyGen.getPrivateKey();
		CertificateExtensions caExts = new CertificateExtensions();
		X500Name caX500Name = new X500Name("CN=Node Local las2peer Root CA, O=las2peer, OU=RWTH-ACIS");
		SubjectAlternativeNameExtension caNameExt = new SubjectAlternativeNameExtension(
				new GeneralNames().add(new GeneralName(new MyDNSName(hostname))));
		caExts.set(caNameExt.getName(), caNameExt);
		BasicConstraintsExtension bce = new BasicConstraintsExtension(true, 0);
		caExts.set(bce.getName(), bce);
		X509Certificate caCert = caKeyGen.getSelfCertificate(caX500Name, new Date(), 3 * 365 * 24 * 3600, caExts);
		ks.setKeyEntry(caX500Name.getCommonName(), caPrivateKey, password, new X509Certificate[] { caCert });
		// generate connector certificate signed by CA
		CertAndKeyGen keyGen = new CertAndKeyGen(CryptoTools.getAsymmetricAlgorithm(),
				CryptoTools.getSignatureMethod());
		keyGen.generate(CryptoTools.getAsymmetricKeySize());
		X500Name x500Name = new X500Name("CN=" + hostname + ", O=las2peer, OU=RWTH-ACIS");
		CertificateExtensions exts = new CertificateExtensions();
		SubjectAlternativeNameExtension nameExt = new SubjectAlternativeNameExtension(
				new GeneralNames().add(new GeneralName(new MyDNSName(hostname))));
		exts.set(nameExt.getName(), nameExt);
		X509Certificate certReq = keyGen.getSelfCertificate(x500Name, new Date(), 3 * 365 * 24 * 3600, exts);
		X509Certificate cert = createSignedCertificate(certReq, caCert, caPrivateKey);
		// the alias has to be the class name here
		ks.setKeyEntry(x500Name.getCommonName(), keyGen.getPrivateKey(), password,
				new X509Certificate[] { cert, caCert });
		// write keystore to file
		FileOutputStream fos = new FileOutputStream(keystoreFilename);
		ks.store(fos, password);
		fos.close();
		logger.log(Level.INFO, "Created Keystore at '" + keystoreFilename + "'");
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

	public static void writeCertificateToPEMStream(Certificate certificate, OutputStreamWriter outputStreamWriter)
			throws IOException, CertificateEncodingException {
		outputStreamWriter.write(X509Factory.BEGIN_CERT + "\n");
		outputStreamWriter.write(new BASE64Encoder().encodeBuffer(certificate.getEncoded()));
		outputStreamWriter.write(X509Factory.END_CERT);
	}

	public static void writeCertificateToPEMFile(Certificate certificate, String filename)
			throws IOException, CertificateEncodingException {
		FileWriter fw = new FileWriter(filename);
		writeCertificateToPEMStream(certificate, fw);
		fw.close();
	}

	/**
	 * This class is used as workaround to allow DNS Names to start with a number.
	 * 
	 * See https://bugs.openjdk.java.net/browse/JDK-8016345
	 * 
	 */
	// XXX remove workaround for JDK-8016345
	private static class MyDNSName implements GeneralNameInterface {

		private final String dnsName;

		public MyDNSName(String dnsName) {
			this.dnsName = dnsName;
		}

		@Override
		public int getType() {
			// same as in sun.security.X509.DNSName
			return (GeneralNameInterface.NAME_DNS);
		}

		@Override
		public void encode(DerOutputStream out) throws IOException {
			// same as in sun.security.X509.DNSName
			out.putIA5String(dnsName);
		}

		@Override
		public int constrains(GeneralNameInterface inputName) throws UnsupportedOperationException {
			// same as in sun.security.X509.DNSName
			int constraintType;
			if (inputName == null) {
				constraintType = NAME_DIFF_TYPE;
			} else if (inputName.getType() != NAME_DNS) {
				constraintType = NAME_DIFF_TYPE;
			} else {
				String inName = (((DNSName) inputName).getName()).toLowerCase(Locale.ENGLISH);
				String thisName = dnsName.toLowerCase(Locale.ENGLISH);
				if (inName.equals(thisName)) {
					constraintType = NAME_MATCH;
				} else if (thisName.endsWith(inName)) {
					int inNdx = thisName.lastIndexOf(inName);
					if (thisName.charAt(inNdx - 1) == '.') {
						constraintType = NAME_WIDENS;
					} else {
						constraintType = NAME_SAME_TYPE;
					}
				} else if (inName.endsWith(thisName)) {
					int ndx = inName.lastIndexOf(thisName);
					if (inName.charAt(ndx - 1) == '.') {
						constraintType = NAME_NARROWS;
					} else {
						constraintType = NAME_SAME_TYPE;
					}
				} else {
					constraintType = NAME_SAME_TYPE;
				}
			}
			return constraintType;
		}

		@Override
		public int subtreeDepth() throws UnsupportedOperationException {
			// same as in sun.security.X509.DNSName
			int sum = 1;
			for (int i = dnsName.indexOf('.'); i >= 0; i = dnsName.indexOf('.', i + 1)) {
				++sum;
			}
			return sum;
		}

		@Override
		public String toString() {
			// same as in sun.security.X509.DNSName
			return ("DNSName: " + dnsName);
		}

		@Override
		public boolean equals(Object obj) {
			// same as in sun.security.X509.DNSName
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof MyDNSName)) {
				return false;
			}
			MyDNSName other = (MyDNSName) obj;
			return dnsName.equalsIgnoreCase(other.dnsName);
		}

		@Override
		public int hashCode() {
			// same as in sun.security.X509.DNSName
			return dnsName.toUpperCase(Locale.ENGLISH).hashCode();
		}

	}

}
