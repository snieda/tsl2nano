package de.tsl2.nano.d8portal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Locale;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.execution.SystemUtil;
import de.tsl2.nano.core.secure.Crypt;
import de.tsl2.nano.core.secure.DistinguishedName;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MainProperties;
import de.tsl2.nano.core.util.Period;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.util.Mail;

/**
 * service for a server/client driven encrypted and versioned document store.
 * <p/>
 * the server is created and hold by an organisation to provide new documents to
 * its clients. clients itself may up- and download the encrypted documents.
 * <p/>
 * technical base for the store are git repositories per client. 
 * the public key infrastructure (PKI) is done by the java command line tool 'keytool',
 * the en-/decryption is provided by the tsl2nano class Crypt.
 * for more informations, see https://docs.oracle.com/javase/8/docs/technotes/tools/unix/keytool.html.
 * <p/>
 * the pki contains a self signed root certificate for the java application itself. the organisation
 * yields a root-signed ca certifcate to sign certificate requests by the client.
 * the repository of the organisation holds all keystores. the private key for the new client
 * repository will be sent by email to the client.
 * <p/>
 * NOTE: This simplified implementation belongs to oracles command line tool implementation 'keytool' 
 * and is used in linux shell command execution. So, it does only work on linux with a jdk  (>=8u301) of oracle!.
 * 
 */
public class RepositoryPortal implements IRepositoryPortal {
	private DistinguishedName orga;
    private KeyStore rootStore; // root ca store
    KeyStore orgaStore;   // own ca keystore
    KeyStore clientStore; // truststore
    private String rootPasswd;
    private String passwd;
    Mail mail = new Mail(System.out, "UTF8");
    String smtpServer;
    String publishOptions;
    private boolean mailEnabled;

    private static final MainProperties MY = new MainProperties(RepositoryPortal.class, "tsl2nano.portal.");
    static <T> T property(String name, T defValue) { return MY.geT(name, defValue);}

    static final String SIG_EXT = ".sig";
	static final boolean SIG_ALWAYS = property("verify.file.signature", true);

	static final String KS_TYPE = property("keystore.type", "pkcs12");
    static final String KS_EXT = "." + KS_TYPE.toLowerCase();
    static final String CERT_FILEEXTENSION = ".pem";
    static final String CERT_X509_EXT_BC_CA_0 = property("x509.ext.ca", "-ext bc:ca,pathlen:0"); // BasicConstraint: end CA (intermediate)
    static final String CERT_X509_EXT_BC_CA_1 = property("x509.ext.ca", "-ext bc:ca,pathlen:1"); // BasicConstraint: root CA
    static final String CERT_X509_EXT_SAN_DNS = property("x509.ext.dns", "-ext SAN=DNS:localhost,IP:127.0.0.1");

    // keytool scripts
    static final String KT_GENKEYPAIR = property("keytool.genkeypair", "keytool -genkeypair -v -noprompt -alias %s -keyalg RSA -keystore %s.pkcs12 " +
        "-dname %s -storepass %s -keypass %s %s  -validity 9999 -deststoretype pkcs12");
    static final String KT_REQUEST = property("keytool.certreq", "keytool -certreq -v -noprompt -keystore %s.pkcs12 -alias %s -storepass %s | " +
        "keytool -gencert -v -noprompt -keystore %s.pkcs12 -alias %s  -storepass %s -ext BC=0 -rfc -outfile %s.pem");
    static final String KT_EXPORT = property("keytool.export", "keytool -export -v -alias %s -storepass %s -file %s.pem -keystore %s.pkcs12 -rfc");
    static final String KT_IMPORT = property("keytool.importcert", "keytool -importcert -v -noprompt -keystore %s.pkcs12 -alias %s -storepass %s -file %s.pem");

    public void createRootCA() { // basic self signed
        rootStore = createCertificate(getRoot(), rootPasswd = Crypt.generatePassword((byte)24),
                new DistinguishedName(getRoot(), Locale.getDefault().getCountry()), CERT_X509_EXT_BC_CA_1, null, null);
    }

