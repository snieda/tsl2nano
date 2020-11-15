package de.tsl2.nano.core.secure;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.security.KeyPair;
import java.security.KeyStore;

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

        //validate certificates, check signification
        // TODO: implement and test!
        KeyPair keyPair = Crypt.generateKeyPair("RSA"/*Crypt.getTransformationPath("RSA", "ECB", "PKCS1Padding")*/);
        try {
            Crypt sender = new Crypt(keyPair.getPublic());
            Crypt receiver = new Crypt(keyPair.getPrivate());
            byte[] sign = sender.sign("test".getBytes(), "SHA-256");
            receiver.validate(null);
            receiver.checkSignification("test".getBytes(), sign, "SHA-256");
        } catch (UnsupportedOperationException ex) {
            //not implemented yet!
        } catch (IllegalArgumentException ex) {
            //TODO: what's wrong with 3f...
        }
    }

    @Test
    public void testPKI() throws Exception {
        System.out.println(Crypt.providers());
        String data = "test data";
        String passwd = Crypt.generatePassword(8);
        TrustedOrganisation dn = new TrustedOrganisation("me", "de");
        KeyPair keyPair = Crypt.generateKeyPair("RSA");
        PKI pki = new PKI(new Crypt(keyPair.getPublic()), dn);
        
        //creating/loading/persisting keystores
        KeyStore newKeyStore = pki.createKeyStore();
        String file = ENV.getConfigPath() + "mystore";
        pki.peristKeyStore(newKeyStore, file, passwd);
        KeyStore keyStore = pki.createKeyStore(file, passwd.toCharArray());
        assertTrue(newKeyStore.size() == keyStore.size());
        
        //TODO: test certificates
//        CertPath certPath = pki.createCertPath(dn, null, null);
//        pki.write(certPath.getCertificates().get(0), new FileOutputStream(file));
//        Certificate certificate = pki.createCertificate(FileUtil.getFile(file));
//        CertPathValidatorResult valResult = pki.verifyCertPath(certPath, new PKIXBuilderParameters(keyStore, null));
//        assertTrue(((PKIXCertPathValidatorResult)valResult).getPublicKey().equals(certificate.getPublicKey()));

        //validating, signing
        byte[] signature = pki.sign(new ByteArrayInputStream(data.getBytes()), "SHA1withRSA", keyPair.getPrivate());
        assertTrue(pki.verify(new ByteArrayInputStream(data.getBytes()), signature, keyPair.getPublic(), "SHA1withRSA"));
        
    }
    
//    @Test
//    public void testSymEncryption() throws Exception {
//        SymmetricCipher c = new SymmetricCipher();
//        System.out.println(c.getTransformationPath());
//        System.out.println(c.getAlgorithmParameterSpec());
//        
//        String data = "mein wichtiger text";
//        byte[] encrypted = c.encrypt(data.getBytes());
//        byte[] decrypted = c.decrypt(encrypted);
//        System.out.println("Symmetric encryption:\n\t" + data + " --> " + new String(encrypted) + " --> " + new String(decrypted));
//        assertTrue(data.equals(new String(decrypted)));
//    }
//    
//    @Test
//    public void testAsymEncryption() throws Exception {
//        AsymmetricCipher c = new AsymmetricCipher();
//        System.out.println(c.getTransformationPath());
//        System.out.println(c.getAlgorithmParameterSpec());
//        
//        String data = "mein wichtiger text";
//        byte[] encrypted = c.encrypt(data.getBytes());
//        byte[] decrypted = c.decrypt(encrypted);
//        System.out.println("Asymmetric encryption:\n\t" + data + " --> " + new String(encrypted) + " --> " + new String(decrypted));
//        assertTrue(data.equals(new String(decrypted)));
//    }
//    
    @Test
    public void testBaseTest() throws Exception {
        //TODO
    }
}
