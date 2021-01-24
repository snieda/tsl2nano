/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 17.05.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.core.secure;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderResult;
import java.security.cert.CertPathParameters;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.CertSelector;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.KeyManagerFactory;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.Argumentator;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.Util;
import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

/** <pre>
 * Public Key Infrastructure. creates and verifies Certificate Paths, by 
 * providing convenience methods on {@link CertificateFactory}, {@link CertPathBuilder} and {@link CertPathValidator}.
 * 
 * works with keystores, certificates and de-/encryption. can create signed certificates of type X.509. For java security properties,
 * see '${java_home}/jre/lib/security/java.security'. You can tell the jvm to use a keystore:
 * -Djavax.net.ssl.keyStore=...java/lib/security/cacerts and -Djavax.net.ssl.keyStorePassword=tobechanged.
 * 
 * use:
 * - create a certificate with path: {@link #createCertPath(TrustedOrganisation, TrustedOrganisation, Date, Date, PublicKey)}
 * - sign it with your roots private key: {@link #sign(InputStream, String, PrivateKey)}
 * - verify a certificate against its public key: {@link #verify(InputStream, byte[], PublicKey, String)}
 * - persist your keystore: {@link #peristKeyStore(KeyStore, String, String)}
 * 
 * Informations:
 * - a keystore is an encrypted store of certificates and/or keys. 
 *   it can be of type:
 *      'certificate path' (for a client certificate + its private key), 
 *      'trusted store' as collection of trusted certificates or 
 *      'secret key' as store of symmetric encryption keys.  
 * - a Cerficate Path is is the path of certicates from root issuer to the client certificate
 * - a Certificate contains the issuer, subject, meta informations (name, host, country, period), the public key and the signature of the issuer (to be verified through its public key)
 * - private keys can sign or decrypt data, and public keys can verify or encrypt data
 * 
 * Developer Annotations:
 * - to use another certificate factory format, set the system property 'tsl2nano.pki.certfactory.format' (default: X.509)
 *      override {@link #createCertPath(TrustedOrganisation, TrustedOrganisation, Date, Date, PublicKey)}
 *      provide a filled {@link CertSelector} and {@link CertPathParameters}
 * - to use another certificate path algorithm, set the system property 'tsl2nano.pki.certpath.algorithm' (default: PKIX)
 * - to use another keystore type, set the system property 'tsl2nano.pki.keystore.type' (default (PKCS12)
 * 
 * TODO: requests for Server-based Certificate Validation Protocol (SCVP)
 * TODO: requests for Online Certificate Status Protocol (OCSP)
 * </pre> 
 * @author Tom
 * @version $Revision$
 */
public class PKI {
	private static final Log LOG = LogFactory.getLog(PKI.class);

    KeyPair keyPair;
    TrustedOrganisation issuer;
    KeyStore keyStore;
    
	private static final String CERT_FACT_FORMAT = System.getProperty("tsl2nano.pki.certfactory.format", "X.509");
    private static final String CERT_PATH_ALG = System.getProperty("tsl2nano.pki.certpath.algorithm", "PKIX");
	private static final String KEYSTORE_TYPE = System.getProperty("tsl2nano.pki.keystore.type", "PKCS12");
	private static final String HASHSIGN_ALG = System.getProperty("tsl2nano.pki.hashsign.algorithm", "SHA256withRSA");

    /**
     * constructor
     */
    public PKI(KeyPair keyPair, TrustedOrganisation orga) {
        this.keyPair = keyPair;
        this.issuer = orga;
        LOG.info(toString());
    }

	private static CertificateFactory getCertificateFactory() throws CertificateException {
		return CertificateFactory.getInstance(CERT_FACT_FORMAT);
	}

