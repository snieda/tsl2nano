package de.tsl2.nano.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.logging.Log;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.classloader.NetworkClassLoader;
import de.tsl2.nano.core.execution.CompatibilityLayer;
import de.tsl2.nano.core.log.LogFactory;

/**
 * simple java encryption. algorithms should have the format "algorithm/mode/padding". padding should be used encrypting
 * fix byte blocks, on streams it is not necessary, modes are:
 * 
 * <pre>
 * - CBC (Cipher Block Chaining)
 * - CFB (Cipher Feedback Mode)
 * - OFB (Output Feedback Mode)
 * - ECB (Electronic Cookbook Mode) is a mode with no feedback
 * </pre>
 * 
 * <pre>
 * tested support for:
 * - DES (random key generation with {@link SecureRandom}, no spec parameter)
 * - AES ( {@link SecretKeySpec} + {@link IvParameterSpec})
 * - PBE ( {@link PBEKeySpec} + PBEParameterSpec)
 * </pre>
 * 
 * <pre>
 * Every implementation of the Java platform is required to support the following standard Cipher transformations with the keysizes in parentheses:
 * AES/CBC/NoPadding (128)
 * AES/CBC/PKCS5Padding (128)
 * AES/ECB/NoPadding (128)
 * AES/ECB/PKCS5Padding (128)
 * DES/CBC/NoPadding (56)
 * DES/CBC/PKCS5Padding (56)
 * DES/ECB/NoPadding (56)
 * DES/ECB/PKCS5Padding (56)
 * DESede/CBC/NoPadding (168)
 * DESede/CBC/PKCS5Padding (168)
 * DESede/ECB/NoPadding (168)
 * DESede/ECB/PKCS5Padding (168)
 * RSA/ECB/PKCS1Padding (1024, 2048)
 * RSA/ECB/OAEPWithSHA-1AndMGF1Padding (1024, 2048)
 * RSA/ECB/OAEPWithSHA-256AndMGF1Padding (1024, 2048)
 * These transformations are described in the Cipher section of the Java Cryptography Architecture Standard Algorithm Name Documentation. Consult the release documentation for your implementation to see if any other transformations are supported.
 * </pre>
 * 
 * for further informations about available algorithms, see <a
 * href="http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#Cipher" /> <br/>
 * <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/security/crypto/CryptoSpec.html" />
 * <p/>
 * TODO: don't fix the encoding to UTF-8. use the system property
 * 
 * @author Tom
 * @version $Revision$
 */
public class Crypt {
    private static final Log LOG = LogFactory.getLog(Crypt.class);
    private Key key;
    private String encoding;
    private String algorithm;
    private boolean useBASE64;
    private AlgorithmParameterSpec paramSpec;

    public static final String ENCODE_UTF8 = "UTF-8";
    public static final String ALGO_DES = "DES/ECB/PKCS5Padding";
    public static final String ALGO_AES = "AES/CBC/PKCS5Padding";
    public static final String ALGO_PBEWithSHAAndAES = "PBEWithSHAAndAES";
    public static final String ALGO_PBEWithMD5AndDES = "PBEWithMD5AndDES";
    public static final String ALGO_PBEWithHmacSHA1AndDESede = "PBEWithHmacSHA1AndDESede";

    // Salt
    private static final byte[] salt8 = {
        (byte) 0x71, (byte) 0x37, (byte) 0x30, (byte) 0x23,
        (byte) 0x81, (byte) 0xd7, (byte) 0xde, (byte) 0x89
    };

    private static final byte[] salt16 = {
        (byte) 0x71, (byte) 0x37, (byte) 0x30, (byte) 0x23,
        (byte) 0x45, (byte) 0x52, (byte) 0x01, (byte) 0x15,
        (byte) 0x63, (byte) 0x82, (byte) 0x27, (byte) 0x72,
        (byte) 0x81, (byte) 0xd7, (byte) 0xde, (byte) 0x89
    };

    /**
     * constructor using AES key generation, {@link #ALGO_AES} algorithm and {@link #ENCODE_UTF8} encoding.
     */
    public Crypt() {
        this(generateRandomKey("DES"), ALGO_DES, ENCODE_UTF8, true);
    }

