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

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import de.tsl2.nano.core.ManagedException;

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
}