    /**
     * creates the Certificate from certification data (see {@link CertificateFactory#generateCertificate(InputStream)}.
     * 
     * @param certEncoded certification data
     * @return certification instance
     */
    public Certificate createCertificate(InputStream certEncoded) {
        try {
            CertificateFactory certFactory = getCertificateFactory();
            return certFactory.generateCertificate(certEncoded);
        } catch (CertificateException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * Create a self-signed X.509 (copied from stackoverflow, using non-API methods from 'sun.security.x509') 
     *
     * @param dn        the X.509 Distinguished Name, eg "CN=Test, L=Berlin, C=DE"
     * @param pair      the KeyPair
     * @param days      days until expiring
     * @param algorithm the signing algorithm, eg "SHA256withRSA"
     */
    @SuppressWarnings("restriction")
	public X509Certificate generateCertificate(String dn, KeyPair pair, int days, String algorithm)
            throws GeneralSecurityException, IOException {
        PrivateKey privkey = pair.getPrivate();
        X509CertInfo info = new X509CertInfo();
        Date from = new Date();
        Date to = new Date(from.getTime() + days * 86400000l);
        CertificateValidity interval = new CertificateValidity(from, to);
        BigInteger sn = new BigInteger(64, new SecureRandom());
        X500Name owner = new X500Name(dn);

        info.set(X509CertInfo.VALIDITY, interval);
        info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
        info.set(X509CertInfo.SUBJECT, owner);
        info.set(X509CertInfo.ISSUER, owner);
        info.set(X509CertInfo.KEY, new CertificateX509Key(pair.getPublic()));
        info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
        AlgorithmId algo = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid);
        info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));

        // Sign the cert to identify the algorithm that's used.
        X509CertImpl cert = new X509CertImpl(info);
        cert.sign(privkey, algorithm);