    public Crypt(byte[] pwd) {
        this(generateSecretKey(pwd, pwd == null || pwd.length == 0 ? ALGO_DES : ALGO_AES),
            pwd.length == 0 ? ALGO_DES : ALGO_AES, ENCODE_UTF8, true);
    }

    public Crypt(byte[] pwd, String algorithm) {
        this(generateKey(pwd, algorithm),
            algorithm,
            ENCODE_UTF8, true);
    }

    static Key generateKey(byte[] pwd, String algorithm) {
        return pwd == null || pwd.length == 0 ? generateRandomKey(algorithm) : isPBE(algorithm) ? generatePBEKey(
            toCharArray(pwd), algorithm) : generateSecretKey(pwd, algorithm);
    }

    private static char[] toCharArray(byte[] pwd) {
        try {
            return new String(pwd, ENCODE_UTF8).toCharArray();
        } catch (UnsupportedEncodingException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * constructor
     * 
     * @param algorithm
     * @param key
     * @param encoding
     */
    public Crypt(Key key, String algorithm, String encoding, boolean useBASE64) {
        super();
        this.key = key;
        this.algorithm = algorithm;
        this.encoding = encoding;
        this.useBASE64 = useBASE64;
        paramSpec = createParamSpec(algorithm);
    }

    private static void preInit(String algorithm) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(providers());
        }
        provide(algorithm);
    }

    private static String providers() {
        StringBuilder ps = new StringBuilder("available security providers:\n");
        Provider[] providers = Security.getProviders();
        for (int i = 0; i < providers.length; i++) {
            ps.append("\t" + providers[i].getInfo() + "\n");
        }
        return ps.toString();
    }

    private static void provide(String algorithm) {
        try {
            SecretKeyFactory.getInstance(algorithm);
        } catch (Exception e) {
            downloadProvider(algorithm);
        }
    }
    
