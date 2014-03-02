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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import de.tsl2.nano.exception.ManagedException;

/**
 * asymmetric (RSA) en-/decryption. @see {@link SymmetricCipher}.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */

public class AsymmetricCipher extends SymmetricCipher {
    Key privateKey;

    /** RSA Algorithm */
    public static String ALG_RSA = "RSA";

    /** ECB Mode */
    public static String MODE_NONE = "NONE";

    /** PKCS5 padding */
    public static String PAD_PKCS1 = "PKCS1PADDING";

    /**
     * constructor using defaults
     */
    public AsymmetricCipher() {
        this(ALG_RSA, 512, MODE_NONE, PAD_NONE);
    }

    /**
     * constructor
     * 
     * @param algorithm
     * @param algKeySize
     * @param mode
     * @param padding
     */
    public AsymmetricCipher(String algorithm, int algKeySize, String mode, String padding) {
        super(algorithm, algKeySize, mode, padding);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Key getEncryptionKey() {
        if (key == null) {
            // Generate a key-pair
            KeyPairGenerator kpg;
            try {
                kpg = KeyPairGenerator.getInstance("RSA");
            } catch (final NoSuchAlgorithmException e) {
                ManagedException.forward(e);
                return null;
            }
            kpg.initialize(algKeySize); // 512 is the keysize.
            final KeyPair kp = kpg.generateKeyPair();
            key = kp.getPublic();
            privateKey = kp.getPrivate();
        }
        return key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Key getDecryptionKey() {
        //init key-pair
        getEncryptionKey();
        return privateKey;
    }

}