    private String getRoot() {
        return getClass().getSimpleName();
    }

    public void createOrganisation(String name, String email, String remoteUrl, String smtpServer) {
        this.smtpServer = smtpServer;
        String stateOrProvince = null;
        String locality = null;
        String streetAddress = null;
        orga = new DistinguishedName(remoteUrl, Locale.getDefault().getCountry(), stateOrProvince, locality, name, name,
                null, streetAddress, email);
        orga.setOutputSimpleValuesOnly();

        if (rootStore == null)
            createRootCA();

        // create and export the new ca certificate in a keystore (not possible through
        // KeyStore class!)
        passwd = Crypt.generatePassword((byte)24);
        orgaStore = createCertificate(name, passwd, orga, CERT_X509_EXT_BC_CA_0, getRoot(), rootPasswd);

        clientStore = Util.trY(() -> KeyStore.getInstance("PKCS12"));
        Util.trY(() -> clientStore.load(null));

        // upload the ca
        Repository rep = new Repository(name, remoteUrl, publishOptions);
        rep.create();
        rep.addFile(orga.getOrganizationName() + CERT_FILEEXTENSION);
        rep.publish();

    }

    void setPublishOptions(String publishParameter) {
        this.publishOptions = publishParameter;
    }

    private KeyStore createCertificate(String name, String passwd, DistinguishedName dname, String x509Ext,
            String parentCA, String parentCAPasswd) {
        // 1. generate keypair - includes a self-signed certifate to wrap the public key
        keytool(KT_GENKEYPAIR, name, name, dname, passwd, passwd, x509Ext);
        // 2. create a certificate request and pipe that into the ca keystore
        if (parentCA != null) {
            keytool(KT_REQUEST, name, name, passwd, parentCA, parentCA, parentCAPasswd, name);
            // 3. import the certifate chain into the the new keystore
            keytool(KT_IMPORT, name, name, passwd, name);
        } else {
            exportCertificate(name, passwd);
        }
        return loadKeyStore(getFolder(orga.getOrganizationName()).getPath() + "/" + name, passwd);
    }

    private void keytool(String cmd, Object... args) {
        SystemUtil.executeShell(getFolder(orga.getOrganizationName()), String.format(cmd, args));
    }

