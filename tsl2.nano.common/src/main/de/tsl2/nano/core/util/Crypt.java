package de.tsl2.nano.core.util;

import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
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
 * for further informations about available algorithms, see <a
 * href="http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#Cipher" /> <br/>
 * <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/security/crypto/CryptoSpec.html" />
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
        this(pwd == null || pwd.length == 0 ? generateRandomKey(algorithm) : isPBE(algorithm) ? generatePBEKey(
            toCharArray(pwd), algorithm) : generateSecretKey(pwd, algorithm),
            algorithm,
            ENCODE_UTF8, true);
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

    private static void preInit() {
        if (LOG.isDebugEnabled()) {
            LOG.debug(providers());
        }
    }

    private static String providers() {
        StringBuilder ps = new StringBuilder("available security providers:\n");
        Provider[] providers = Security.getProviders();
        for (int i = 0; i < providers.length; i++) {
            ps.append("\t" + providers[i].getInfo() + "\n");
        }
        return ps.toString();
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
        preInit();
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
        preInit();
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
        preInit();
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
     * encrypts data using {@link #key()}, {@link #algorithm}, {@link #encoding} - delegating to
     * {@link #encrypt(byte[], Key, String)}.
     * 
     * @param data to be encrypted
     * @return encrypted string
     */
    public String encrypt(String data) {
        try {
            return encrypt(data.getBytes(encoding), key(), paramSpec, algorithm, encoding, useBASE64);
        } catch (UnsupportedEncodingException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    static String encrypt(byte[] data,
            Key key,
            AlgorithmParameterSpec paramSpec,
            String algorithm,
            String encoding,
            boolean useBASE64) {
        try {

            // encrypt using the cypher
            byte[] raw = cipher(algorithm, Cipher.ENCRYPT_MODE, key, paramSpec).doFinal(data);

            // converts to base64 for easier display.
            return useBASE64 ? new BASE64Encoder().encode(raw) : new String(raw, encoding);
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }

    }

    static Cipher cipher(String algorithm, int mode, Key key, AlgorithmParameterSpec spec) throws Exception {
        Cipher cipher = Cipher.getInstance(algorithm);
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
        return decrypt(encrypted, key, paramSpec, algorithm, encoding, useBASE64);
    }

    static String decrypt(String encrypted,
            Key key,
            AlgorithmParameterSpec paramSpec,
            String algorithm,
            String encoding,
            boolean useBASE64) {

        try {
            //decode the BASE64 coded message
            byte[] raw = useBASE64 ? new BASE64Decoder().decodeBuffer(encrypted) : encrypted.getBytes(encoding);

            //decode the message
            byte[] bytes = cipher(algorithm, Cipher.DECRYPT_MODE, key, paramSpec).doFinal(raw);

            //converts the decoded message to a String
            return new String(bytes, encoding);
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        if (args.length == 3) {
            Crypt c = new Crypt(args[0].getBytes(), args[1]);
            String txt = args[2];
            System.out.println("encrypted:" + (txt = c.encrypt(txt)));
            System.out.println("decrypted:" + c.decrypt(txt));
        } else {
            System.out.println("usage: Crypt <key> <ALGORITHM> <text>");
            System.out.println("  example: Crypt mYpASsWord AES meintext");
            System.out.println("  algorithms are:");
            System.out
                .println(
                "  AES,AESWrap,ARCFOUR,Blowfish,CCM,DES,DESede,DESedeWrap,ECIES,GCM,PBEWith<digest>And<encryption>,RC2,RC4,RC5,RSA");
            System.out.println("  providers are:\n" + Crypt.providers());
            System.out
                .println("  for further informations see: http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#Cipher");
        }

    }
}
