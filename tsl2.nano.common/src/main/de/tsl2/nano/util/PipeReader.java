/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: May 22, 2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import org.apache.commons.logging.Log;

import de.tsl2.nano.exception.ManagedException;
import de.tsl2.nano.log.LogFactory;

/**
 * Piped Stream connector. Is able to connect one or two streams with a pipe. For further informations, see
 * {@link PipedInputStream} and {@link PipedOutputStream}. Use {@link #readInThreads(InputStream, OutputStream)} to let
 * it do the whole work.
 * <p/>
 * Their are two use cases: <br/>
 * The standard one will write to the given output-stream, reading input from a print-stream (see
 * {@link #getPrintStream()} - creating one new thread. You should connect the print-stream to the desired instance
 * (like System.out).<br/>
 * The more complicated use case will create two threads. one to read from given input and writing to the pipe, the
 * second reading the pipe and writing to the given output. usefull for en-decodings.
 * <p/>
 * WARNING: be sure, your output stream doesn't write again to the printstream - this would be done inside the same
 * thread with a deadlock.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class PipeReader implements Runnable {
    /** given input stream or null */
    InputStream in;
    /** given ouput stream */
    OutputStream out;
    /** internal input pipe */
    PipedInputStream pi;
    /** internal output pipe */
    PipedOutputStream po;
    /** printstream for output pipe */
    PrintStream ps;

    /** phase indicator to know, if the first thread is running */
    boolean firstStepConnected = false;

    private static final Log LOG = LogFactory.getLog(PipeReader.class);

    /**
     * constructor
     * 
     * @param in (optional) input stream (not yet an {@link PipedInputStream}!) to be connected to the output stream. if
     *            null, only a pipe from to the output-stream will be set.
     * @param po output stream (not yet an {@link PipedOutputStream}!) to be connected to the input stream.
     */
    public PipeReader(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
        firstStepConnected = in != null ? false : true;
        try {
            this.pi = new PipedInputStream();
            this.po = new PipedOutputStream((PipedInputStream) pi);
            this.ps = new PrintStream(po);
        } catch (IOException e) {
            ManagedException.forward(e);
        }
    }

    /**
     * getPrintStream
     * 
     * @return printstream, connected to the pipe. if you didn't give an input stream you should connect this
     *         printstream to an instance (e.g. System.out).
     */
    public PrintStream getPrintStream() {
        return ps;
    }

    protected PipedOutputStream getConnectionPipe() {
        return po;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        if (!firstStepConnected) {
            firstStepConnected = true;
            connect(in, po);
        } else
            connect(pi, out);
    }

    /**
     * does the work...(read from in, write to out)
     * 
     * @param in input stream
     * @param out output stream
     */
    private void connect(InputStream in, OutputStream out) {
        LOG.info("opening pipe: " + in + " --> " + out);
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        //the printstream may write 8192 bytes a time
        byte[] buffer = new byte[8192];
        int bytes_read;
        try {
            while (true) {
                bytes_read = in.read(buffer);
                if (bytes_read == -1) {
                    break;
                }
                out.write(buffer, 0, bytes_read);
                Thread.sleep(200);
            }
        } catch (Exception e) {
            ManagedException.forward(e);
        } finally {
            try {
                LOG.info("closing pipe: " + in + " --> " + out);
                po.close();
                pi.close();
            } catch (IOException e) {
                ManagedException.forward(e);
            }
        }
    }

    /**
     * creates a PipeReader starting one (if in is null) or two threads to connect the streams through a pipe.
     * 
     * @param in (optional) input stream (not yet an {@link PipedInputStream}!) to be connected to the output stream. if
     *            null, only a pipe from to the output-stream will be set.
     * @param po output stream (not yet an {@link PipedOutputStream}!) to be connected to the input stream.
     * @return a printstream that may be set to an output stream (e.g. System.out or/and System.err). makes sense if you
     *         don't give any input stream.
     */
    public static PrintStream readInThreads(InputStream in, OutputStream out) {
        PipeReader pipeReader = new PipeReader(in, out);
        if (!pipeReader.firstStepConnected)
            new Thread(pipeReader).start();
        new Thread(pipeReader).start();
        return pipeReader.getPrintStream();
    }
}