        // Update the algorith, and resign.
        algo = (AlgorithmId) cert.get(X509CertImpl.SIG_ALG);
        info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo);
        cert = new X509CertImpl(info);
        cert.sign(privkey, algorithm);
        return cert;
    }

    /**
     * Returns true if the certificate is self-signed, false otherwise.
     */
    boolean isSelfSigned(X509Certificate cert) {
        return signedBy(cert, cert);
    }

    boolean signedBy(X509Certificate end, X509Certificate ca) {
        if (!ca.getSubjectDN().equals(end.getIssuerDN())) {
            return false;
        }
        try {
            end.verify(ca.getPublicKey());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * creates the CertPath from certifation data (see {@link CertificateFactory#generateCertificate(InputStream)}.
     * 
     * @param certEncoded certification data
     * @return certification instance
     */
    public static CertPath createCertPath(InputStream certEncoded) {
        try {
            CertificateFactory certFactory = getCertificateFactory();
            return certFactory.generateCertPath(certEncoded);
        } catch (CertificateException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    public CertPath createCertPath(TrustedOrganisation subjectDN,
            Date startDate,
            Date expiringDate) {
        return createCertPath(keyStore, subjectDN, issuer, startDate, expiringDate, keyPair.getPublic());
    }

    /**
     * creates a new Certification Path. To create a self-signed certificate, give an issuerDN = null.
     * 
     * @param subjectDN
     * @param issuerDN (optional) something like "O=xyz,C=us". if null, certificate is self-signed
     * @param startDate (optional) start
     * @param expiringDate (optional) end
     * @param subjectPublicKey
     * @return certificate
     */
    public CertPath createCertPath(KeyStore keyStore, 
    		TrustedOrganisation subjectDN,
            TrustedOrganisation issuerDN,
            Date startDate,
            Date expiringDate,
            PublicKey subjectPublicKey) {
        try {
            Set<TrustAnchor> trustAnchors = issuerDN != null
                ? Collections.singleton(new TrustAnchor(issuerDN.toString(), subjectPublicKey, null)) : new HashSet<>();
            CertSelector targetConstraints = createX509CertSelector(subjectDN, issuerDN, expiringDate, subjectPublicKey);
            CertPathParameters params = createPKIXBuilderParams(keyStore, startDate, trustAnchors, targetConstraints);
            return createCertPath(params);
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

	protected static PKIXBuilderParameters createPKIXBuilderParams(KeyStore keyStore, Date startDate, Set<TrustAnchor> trustAnchors,
			CertSelector targetConstraints) throws InvalidAlgorithmParameterException {
		PKIXBuilderParameters params = new PKIXBuilderParameters(trustAnchors, targetConstraints);
		params.setRevocationEnabled(true);
		params.setDate(startDate);
		//TODO: how to create and fill a certstore
		if (keyStore == null)
			throw new IllegalStateException("a keystore must be set and filled with a root certificate before creating a certpath!");
		params.setCertStores(Arrays.asList(getCertStore(keyStore)));
		return params;
	}

	private static CertStore getCertStore(KeyStore keyStore) {
	   ArrayList<Certificate> certsAndCrls = new ArrayList<>();
	   try {
	      Enumeration<String> aliases = keyStore.aliases();
	      while(aliases.hasMoreElements()) {
	         String alias = (String)aliases.nextElement();
	         X509Certificate cert = (X509Certificate)keyStore.getCertificate(alias);
	         LOG.debug("Adding " + cert.getSubjectX500Principal().getName("RFC1779"));
	         certsAndCrls.add(cert);
	      }
         return CertStore.getInstance("Collection", new CollectionCertStoreParameters(certsAndCrls));
	   } catch (Exception ex) {
	      ManagedException.forward(ex);
	      return null;
	   }

	}

	protected static X509CertSelector createX509CertSelector(TrustedOrganisation subjectDN, TrustedOrganisation issuerDN,
			Date expiringDate, PublicKey subjectPublicKey) {
		X509CertSelector targetConstraints = new X509CertSelector();
		if (issuerDN != null)
		    targetConstraints.setIssuer(issuerDN.toX500Principal());
		targetConstraints.setSubject(subjectDN.toX500Principal());
		targetConstraints.setSubjectPublicKey(subjectPublicKey);
		targetConstraints.setCertificateValid(expiringDate);
		// select only certificates with a digitalSignature (first bit = true)
		targetConstraints.setKeyUsage(new boolean[] { true });
		return targetConstraints;
	}

    public static CertPath createCertPath(CertPathParameters params) {
        // create CertPathBuilder that implements the "PKIX" algorithm
        try {
            CertPathBuilder cpb = CertPathBuilder.getInstance(CERT_PATH_ALG);
            CertPathBuilderResult cpbResult = cpb.build(params);
            CertPath cp = cpbResult.getCertPath();
            LOG.debug("build passed, path contents: " + cp);
            return cp;
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    public static CertPathValidatorResult verifyCertPath(CertPath cp, CertPathParameters params) {
        // create CertPathValidator that implements the "PKIX" algorithm
        CertPathValidator cpv = null;
        try {
            cpv = CertPathValidator.getInstance(CERT_PATH_ALG);
            // validate certification path ("cp") with specified parameters ("params")
            CertPathValidatorResult cpvResult = cpv.validate(cp, params);
            return cpvResult;
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * creates a public key from existing data perhaps read from a file.
     * 
     * @param encodedKey existing key data
     * @param algorithm
     * @return
     */
    public static Key createPublicKey(byte[] encodedKey, String algorithm) {
        try {
            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(encodedKey);
            KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
            return keyFactory.generatePublic(pubKeySpec);
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    public static long write(Certificate cert, OutputStream out) {
        try {
            return FileUtil.write(new ByteArrayInputStream(cert.getEncoded()), out, true);
        } catch (CertificateEncodingException e) {
            ManagedException.forward(e);
            return -1;
        }
    }

    public static long write(Key key, OutputStream out) {
        return FileUtil.write(new ByteArrayInputStream(key.getEncoded()), out, true);
    }

    /**
     * signs the given data. this instance must contain a privatekey as key.
     * 
     * @param data to create a signature for
     * @return signature
     */
    public byte[] sign(InputStream data) {
        return sign(data, keyPair.getPrivate());
    }

    public byte[] sign(InputStream data, PrivateKey privateKey) {
        return sign(data, HASHSIGN_ALG, privateKey);
    }

    /**
     * delegates to {@link #sign(InputStream, String, PrivateKey)} using the given file as data input stream
     */
    public static byte[] sign(String file, String algorithm, PrivateKey privateKey) {
        return sign(FileUtil.getFile(file), algorithm, privateKey);
    }

    /**
     * signs the given data with given algorithm and private key
     * 
     * @param data to create a signature for
     * @param algorithm signature algorithm e.g. SHA1withDSA
     * @param privateKey private key
     * @return signature for data
     */
    public static byte[] sign(InputStream data, String algorithm, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance(algorithm);
            signature.initSign(privateKey);
            BufferedInputStream bufin = new BufferedInputStream(data);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = bufin.read(buffer)) >= 0) {
                signature.update(buffer, 0, len);
            }
            bufin.close();
            return signature.sign();
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    public boolean verify(InputStream data, byte[] signature) {
        return verify(data, signature, keyPair.getPublic(), HASHSIGN_ALG);
    }

    /**
     * verifies the given signature against the given data
     * 
     * @param signature
     * @param algorithm e.g. SHA1withDSA
     */
    public static boolean verify(InputStream data, byte[] signature, PublicKey key, String algorithm) {
        try {
            Signature sig = Signature.getInstance(algorithm);
            sig.initVerify(key);

            //Supply the Signature Object With the Data to be Verified
            BufferedInputStream bufin = new BufferedInputStream(data);

            byte[] buffer = new byte[1024];
            int len;
            while (bufin.available() != 0) {
                len = bufin.read(buffer);
                sig.update(buffer, 0, len);
            }
            bufin.close();

            boolean verifies = sig.verify(signature);
            LOG.info("signature verifies: " + verifies);
            return verifies;
        } catch (Exception e) {
            ManagedException.forward(e);
            return false;
        }
    }

    public void addCertificate(String alias, Certificate cer) {
    	if (keyStore == null)
    		keyStore = createKeyStore();
    	try {
			keyStore.setCertificateEntry(alias, cer);
		} catch (KeyStoreException e) {
			ManagedException.forward(e);
		}
    }
    
    public void setKeyStore(KeyStore keyStore) {
    	this.keyStore = keyStore;
    }
    
    public static KeyStore createKeyStore() {
        return createKeyStore(null, null);
    }

    public static KeyStore createKeyStore(String file, char[] password) {
        return createKeyStore(KEYSTORE_TYPE, file, password);
    }

    /**
     * creates a new - or loads an existing {@link KeyStore} from file.
     * 
     * @param type keystore stype like PKCS12
     * @param file (optional) if not null, the file will be loaded as keystore. if null, a new keystore will be created
     * @param password (optional) keystore password, if file != null
     * @return keystore
     */
    public static KeyStore createKeyStore(String type, String file, char[] password) {
        InputStream is = null;
        try {
            KeyStore ks = KeyStore.getInstance(type);
            if (file != null)
            	if (new File(file).exists())
            		is = new FileInputStream(file);
            	else
            		is = Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
            ks.load(is, password);
            LOG.debug("keystore created: " + ks);
            return ks;
        } catch (Exception ex) {
            ManagedException.forward(ex);
            return null;
        } finally {
            if (is != null) {
                FileUtil.close(is, true);
            }
        }
    }

    /**
     * convenience to create a keymanagerfactory - e.g. for an SSLContext.init(...).
     * @param ks
     * @param password
     * @return
     */
    public static KeyManagerFactory getKeyManagerFactory(KeyStore ks, String password) {
    	try {
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(ks, password.toCharArray());
			return kmf;
		} catch (UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
			ManagedException.forward(e);
			return null;
		}    
    }
    
    /**
     * perists the given {@link KeyStore} to the given file. The keystore will be encrypted (see
     * {@link KeyStore#store(OutputStream, char[])}.
     * 
     * @param keyStore to be stored
     * @param file target file
     * @param password used for encryption.
     */
    public static void peristKeyStore(KeyStore keyStore, String file, String password) {
        try {
            keyStore.store(new FileOutputStream(file), password.toCharArray());
        } catch (Exception e) {
            ManagedException.forward(e);
        }
    }

    @Override
    public String toString() {
    	return Util.toString(getClass(), "\n\tttrusted-orga:", issuer, "\n\tFactoryFormat/CertPathAlg/KeystoreType/HashSignAlg: "
    			, CERT_FACT_FORMAT, CERT_PATH_ALG, KEYSTORE_TYPE, HASHSIGN_ALG);
    }
    
    @SuppressWarnings("unchecked")
	private static final Map<String, String> manual() {
        return MapUtil.asMap("help", "this help", 
        		"gencert", "creates a certificate : <subject-dn> [issuer-dn] [public-key]", 
        		"vercert", "verifies a certificate: <cert-file> <signature>");
    }

    public static void main(String[] args) {
        Argumentator am = new Argumentator("PKI", manual(), args);
        am.start(System.out, a -> {
//        	String certFile = (String) a.get("cert-file");
//        	byte[] signature = ((String)a.get("signature")).getBytes();
        	//TODO: implement? where to get keypair from?
//        	new PKI()
//			PKI.verify(FileUtil.getFile(certFile), signature);
            return PKI.class.getSimpleName() + " finished successfull!";
        });
    }
}
