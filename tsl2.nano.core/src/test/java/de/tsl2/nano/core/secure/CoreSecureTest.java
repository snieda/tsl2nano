package de.tsl2.nano.core.secure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.Certificate;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.X509Certificate;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.FileUtil;

public class CoreSecureTest implements ENVTestPreparation {

	@BeforeClass
	public static void setUp() {
		ENVTestPreparation.setUp("core", false);
	}

	@AfterClass
	public static void tearDown() {
		ENVTestPreparation.tearDown();
	}

    @Test
    public void testCrypt() throws Exception {
        String txt = "test1234";
        String testfile = ENV.getTempPath() + "testfile.txt";
        FileUtil.writeBytes(txt.getBytes(), testfile, false);

        Crypt.main(new String[0]);
        Crypt.main(new String[] { "", "DES", txt });
        Crypt.main(new String[] { "", "AES", txt });
        Crypt.main(new String[] { "", Crypt.ALGO_DES, txt });
        Crypt.main(new String[] { "", Crypt.ALGO_AES, txt });
        Crypt.main(new String[] { "0123456", Crypt.ALGO_PBEWithMD5AndDES, txt });
        Crypt.main(new String[] { "0123456", Crypt.ALGO_PBEWithMD5AndDES, "-file:" + testfile, "-include:\\w+" });
        Crypt.main(new String[] { "0123456", Crypt.ALGO_PBEWithMD5AndDES, "-file:" + testfile, "-base64",
            "-include:\\w+" });
        //not available on standard jdk:
//        Crypt.main(new String[]{"0123456", Crypt.ALGO_PBEWithHmacSHA1AndDESede, txt});
//        Crypt.main(new String[]{"0123456", Crypt.ALGO_PBEWithSHAAndAES, txt});

        KeyPair keyPair = Crypt.generateKeyPair("RSA"/*Crypt.getTransformationPath("RSA", "ECB", "PKCS1Padding")*/);
        Crypt sender = new Crypt(keyPair.getPublic());
        Crypt receiver = new Crypt(keyPair.getPrivate());
        String encrypted = sender.encrypt(txt);
        assertEquals(txt, receiver.decrypt(encrypted));
        
        //validate certificates, check signature
        try {
            sender = new Crypt(keyPair.getPrivate());
            receiver = new Crypt(keyPair.getPublic());
            String signature = sender.sign("test");
            receiver.verify("test", signature);
            receiver.validate(null);
        } catch (UnsupportedOperationException ex) {
            //validate() not implemented yet!
        }
    }

    @Test
    public void testPKI() throws Exception {
        System.out.println(Crypt.providers());
        String data = "test data";
        String passwd = Crypt.generatePassword((byte)8);
        DistinguishedName dn = new DistinguishedName("me", "de");
        KeyPair keyPair = Crypt.generateKeyPair("RSA");
        PKI pki = new PKI(keyPair, dn);
        
        //creating/loading/persisting keystores
        KeyStore newKeyStore = pki.createKeyStore();
        String fileKeyStore = ENV.getConfigPath() + "mystore";
        pki.peristKeyStore(newKeyStore, fileKeyStore, passwd);
        KeyStore keyStore = pki.createKeyStore(fileKeyStore, passwd.toCharArray());
        assertTrue(newKeyStore.size() == keyStore.size());

        Certificate x509RootSelfSignedCert = pki.generateCertificate(dn.toString(), keyPair, 9999, "SHA256withRSA");
        System.out.println(x509RootSelfSignedCert);
        String fileCert = ENV.getConfigPath() + "certificate.cer";
        pki.write(x509RootSelfSignedCert, new FileOutputStream(fileCert));
        
        String fileKey = ENV.getConfigPath() + "public.key";
        pki.write(keyPair.getPublic(), new FileOutputStream(fileKey));
        String filePrivate = ENV.getConfigPath() + "private.key";
        pki.write(keyPair.getPrivate(), new FileOutputStream(filePrivate));
        
        Certificate certificate = pki.createCertificate(FileUtil.getFile(fileCert));

        //validating, signing
        assertTrue(certificate instanceof X509Certificate);
        assertTrue(pki.isSelfSigned((X509Certificate) certificate));
        byte[] signature = pki.sign(new ByteArrayInputStream(data.getBytes()));
        assertTrue(pki.verify(new ByteArrayInputStream(data.getBytes()), signature));
        
        //...without content...
        assertTrue(pki.getKeyManagerFactory(keyStore, Crypt.generatePassword((byte)8)) != null);
        assertEquals(keyPair.getPublic(), pki.createPublicKey(keyPair.getPublic().getEncoded(), keyPair.getPublic().getAlgorithm()));
        
        PKI.main(new String[] {"help"});
        PKI.main(new String[] {"vercert", fileKeyStore, String.valueOf(signature)});
        
        //TODO: test create certpath
//        pki.createCertPath(FileUtil.getFile(fileCert));
        CertPath certPath = null;
		try {
			pki.addCertificate("root-selfsigned", certificate);
			certPath = pki.createCertPath(dn, null, null); //NOT WORKING YET!!
	        pki.write(certPath.getCertificates().get(0), new FileOutputStream(fileCert));
	        CertPathValidatorResult valResult = pki.verifyCertPath(certPath, new PKIXBuilderParameters(keyStore, null));
	        assertTrue(((PKIXCertPathValidatorResult)valResult).getPublicKey().equals(certificate.getPublicKey()));
		} catch (Exception e) {
			e.printStackTrace(); // create cert path without certificates in trust store does not work
		}
    }
    
    @Test
    public void testDistinguishedName() throws Exception {
        DistinguishedName to = new DistinguishedName("DC=*.test.de, CN=Test, L=Berlin, C=DE, O=TestOwner, OU=Owners Trustcenter, STREET=Buxdehuder Str. 9, ST=Ostrhein Suedfalen, E=info@testowner.de");
        assertEquals("DC=*.test.de", to.getDomainComponent());
        assertEquals("CN=Test", to.getCommonName());
        assertEquals("C=DE", to.getCountryName());
        assertEquals("L=Berlin", to.getLocalityName());
        assertEquals("O=TestOwner", to.getOrganizationName());
        assertEquals("OU=Owners Trustcenter", to.getOrganizationalUnitName());
        assertEquals("STREET=Buxdehuder Str. 9", to.getStreetAddress());
        assertEquals("ST=Ostrhein Suedfalen", to.getStateOrProvince());
        assertEquals("EMAILADDRESS=info@testowner.de", to.getEmail());
        
        DistinguishedName to1 = new DistinguishedName("DC=*.test.de, CN=Test, L=Berlin, C=DE, O=TestOwner, OU=Owners Trustcenter, STREET=Buxdehuder Str. 10, ST=Ostrhein Suedfalen, EMAILADDRESS=info@testowner.de");
        assertTrue(to.hashCode() != to1.hashCode());
        assertNotEquals(to, to1);
        assertNotEquals(to.toString(), to1.toString());
        assertNotEquals(to.toX500Name(), to1.toX500Name());
        assertNotEquals(to.toX500Principal(), to1.toX500Principal());
    }
}
