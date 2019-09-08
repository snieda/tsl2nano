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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Worker {
    private static final Log LOG = LogFactory.getLog(Worker.class);
    ExecutorService executor;

    public Worker(ExecutorService executor, Socket accept) {
        this.executor = executor;
        run(accept);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void run(Socket socket) {
        try {
//            URL remoteURL = new URL("http:" + socket.getInetAddress() + ":" + socket.getPort());
//            //connect the current thread to the remote classloader
//            URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{remoteURL});
//            Thread.currentThread().setContextClassLoader(urlClassLoader);

            //run the transferred job
            ObjectInputStream stream = new ObjectInputStream(socket.getInputStream());
            LOG.info("reading socket input stream: " + stream.available() + " bytes");
            JobContext<?> job = (JobContext<?>) stream.readObject();
            LOG.info("doing job " + job);
            if (job.getClassLoader() != null) {
                LOG.info("initializing transfered classloader: " + job.getClassLoader());
                Thread.currentThread().setContextClassLoader(job.getClassLoader());
            }
            FutureTask task = new FutureTask(job.getCallable());
            executor.execute(task);
            response(socket, task);
        } catch (Exception e) {
            ManagedException.forward(e);
        }
    }

    /**
     * blocks until task is done and requester closes the connection
     * @param socket connection
     * @param task executed task
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void response(Socket socket, FutureTask task) throws IOException, ClassNotFoundException {
        ObjectInputStream stream;
        while (socket.isConnected() && !socket.isClosed()) {
            /*
             * get the request from client. if socket was closed from remote, an EOFEception will be thrown
             */
            stream = new ObjectInputStream(socket.getInputStream());
            Request request = (Request) stream.readObject();
            request.setProgress(Request.PROGRESS_ACCEPTED);
            request.createResponse(task);
            //send it back to the client
            LOG.debug("sending response " + request + " for job " + task);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(request);
        }
    }
}
