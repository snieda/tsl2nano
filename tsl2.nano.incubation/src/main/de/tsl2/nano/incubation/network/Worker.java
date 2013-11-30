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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.apache.commons.logging.Log;

import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.log.LogFactory;

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
            JobContext<?> job = (JobContext<?>) stream.readObject();
            LOG.info("doing job " + job);
            if (job.getClassLoader() != null) {
                LOG.info("initializing transfered classloader: " + job.getClassLoader());
                Thread.currentThread().setContextClassLoader(job.getClassLoader());
            }
            FutureTask task = new FutureTask(job.getCallable());
            executor.execute(task);
            while (!task.isCancelled() && !task.isDone()) {
                //get the request from client
                stream = new ObjectInputStream(socket.getInputStream());
                Request request = (Request) stream.readObject();
                request.createResponse(task);
                //send it back to the client
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.writeObject(request);
            }
            LOG.info("finished job " + job);
            socket.close();
        } catch (Exception e) {
            ForwardedException.forward(e);
        }
    }
}
