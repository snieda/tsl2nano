/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 30.10.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.core.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import de.tsl2.nano.core.ManagedException;

/**
 * provides standard en-/decryption functions
 * 
 * @author Tom
 * @version $Revision$
 */
public class EncryptUtil extends Util {
    private static final String DEFAULT_ALGORITHM = "AES/ECB/PKCS5Padding";

    /**
     * delegates to {@link #encrypt(String, String, byte[])} using {@link #DEFAULT_ALGORITHM}.
     */
    public static final byte[] encrypt(String key, byte[] data) {
        return encrypt(key, DEFAULT_ALGORITHM, data);
    }

    /**
     * encrypt
     * 
     * @param key certificate
     * @param algorithm transformation
     * @param data data to encrypt
     * @return encrypted data
     */
    public static final byte[] encrypt(String key, String algorithm, byte[] data) {
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), algorithm);
        try {
            Cipher c = Cipher.getInstance(algorithm);
            c.init(Cipher.ENCRYPT_MODE, keySpec);
            //TODO: does this the whole encryption or do we need to encrypt through f.e. Base64.encryp(..)?
            //return Base64.encodeBytes(c.doFinal(data));
            return c.doFinal(data);
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * delegates to {@link #decrypt(String, String, byte[])} using {@link #DEFAULT_ALGORITHM}
     */
    public static final byte[] decrypt(String key, byte[] data) {
        return decrypt(key, DEFAULT_ALGORITHM, data);
    }

    /**
     * decrypt
     * 
     * @param key certificate
     * @param algorithm transformation
     * @param data data to encrypt
     * @return encrypted data
     */
    public static final byte[] decrypt(String key, String algorithm, byte[] data) {
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), algorithm);
        try {
            Cipher c = Cipher.getInstance(algorithm);
            c.init(Cipher.DECRYPT_MODE, keySpec);
            //TODO: does this the whole encryption or do we need to decrypt through f.e. Base64.decryp(..)?
            //return Base64.decodeBytes(c.doFinal(data));
            return c.doFinal(data);
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }
}