    private static void downloadProvider(String algorithm2) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl instanceof NetworkClassLoader) {
            //if available we try to download bouncycastle through maven
            new CompatibilityLayer().runOptionalMain("de.tsl2.nano.jarresolver.JarResolver", "org.bouncycastle");
        }
    }

    private static AlgorithmParameterSpec createParamSpec(String algorithm) {
        return isPBE(algorithm) ? createPBEParamSpec() : (algorithm.contains("/") && !algorithm.contains("ECB"))
            ? new IvParameterSpec(
                algorithm.startsWith("DES") ? salt8 : salt16) : null;
//        return isPBE(algorithm) ? createPBEParamSpec() : algorithm.startsWith("DES") || algorithm.contains("/ECB/")
//            ? null : createAESParamSpec();
    }

    private static AlgorithmParameterSpec createPBEParamSpec() {
        // Create PBE parameter set
        return new PBEParameterSpec(salt8, 20 /* Iteration count */);
    }

    /**
     * Generates the password based encryption key (PBE). using the given algorithm
     */
    private static Key generateSecretKey(byte[] pwd, String algorithm) {
        preInit(algorithm);
        try {
            SecretKeyFactory keyFac = SecretKeyFactory.getInstance(algorithm);
            return keyFac.generateSecret(new SecretKeySpec(pwd, algorithm));
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * Generates the password based encryption key (PBE). using the given algorithm
     */
    private static Key generatePBEKey(char[] pwd, String algorithm) {
        preInit(algorithm);
        try {
            PBEKeySpec pbeKeySpec = new PBEKeySpec(pwd);
            SecretKeyFactory keyFac = SecretKeyFactory.getInstance(algorithm);
            return keyFac.generateSecret(pbeKeySpec);
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * simple random key generation
     * 
     * @param algorithm
     * @return
     */
    private static Key generateRandomKey(String algorithm) {
        preInit(algorithm);
        try {
            //for key generation use only the algorithm name like AES (not AES/CBC/PKCS5Padding)
            algorithm = StringUtil.substring(algorithm, null, "/");
            KeyGenerator generator = KeyGenerator.getInstance(algorithm);
            generator.init(new SecureRandom());
            return generator.generateKey();
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    static boolean isPBE(String algorithm) {
        return algorithm.startsWith("PBE");
    }

    private Key key() {
//        if (key == null)
//            key = generateKey("DES");
        return key;
    }

    /**
     * encryption is delegated to {@link #encrypt(byte[], Key, AlgorithmParameterSpec, String, String, boolean)}
     * 
     * @param stream byte stream
     * @param contentExpression regular expression to evaluate which bytes should be encrypted.
     * @return encrypted stream
     */
    public InputStream encrypt(InputStream stream,
            String contentExpression) {
        try {
            return new CipherInputStream(stream, cipher(algorithm, Cipher.ENCRYPT_MODE, key, paramSpec)) {
//                BASE64Encoder encoder = new BASE64Encoder();
                @Override
                public int read(byte[] arg0, int arg1, int arg2) throws IOException {
                    int l = super.read(arg0, arg1, arg2);
//                    encoder.encode(
                    return l;
                }
            };
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * decryption is delegated to {@link #decrypt(String, Key, AlgorithmParameterSpec, String, String, boolean)}
     * 
     * @param stream byte stream
     * @param contentExpression regular expression to evaluate which bytes should be encrypted.
     * @return decrypted stream
     */
    public InputStream decrypt(InputStream stream,
            String contentExpression) {
        try {
            return new CipherInputStream(stream, cipher(algorithm, Cipher.DECRYPT_MODE, key, paramSpec)) {
                @Override
                public int read(byte[] arg0, int arg1, int arg2) throws IOException {
                    int l = super.read(arg0, arg1, arg2);
                    return l;
                }
            };
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * encrypts data using {@link #key()}, {@link #algorithm}, {@link #encoding} - delegating to
     * {@link #encrypt(byte[], Key, String)}.
     * 
     * @param data to be encrypted
     * @return encrypted string
     */
    public String encrypt(String data) {
        try {
            return encrypt(data.getBytes(encoding), key(), paramSpec, algorithm, encoding, useBASE64, 0, data.length());
        } catch (UnsupportedEncodingException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    public static String encrypt(String data,
            String key,
            String algorithm) {
        return encrypt(data.getBytes(), generateKey(key.getBytes(), algorithm), createParamSpec(algorithm),
            algorithm, ENCODE_UTF8, true, 0, data.length());
    }

    static String encrypt(byte[] data,
            Key key,
            String algorithm) {
        return encrypt(data, key, createParamSpec(algorithm), algorithm, ENCODE_UTF8, true, 0, data.length);
    }

    static String encrypt(byte[] data,
            Key key,
            AlgorithmParameterSpec paramSpec,
            String algorithm,
            String encoding,
            boolean useBASE64,
            int offset,
            int length) {
        try {

            // encrypt using the cypher
            byte[] raw = cipher(algorithm, Cipher.ENCRYPT_MODE, key, paramSpec).doFinal(data, offset, length);

            // converts to base64 for easier display.
            return useBASE64 ? encodeBase64(raw) : new String(raw, encoding);
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }

    }

    static String encodeBase64(byte[] raw) {
        return new BASE64Encoder().encode(raw);
    }

    static Cipher cipher(String algorithm, int mode, Key key, AlgorithmParameterSpec spec) throws Exception {
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance(algorithm);
        } catch (NoSuchAlgorithmException ex) {
            downloadProvider(algorithm);
            cipher = Cipher.getInstance(algorithm);
            if (cipher == null)
                throw ex;
        }
        cipher.init(mode, key, spec);
        return cipher;
    }

    /**
     * decrypt encrypted string using {@link #key()}, {@link #algorithm}, {@link #encoding}
     * 
     * @param encrypted to be decrypted
     * @return decrypted string
     */
    public String decrypt(String encrypted) {
        return decrypt(encrypted, key, paramSpec, algorithm, encoding, useBASE64, 0, encrypted.length());
    }

    public static String decrypt(String encrypted,
            String key,
            String algorithm) {
        return decrypt(encrypted, generateKey(key.getBytes(), algorithm), createParamSpec(algorithm), algorithm,
            ENCODE_UTF8, true, 0, encrypted.length());
    }

    static String decrypt(String encrypted,
            Key key,
            AlgorithmParameterSpec paramSpec,
            String algorithm) {
        return decrypt(encrypted, key, paramSpec, algorithm, ENCODE_UTF8, true, 0, encrypted.length());
    }

    static String decrypt(String encrypted,
            Key key,
            AlgorithmParameterSpec paramSpec,
            String algorithm,
            String encoding,
            boolean useBASE64,
            int offset,
            int length) {

        try {
            //decode the BASE64 coded message
            byte[] raw = useBASE64 ? decodeBase64(encrypted) : encrypted.getBytes(encoding);

            //TODO: check the offset
            length = length > raw.length ? raw.length : length;
            //decode the message
            byte[] bytes = cipher(algorithm, Cipher.DECRYPT_MODE, key, paramSpec).doFinal(raw, offset, length);

            //converts the decoded message to a String
            return new String(bytes, encoding);
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    static byte[] decodeBase64(String encrypted) {
        try {
            return new BASE64Decoder().decodeBuffer(encrypted);
        } catch (IOException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    private static final void log(String txt) {
        System.out.println(txt);
    }

    static <T> T getData(String arg, Class<T> type) {
        if (arg.startsWith("-file:")) {
            arg = getFileName(arg);
            if (InputStream.class.isAssignableFrom(type))
                return (T) FileUtil.getFile(arg);
            else if (String.class.isAssignableFrom(type))
                return (T) new String(FileUtil.getFileBytes(arg, null));
            else
                throw new IllegalArgumentException(type + " not allowed!");
        } else
            return (T) arg;
    }

    static String getFileName(String arg) {
        return StringUtil.substring(arg, "-file:", null);
    }

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        if (args.length == 4 && args[0].equals("hash")) {
            log(args[2]
                + ":"
                + StringUtil.toHexString(StringUtil.cryptoHash(getData(args[3], String.class), args[2],
                    Integer.valueOf(args[1]))));
        } else if (args.length >= 3) {
            Crypt c = new Crypt(args[0].getBytes(), args[1]);
            String txt = args[2];
            if (txt.startsWith("-file:")) {
                boolean base64 = args.length > 3 ? args[3].equals("-base64") : false;
                int ii = base64 ? 4 : 3;
                String include = args.length > ii ? args[ii] : null;
                InputStream stream = c.encrypt(getData(txt, InputStream.class), include);
                if (base64) { //TODO: would it be possible with a stream, not a byte array <-- performance ?
                    byte[] bytes = encodeBase64(ByteUtil.toByteArray(stream)).getBytes();
                    FileUtil.writeBytes(bytes, getFileName(txt) + ".encrypted", false);

                    stream = c.decrypt(ByteUtil.getInputStream(decodeBase64(new String(bytes))), include);
                    FileUtil.write(stream, getFileName(txt) + ".decrypted");
                } else {
                    FileUtil.write(stream, getFileName(txt) + ".encrypted");

                    stream = c.decrypt(getData(txt + ".encrypted", InputStream.class), include);
                    FileUtil.write(stream, getFileName(txt) + ".decrypted");
                }
            } else {
                log("encrypted:" + (txt = c.encrypt(txt)));
                log("decrypted:" + c.decrypt(txt));
            }
        } else {
            log("usage: Crypt <key|'hash' length> <ALGORITHM> <text|-file:filname [-base64] [-include:regexp]>");
            log("  example 1: Crypt mYpASsWord AES meintext");
            log("  example 2: Crypt hash 32 MD5 meintext");
            log("  example 3: Crypt mYpASsWord AES -file:meintextfile.txt");
            log("  example 4: Crypt mYpASsWord AES -file:meintextfile.txt -base64 -include:[^;]+");
            log("  algorithms are:");
            log(
            "  AES,AESWrap,ARCFOUR,Blowfish,CCM,DES,DESede,DESedeWrap,ECIES,GCM,PBEWith<digest>And<encryption>,RC2,RC4,RC5,RSA\n"
                + "  Hash: MD2, MD5, SHA, SHA-1, SHA-256, SHA-384, SHA-512");
            log("  providers are:\n" + Crypt.providers());
            log("  for further informations see: http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#Cipher");
        }
    }
}
