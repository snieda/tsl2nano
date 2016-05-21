/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 17.05.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.core.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderResult;
import java.security.cert.CertPathParameters;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.Argumentator;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;

/**
 * works with keystores, certificates and de-/encryption. can create signed certificates.
 * 
 * @author Tom
 * @version $Revision$
 */
public class PKI {
    private static final Log LOG = LogFactory.getLog(Crypt.class);

    Crypt crypt;
    TrustedOrganisation issuer;
    
    /**
     * constructor
     */
    public PKI(Crypt crypt, TrustedOrganisation orga) {
        this.crypt = crypt;
        this.issuer = orga;
    }

    /**
     * creates the Certificate from certifation data (see {@link CertificateFactory#generateCertificate(InputStream)}.
     * 
     * @param certEncoded certification data
     * @return certification instance
     */
    public Certificate createCertificate(InputStream certEncoded) {
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            return certFactory.generateCertificate(certEncoded);
        } catch (CertificateException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * creates the Certificate from certifation data (see {@link CertificateFactory#generateCertificate(InputStream)}.
     * 
     * @param certEncoded certification data
     * @return certification instance
     */
    public static CertPath createCertPath(InputStream certEncoded) {
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            return certFactory.generateCertPath(certEncoded);
        } catch (CertificateException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    public CertPath createCertPath(TrustedOrganisation subjectDN,
            Date startDate,
            Date expiringDate) {
        return createCertPath(subjectDN, issuer, startDate, expiringDate, (PublicKey) crypt.key);
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
    public static CertPath createCertPath(TrustedOrganisation subjectDN,
            TrustedOrganisation issuerDN,
            Date startDate,
            Date expiringDate,
            PublicKey subjectPublicKey) {
        try {
            Set<TrustAnchor> trustAnchors = issuerDN != null ?
                Collections.singleton(new TrustAnchor(issuerDN.toString(), subjectPublicKey, null)) : null;
            X509CertSelector targetConstraints = new X509CertSelector();
            if (issuerDN != null)
                targetConstraints.setIssuer(issuerDN.toX500Principal());
            targetConstraints.setSubject(subjectDN.toX500Principal());
            targetConstraints.setSubjectPublicKey(subjectPublicKey);
            targetConstraints.setCertificateValid(expiringDate);
         // select only certificates with a digitalSignature (first bit = true)
            targetConstraints.setKeyUsage(new boolean[]{true});
            PKIXBuilderParameters params = new PKIXBuilderParameters(trustAnchors, targetConstraints);
            params.setRevocationEnabled(true);
            params.setDate(startDate);
            return createCertPath(params);
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    public static CertPath createCertPath(CertPathParameters params) {
        // create CertPathBuilder that implements the "PKIX" algorithm
        try {
            CertPathBuilder cpb = CertPathBuilder.getInstance("PKIX");
            // build certification path using specified parameters ("params")
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
            cpv = CertPathValidator.getInstance("PKIX");
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
        return sign(data, (PrivateKey) crypt.key);
    }

    public byte[] sign(InputStream data, PrivateKey privateKey) {
        return sign(data, crypt.algorithm, privateKey);
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
        return verify(data, signature, (PublicKey) crypt.key, crypt.algorithm);
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

    public static KeyStore createKeyStore() {
        return createKeyStore(null, null);
    }
    public static KeyStore createKeyStore(String file, char[] password) {
        return createKeyStore("PKCS12", file, password);
    }
    
    /**
     * creates a new - or loads an existing {@link KeyStore} from file.
     * 
     * @param type
     * @param file (optional) if not null, the file will be loaded as keystore. if null, a new keystore will be created
     * @param password (optional) keystore password, if file != null
     * @return keystore
     */
    public static KeyStore createKeyStore(String type, String file, char[] password) {
        java.io.FileInputStream fis = null;
        try {
            KeyStore ks = KeyStore.getInstance(type);
            if (file != null)
                fis = new FileInputStream(file);
            ks.load(fis, password);
            LOG.debug("keystore created: " + ks);
            return ks;
        } catch (Exception ex) {
            ManagedException.forward(ex);
            return null;
        } finally {
            if (fis != null) {
                FileUtil.close(fis, true);
            }
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
    
    private static final Map<String, String> manual() {
        return MapUtil.asMap("help", "this help"
            , "gencert", "creates a certificate : <subject-dn> [issuer-dn] [public-key]"
            , "vercert", "verifies a certificate: <cert-file>");
    }
    public static void main(String[] args) {
        new Argumentator("PKI", manual(), args);
    }
}
