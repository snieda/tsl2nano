/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 22.12.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.core.util;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.core.Argumentator;
import de.tsl2.nano.core.Finished;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;

/**
 * permutes the given array in the given range and prints to the given print stream. usable as brute force algorithm.
 * 
 * @author Tom
 * @version $Revision$
 */
public class Permutator extends APermutator<Character, Integer> {
    public Permutator(int length) {
        this('0', 'z', 0, length);
    }

    public Permutator(char min, char max, int length) {
        super(min, max, 0, length);
    }

    /**
     * constructor
     * 
     * @param src
     * @param min
     * @param max
     * @param index
     */
    public Permutator(char min, char max, int start, int end) {
        super(min, max, start, end);
    }

    public InputStream permute() {
        PipedInputStream in = new PipedInputStream();
        try {
            PrintStream stream = ByteUtil.getPipe(in);
            permute(stream);
            //wait, to have some bytes in the queue before returning
            Thread.sleep(200);
            return in;
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    private static Map<String, String> getManual() {
        HashMap<String, String> man = new LinkedHashMap<String, String>();
        man.put("source", "(!) source collection");
        man.put("transformer", "(!) transforming action");
        man.put("swap", "(default:false) whether to swap key and values in destination-map");
        man.put("backward", "(!) action to do a back-transformation for each keys value");
        return man;
    }

    @Override
    protected Character[] getSource() {
        char[] chararr = StringUtil.fixString(toNumber(end) - toNumber(start), min.toString().charAt(0)).toCharArray();
        Character[] cs = new Character[chararr.length];
        System.arraycopy(chararr, 0, cs, 0, chararr.length);
        return cs;
    }

    @Override
    protected Character increase(Character c) {
        return ++c;
    }
}
