/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: May 17, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.encryption;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;

import de.tsl2.nano.exception.ManagedException;

/**
 * symmetric en-/decryption
 * 
 * @see http://java.sun.com/j2se/1.4.2/docs/guide/security/jce/JCERefGuide.html#JceKeystore
 * @see http://docs.oracle.com/javase/6/docs/technotes/guides/security/SunProviders.html
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class SymmetricCipher {
    String algorithm;
    int algKeySize;
    String mode;
    String padding;

    //dynamic values
    protected AlgorithmParameterSpec algorithmParameterSpec;
    protected Key key;

    private static byte[] iv = { 0x0a, 0x01, 0x02, 0x03, 0x04, 0x0b, 0x0c, 0x0d };

    /** DES Algorithm */
    public static String ALG_DES = "DES";

    /** ECB Mode */
    public static String MODE_ECB = "ECB";

    /** ECB Mode */
    public static String MODE_CFB = "CFB";

    /** ECB Mode */
    public static String MODE_OFB = "OFB";

    /** PKCS5 padding */
    public static String PAD_PKCS5 = "PKCS5Padding";

    /** no padding */
    public static String PAD_NONE = "NoPadding";
    
    /**
     * constructor using defaults
     */
    public SymmetricCipher() {
        this(ALG_DES, 56, MODE_ECB, PAD_PKCS5);
    }

    /**
     * constructor
     * 
     * @param algorithm alg
     * @param algKeySize size
     * @param mode mode
     * @param padding pad
     */
    public SymmetricCipher(String algorithm, int algKeySize, String mode, String padding) {
        super();
        this.algorithm = algorithm;
        this.algKeySize = algKeySize;
        this.mode = mode;
        this.padding = padding;
    }

    protected void init() {
        // Generate a secret key
    }

    /**
     * createKey
     * 
     * @return
     */
    protected Key getEncryptionKey() {
        if (key == null) {
            KeyGenerator kg;
            try {
                kg = KeyGenerator.getInstance("DES");
            } catch (final NoSuchAlgorithmException e) {
                ManagedException.forward(e);
                return null;
            }
            kg.init(algKeySize); // 56 is the keysize. Fixed for DES
            return kg.generateKey();
        }
        return key;
    }

    /**
     * on symmetric cipher it is same as the encryption key.
     * 
     * @return decryption key
     */
    protected Key getDecryptionKey() {
        return getEncryptionKey();
    }

    /**
     * algorithm/mode/padding to be used on cipher
     * 
     * @param algorithm alg
     * @param mode mode
     * @param padding pad
     * @return path
     */
    public static String getTransformationPath(String algorithm, String mode, String padding) {
        return algorithm + "/" + mode + "/" + padding;
    }

    /**
     * getTransformationPath
     * 
     * @return path
     */
    public String getTransformationPath() {
        return algorithm + "/" + mode + "/" + padding;
    }

    public AlgorithmParameterSpec getAlgorithmParameterSpec() {
        if (algorithmParameterSpec == null) {
            algorithmParameterSpec = new IvParameterSpec(iv);
        }
        return algorithmParameterSpec;

    }

    /**
     * encrypt
     * 
     * @param data data to encrypt
     * @return encrypted data
     * @throws Exception on any error
     */
    public byte[] encrypt(byte[] data) throws Exception {
        final Cipher cipher = Cipher.getInstance(getTransformationPath());
        cipher.init(Cipher.ENCRYPT_MODE, getEncryptionKey(), getAlgorithmParameterSpec());
        return cipher.doFinal(data);
    }

    /**
     * decrypt
     * 
     * @param data data to decrypt
     * @return decrypted data
     * @throws Exception on any error
     */
    public byte[] decrypt(byte[] data) throws Exception {
        final Cipher cipher = Cipher.getInstance(getTransformationPath());
        cipher.init(Cipher.DECRYPT_MODE, getDecryptionKey(), getAlgorithmParameterSpec());
        return cipher.doFinal(data);
    }
}