    private KeyStore loadKeyStore(String name, String passwd) {
        try {
            KeyStore keyStore = KeyStore.getInstance(KS_TYPE);
            keyStore.load(FileUtil.getFile(name + KS_EXT), passwd.toCharArray());
            return keyStore;
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    private void exportCertificate(String name, String passwd) {
        keytool(KT_EXPORT, name, passwd, name, name);
    }

    private File getFolder(String name) {
        File file = FileUtil.userDirFile(name);
        file.mkdirs();
        return file;
    }

    public void createRepository(String id, String name, String email, String password) {
        DistinguishedName dname = new DistinguishedName(name, name);
        // create client certificate from server certificate
        KeyStore privateKeyStore = createCertificate(id, password, dname, "", orga.getOrganizationName(),
                this.passwd);
        Util.trY(() -> clientStore.setCertificateEntry(id, privateKeyStore.getCertificate(id)));
        Util.trY(() -> clientStore.store(
                FileUtil.getFileOutput(getFolder(orga.getOrganizationName()) + "/" + orga.getOrganizationName() + "-clients" + KS_EXT),
                this.passwd.toCharArray()));

        // create repository with readme
        FileUtil.writeBytes(String.format("ID: %s\nNAME: %s\nEMAIL: %s\n", id, name, email).getBytes(), getFolder(id) + "/readme.md", false);
        Repository rep = new Repository(id, orga.getCommonName(), publishOptions);
        rep.create();
        rep.addFile("readme.md");
        rep.publish();
        // send the private key per email
        Key key = Util.trY(() -> privateKeyStore.getKey(id, password.toCharArray()));
        String keyEncoded = Util.trY(() -> new String(key.getEncoded(), "UTF8"));
        sendmail(email, keyEncoded);
    }

    void sendmail(String email, String keyEncoded) {
        if (!mailEnabled)
            return;
        mail.sendEmail(smtpServer, orga.getEmail(), orga.getEmail(), email, email,
                "account to " + orga.getOrganizationName(), keyEncoded);
    }

    public String upload(String id, String filename, String name, String type, Period period, String description) {
        // encrypt file
        Certificate cert = Util.trY(() -> clientStore.getCertificate(id));
        if (cert == null)
            throw new IllegalArgumentException("no client certificate found for id: " + id);
        Crypt crypt = new Crypt(cert.getPublicKey());
        InputStream stream = FileUtil.getFile(filename);
        InputStream encrypted = crypt.encrypt(stream);
        // create filename
        Repository rep = getClientRepository(id);
        String gitFilename = evalSpecFileName(name, type, period);
        String path = rep.getBaseDir() + "/" + gitFilename;

        // upload
        FileUtil.write(encrypted, path);
        rep.addFile(gitFilename);
        //sign the encrypted data hash (should we use the implementatino of PKI?)
        if (SIG_ALWAYS) { // create signature of orga
        	Crypt privateCrypt = getPrivateCrypt(orga.getOrganizationName(), passwd);
	        String signature = privateCrypt.sign(FileUtil.getFileString(filename));
	        FileUtil.writeBytes(signature.getBytes(), path + SIG_EXT, false);
	        rep.addFile(path + SIG_EXT);
        }
        rep.publish();

        // trigger emails (perhaps through the git provider?)

        return path;
    }

    @Override
    public byte[] download(String fileName, String id, String passwd) {
    	verify(fileName, id);
        Crypt crypt = getPrivateCrypt(id, passwd);
        return Util.trY(() -> FileUtil.readBytes(crypt.decrypt(FileUtil.getFile(fileName))));
    }

	InputStream decrypt(String fileName, String id, String passwd) {
    	verify(fileName, id);
        return getPrivateCrypt(id, passwd).decrypt(FileUtil.getFile(fileName));
    }

    private Repository getClientRepository(String id) {
        return new Repository(id, null, publishOptions);
    }

	private Crypt getPrivateCrypt(String id, String passwd) {
		KeyStore privateStore = loadKeyStore(getFolder(orga.getOrganizationName()).getPath() + "/" + id, passwd);
        Crypt crypt = new Crypt(Util.trY(() -> privateStore.getKey(id, passwd.toCharArray())));
		return crypt;
	}

    private void verify(String fileName, String id) {
    	String signature = loadSignature(fileName, id); // signature of organisation!
    	Crypt crypt = Util.trY(() -> new Crypt(orgaStore.getCertificate(orga.getOrganizationName()).getPublicKey()));
    	// TODO: verify not working yet!
//    	crypt.verify(FileUtil.getFileString(fileName), signature);
	}

    private String loadSignature(String fileName, String id) {
    	String sigFile = fileName + SIG_EXT;
    	if (!new File(sigFile).exists())
    		if (SIG_ALWAYS)
    			throw new IllegalStateException("cannot verify signature file " + sigFile);
    		else
    			return null;
		return FileUtil.getFileString(sigFile);
	}

	private String evalSpecFileName(String name, String type, Period period) {
        return Util.toString("-", type, name, "period", period, "ts", System.currentTimeMillis());
    }

    public List<String> synchronize(String id) {
        return newFiles(id);
    }

    private List<String> newFiles(String id) {
        Repository rep = getClientRepository(id);
        List<String> newfiles = rep.newFiles();
        rep.refresh();
        return newfiles;
    }

    public List<String> find(String id, String search) {
        // collect and filter names from git
        Repository rep = getClientRepository(id);
        return rep.lsFiles();
    }

    public Long createQRCode(String url) {
        return null;
    }

    public void setMailEnabled(boolean mailEnabled) {
        this.mailEnabled = mailEnabled;
	}
}
