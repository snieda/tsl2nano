/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 11.05.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.core.secure;

import org.simpleframework.xml.Attribute;

import de.tsl2.nano.core.util.Util;

/**
 * Provides secure hashing of data
 * 
 * @author Tom
 * @version $Revision$
 */
public class Hash implements ISecure {

    /** algorithm */
    @Attribute
    private String algorithm;
    /** length */
    @Attribute
    private int length;

    /**
     * constructor
     */
    public Hash() {
        this("SHA", 32);
    }

    /**
     * constructor
     * @param algorithm
     * @param length
     */
    public Hash(String algorithm, int length) {
        super();
        this.algorithm = algorithm;
        this.length = length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String encrypt(String data) {
        return String.valueOf(Util.cryptoHash(data.getBytes(), algorithm, length));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String decrypt(String encrypted) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canDecrypt() {
        return false;
    }
}
