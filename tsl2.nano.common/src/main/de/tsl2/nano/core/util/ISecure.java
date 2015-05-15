/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 11.05.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.core.util;

/**
 * base methods for encryption and decryption
 * 
 * @author Tom
 * @version $Revision$
 */
public interface ISecure {

    /**
     * encrypt
     * 
     * @param data
     * @return
     */
    String encrypt(String data);

    /**
     * decrypt
     * 
     * @param encrypted
     * @return
     */
    String decrypt(String encrypted);

    /**
     * canDecrypt
     * @return true, if decryption is possible
     */
    boolean canDecrypt();
}
