/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 05.12.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.util;

import java.security.MessageDigest;
import java.util.Arrays;

import de.tsl2.nano.exception.ForwardedException;

/**
 * utils for general purpose on simple objects.
 * 
 * @author Tom
 * @version $Revision$
 */
public class Util {
    
    protected Util() {
    }
    
    /**
     * isAllNull
     * @param objects objects to check
     * @return true, if all objects are null
     */
    public static boolean isAllNull(Object... objects) {
        checkParameterCount(objects, 1);
        for (Object o : objects) {
            if (o != null)
                return false;
        }
        return true;
    }

    /**
     * hasNull
     * @param objects objects to check
     * @return true, if at least one object is null
     */
    public static boolean hasNull(Object... objects) {
        checkParameterCount(objects, 1);
        for (Object o : objects) {
            if (o != null)
                return false;
        }
        return true;
    }

    protected static void checkParameterCount(Object[] objects, int i) {
        if (objects == null || objects.length < i)
            throw new IllegalArgumentException("at least " + i + " parameter must be given!");
    }
    
    /**
     * isEmpty
     * 
     * @param object object to analyze
     * @return true, if object is null or empty
     */
    public static final boolean isEmpty(Object object) {
        return isEmpty(object, false);
    }

    /**
     * isEmpty
     * 
     * @param object object to analyze
     * @return true, if object is null or empty
     */
    public static final boolean isEmpty(Object object, boolean trim) {
        return object == null || (trim ? object.toString().trim().isEmpty() : object.toString().isEmpty());
    }

    /**
     * checks, whether data is contained in c.
     * 
     * @param entry to check
     * @param elements collection of available entries
     * @return true, if data is equal to one of c.
     */
    public static final boolean in(Object entry, Object... elements) {
        checkParameterCount(elements, 1);
        return Arrays.asList(elements).contains(entry);
    }

    /**
     * asString
     * 
     * @param obj obj to call toString() if not null
     * @return obj.toString() or null
     */
    public static final String asString(Object obj) {
        return obj != null ? obj.toString() : null;
    }

    /**
     * getCryptoHash
     * 
     * @param data
     * @return
     */
    public static final byte[] cryptoHash(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA").digest(data);
        } catch (Exception e) {
            ForwardedException.forward(e);
            return null;
        }
    }

    /**
     * evaluates hashcode of all objects
     * @param objects objects to check
     * @return combined hashcode
     */
    public static int hashCode(Object...objects) {
        checkParameterCount(objects, 1);
        return Arrays.hashCode(objects);
    }
    
    /**
     * checks, if all objects are equal
     * @param objects objects to check
     * @return true, if all objects are equal
     */
    public static boolean equals(Object...objects) {
        checkParameterCount(objects, 2);
        Object last = objects[0];
        for (Object o : objects) {
            if (o != null && !o.equals(last))
                return false;
            last = o;
        }
        return true;
    }
    
    /**
     * value
     * @param objects object to check
     * @param defaultValue
     * @return object, or if null the defaultValue
     */
    @SuppressWarnings("unchecked")
    public static <T> T value(Object object, T defaultValue) {
        return (T) (object != null ? object : defaultValue);
    }
    
    
    /**
     * standard toString implementation
     * @param cls root class
     * @param members class members
     * @return tostring representation
     */
    public static String toString(Class<?> cls, Object...members) {
        StringBuilder buf = new StringBuilder(cls.getSimpleName() + "(" + members[0]);
        for (int i = 1; i < members.length; i++) {
            buf.append(", " + members[i]);
        }
        return buf.append(")").toString();
    }
}
