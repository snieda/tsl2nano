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
public class Permutator {
    char min;
    char max;
    int start;
    int end;

    public Permutator(int length) {
        this('0', 'z', 0, length);
    }

    public Permutator(char min, char max, int length) {
        this(min, max, 0, length);
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
        super();
        this.min = min;
        this.max = max;
        this.start = start;
        this.end = end;
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

    public void permute(final PrintStream out) {
        Runnable runner = new Runnable() {
            @Override
            public void run() {
                permute(out, StringUtil.fixString(end - start, (char) min).toCharArray(), -1);
            }
        };
        ConcurrentUtil.startDaemon("permutator", runner);
    }

    /**
     * permute
     * 
     * @param out
     * @param src
     * @param position
     */
    public void permute(PrintStream out, char[] src, int position) {
        out.println(src);
        if (++position < src.length) {
            for (char c = min; c <= max; c++) {
                src[position] = c;
                permute(out, src, position);
            }
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

    /**
     * <pre>
     * Source: collection of strings
     * Transf: action to transform source-string
     * swap  : whether to swap keys and values
     * Dest  : property map with: source-string=transformation
     * Trial : action on each dest-key, checking if action-result equals key-value
     *       : distributing the work to many stations
     * 
     * </pre>
     */
    public static final void main(String[] args) {
        Argumentator ator = new Argumentator("permutator", getManual(), args);
        Object t;
        if (ator.check(System.out)) {
            String source = ator.consume("source", "");
            Collection src = CollectionUtil.load(source, null);
            String transformer = ator.consume("transformer", "");
            String mapname = source + ".transformed";
            boolean swap = ator.hasOption("swap");

            /*
             * create a transformation map. if map was saved before,
             * load it and check, if transformation was done.
             * if not, append the new transformation to the map
             */
            Properties p = new Properties();
            File fmap = new File(mapname);
            if (fmap.exists())
                try {
                    p.load(new FileReader(fmap));
                } catch (Exception e) {
                    ManagedException.forward(e);
                }
            if (!p.values().containsAll(src)) {
                transformer = transformer.replace("\"", "");
                String[] a = transformer.split("\\s");
                String[] a1 = new String[a.length - 2];
                System.arraycopy(a, 2, a1, 0, a1.length);
                for (Object o : src) {
                    BeanClass bc = BeanClass.createBeanClass(a[0]);
                    t = BeanClass.call(bc.getClazz(), a[1], false, a1);
                    if (swap)
                        p.put(t, o);
                    else
                        p.put(o, t);
                }
                FileUtil.saveProperties(mapname, p);
            }
            /*
             * use the transormation map on the 'trial' action. check the 'trial' result 
             * with the transformed result.
             */
            String backward = ator.consume("backward", "");
            if (!Util.isEmpty(backward)) {
                backward = backward.replace("\"", "");
                String[] a = backward.split("\\s");
                String[] a1 = CollectionUtil.copyOfRange(a, 2, a.length);
                Set<Object> keySet = p.keySet();
                BeanClass bc = BeanClass.createBeanClass(a[0]);
                String v;
                for (Object k : keySet) {
                    v = (String) p.get(k);
                    a1[0] = v;
                    t = BeanClass.call(bc.getClazz(), a[1], false, a1);
                    if (t != null && t.equals(k)) {
//                        System.setProperty("invader.result", k + ":" + v);
                        System.out.println(backward + " ==> " + k + ":" + v);
                        Finished.apply();
                    }
                }
            }
        }
    }
}
