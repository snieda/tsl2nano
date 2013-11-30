/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 29.11.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.incubation.network;

import static de.tsl2.nano.incubation.network.Request.CANCEL;
import static de.tsl2.nano.incubation.network.Request.CANCELED;
import static de.tsl2.nano.incubation.network.Request.DONE;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;

import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.log.LogFactory;

/**
 * Is able to evaluate the current state of remote Job/Work executed through a {@link Worker}.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Work<CONTEXT> implements Future<CONTEXT> {
    private static final Log LOG = LogFactory.getLog(Work.class);
    Socket socket;
    CONTEXT result;
    boolean done;

    /**
     * constructor
     * 
     * @param socket
     */
    public Work(Socket socket) {
        super();
        this.socket = socket;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return connected() ? (Boolean) send(CANCEL).getResponse() : !done;
    }

    @Override
    public boolean isCancelled() {
        return connected() ? (Boolean) send(CANCELED).getResponse() : !done;
    }

    @Override
    public boolean isDone() {
        return connected() ? (Boolean) send(DONE).getResponse() : done;
    }

    private boolean connected() {
        return socket.getChannel() != null && socket.getChannel().isOpen();
    }

    @SuppressWarnings("unchecked")
    @Override
    public CONTEXT get() throws InterruptedException, ExecutionException {
        return connected() ? result = (CONTEXT) send(Request.RESULT).getResponse() : result;
    }

    /**
     * sends a request to the remote worker. the remote worker will answer this request, filling its state through
     * {@link Request#createResponse(Future)}.
     * 
     * @param request state to check
     * @return response of remote {@link Worker}.
     */
    private Request send(byte request) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(new Request(request));

            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            return (Request) in.readObject();
        } catch (Exception e) {
            ForwardedException.forward(e);
        }
        return null;
    }

    @Override
    public CONTEXT get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long startTime = System.currentTimeMillis();
        long maxTime = unit.toMillis(timeout);
        try {
            while (System.currentTimeMillis() - startTime < maxTime) {
                if (isDone()) {
                    CONTEXT result = get();
                    LOG.info("task returning context: " + result);
                    return result;
                }
                Thread.sleep(200);
            }
        } catch (Exception e) {
            ForwardedException.forward(e);
        }
        //unreachable
        return null;
    }

}