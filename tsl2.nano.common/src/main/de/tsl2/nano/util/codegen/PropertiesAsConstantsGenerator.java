/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Oct 2, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.util.codegen;

/**
 * Generates an interface with integer constants (hashed keys) to be used to get resource values. saves ram-space using
 * integers instead of full string keys. provides type-safed access to property values.
 * <p/>
 * ON CONSTRUCTION:
 * 
 * <pre>
 * - (generate key/values to property file (from bean-attribute informations))
 * - load key/values from property file.
 * - map them into a Map[Integer, Object] where the integers are the hashes of the keys. 
 * - generate an interface for that property file with: 
 *   public static final Integer [key-as-constant-name]=[hash(key)];
 *   
 *   These constants can then be used to get the mapped values:
 *      myresourceBundle.getString([generated-interface].[key-name-as-constant-name]);
 * </pre>
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class PropertiesAsConstantsGenerator extends ClassGenerator {

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object getModel(String modelFile, ClassLoader classLoader) {
        // TODO Auto-generated method stub
        return super.getModel(modelFile, classLoader);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDefaultDestinationFile(String modelFile) {
        // TODO Auto-generated method stub
        return super.getDefaultDestinationFile(modelFile);
    }

    static final int encode(String propKey) {
//        return propKey.hashCode();
        char val[] = propKey.toCharArray();
        int h = 0;
        int len = val.length;
        for (int i = 0; i < len; i++) {
            h = 31 * h + val[i];
            System.out.println(val[i] + "(" + (int) val[i] + ") ==>" + h);
        }
        return h;
    }

    static final String decode(int h) {
        if (h == 0)
            return "";
        //encoding must be bijective to be decoded - not possible with standard hashcode
        char c[] = new char[h / 31];
        for (int i = c.length - 1; i >= 0; i--) {
            c[i] = (char) (h % 31);
            h -= c[i];
        }
//        for (int i = 0; i < len; i++) {
//            h = 31*h + val[off++];
//        }
        return String.valueOf(c);
    }

    public static final void main(String args[]) {
        String t = "abc";
        int h = encode(t);
        System.out.println(t + "-->" + h);
        System.out.println("decoded:" + decode(h));
    }
}
