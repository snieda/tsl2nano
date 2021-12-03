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

import java.io.File;
import java.util.Arrays;

import org.simpleframework.xml.Attribute;

import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.core.util.StringUtil;
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

    /**
     * constructor
     */
    public Hash() {
        this("SHA-512");
    }

    /**
     * constructor
     * @param algorithm
     * @param length
     */
    public Hash(String algorithm) {
        super();
        this.algorithm = algorithm;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String encrypt(String data) {
        return String.valueOf(Util.cryptoHash(data.getBytes(), algorithm));
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

    public static void checkHash(File hashFile, Object...calculationParts) throws IllegalAccessException {
    	if (!hashFile.exists())
    		throw new IllegalStateException("please provide the right hash in the file " + hashFile.getAbsolutePath());
    	checkHash(hashFile.getName(), FileUtil.getFileString(hashFile.getAbsolutePath()), calculationParts);
    }
    
    /** calculates a hash through some reliable runtime parameters and the given calculationParts. compares the calculated 
     *  hash against the given hash throwing an exception if not equal.
     * @param label any text to be used by the exception
     * @param givenHash hash to be equal to the calculated one
     * @param calculationParts known parts of hash
     * @throws IllegalAccessException if calculated and given hash are not equal
     */
    public static void checkHash(String label, String givenHash, Object...calculationParts) throws IllegalAccessException {
    	if (!givenHash.equals(calculated(calculationParts)))
    		throw new IllegalAccessException(label + " not allowed with given " + givenHash);
    }

	private static String calculated(Object... calculationParts) {
		String runtime = String.valueOf(new char[] {'a', '3', 'G', '§', 'l', '8', 'P', '%', '#', 'r', '9', 'H', '0', '?', '/', 'l'})
			+ Arrays.toString(Thread.currentThread().getStackTrace())
//			+ Thread.currentThread().getContextClassLoader().toString() // only on special classloaders with toString()...
			;
		return StringUtil.toBase64(StringUtil.cryptoHash(new String(ObjectUtil.serialize(calculationParts)) + runtime));
	}
}
