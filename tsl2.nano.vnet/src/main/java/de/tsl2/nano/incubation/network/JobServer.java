/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 28.11.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.incubation.network;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.net.ServerSocketFactory;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;

/**
 * Provides a mechanism to distribute jobs to other hosts - without manual installation of new classes and jars.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class JobServer implements Runnable, Closeable {
    /** local thread pool */
    private transient ExecutorService localExecutorService = Executors.newCachedThreadPool();
    /** ip adresses of registered workstations */
    private Collection<String> availableWorkstations;
    /**
     * current connections. key: ip adress of a connected workstation (see {@link #availableWorkstations}. response:
     * current connection.
     */
    transient private Map<String, Socket> connections;
    transient private Map<String, Collection<Class<?>>> loadedCommands;

    /** port, used for all connections */
    private int port;

    private static final Log LOG = LogFactory.getLog(JobServer.class);

    /**
     * constructor
     */
    public JobServer() {
        this(Executors.newCachedThreadPool(), Arrays.asList("127.0.0.1"), 9876, true);
    }

    /**
     * constructor
     */
    public JobServer(ExecutorService executorService,
            Collection<String> availableWorkstations,
            int port,
            boolean isWorker) {
        super();
        this.localExecutorService = executorService;
        this.availableWorkstations = availableWorkstations;
        this.connections = new HashMap<String, Socket>();
        loadedCommands = new HashMap<String, Collection<Class<?>>>();
        this.port = port;
        if (isWorker) {
            localExecutorService.execute(this);
        }
    }

    private Socket newConnection() {
        if (connections.size() == availableWorkstations.size()) {
            return null;
        }
        Set<String> ips = connections.keySet();
        for (String ip : availableWorkstations) {
            if (!ips.contains(ip)) {
                try {
                    Socket socket = new Socket(ip, port);
                    connections.put(ip, socket);
                    return socket;
                } catch (Exception e) {
                    ManagedException.forward(e);
                }
            }
        }
        return null;
    }

    private Socket getConnection() {
        Socket socket = newConnection();
        return socket != null ? socket : getMostAvailableConnection();
    }

    private Socket getMostAvailableConnection() {
        Collection<Socket> sockets = connections.values();
        for (Socket socket : sockets) {
            //TODO: how to check availability?
            if (!socket.isConnected()) {
                return socket;
            }
        }
        throw new IllegalStateException("all registered workstations are in use! no more connections available");
    }

    private boolean isLoaded(String ip, Class<?> command) {
        return loadedCommands.containsKey(ip) && loadedCommands.get(ip).contains(command);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <CONTEXT extends Serializable> Future<CONTEXT> execute(String name, Callable<CONTEXT> command) {
        Socket connection = getConnection();
        LOG.info("distributing new job '" + command + "' on target " + connection);

        ObjectOutputStream o;
        ClassLoader cl = null;
        try {
            //don't send class (use urlclassloader on the other side) but send object to the remote host
            //TODO: perhaps, send current classloader? the urlclassloader would have to know, which jars and pathes to load!
            if (!isLoaded(connection.getInetAddress().getHostAddress(), command.getClass())) {
                //TODO: don't use classloader but provide jars/directories in url-parameter of JobContext!
                cl = new SerializableClassLoader(command.getClass().getClassLoader());
            }
            o = new ObjectOutputStream(connection.getOutputStream());
            o.writeObject(new JobContext<CONTEXT>(name, command, cl, null));
            return new Work(name, connection);
        } catch (Exception e) {
            ManagedException.forward(e);
            FutureTask<CONTEXT> futureTask = new FutureTask<CONTEXT>(command);
            return futureTask;
        }
    }

    /**
     * waits for the given execution to be done.
     * @param name job name
     * @param command execution logic
     * @param timeout max time to wait
     * @return result of given job
     */
    public <CONTEXT extends Serializable> CONTEXT executeWait(String name, Callable<CONTEXT> command, long timeout) {
        try {
            return execute(name, command).get(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * waits for remote requests to do some work.
     */
    @Override
    public void run() {
        ServerSocket serverSocket = null;
        try {
            //create a socket, waiting for a connection to get a working job
            serverSocket = ServerSocketFactory.getDefault().createServerSocket(port);
            while (port > 0) {
                new Worker(localExecutorService, serverSocket.accept());
            }
        } catch (Exception e) {
            ManagedException.forward(e);
        } finally {
            if (serverSocket != null)
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    @Override
    public void close() throws IOException {
        port = 0;
        Collection<Socket> c = connections.values();
        for (Socket socket : c) {
            socket.close();
        }
    }
}
class SerializableClassLoader extends ClassLoader implements Serializable {
    private static final long serialVersionUID = -2466479262962610559L;

    public SerializableClassLoader() {
        super();
    }

    public SerializableClassLoader(ClassLoader parent) {
        super(parent);
    }
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        //TODO: howto read ClassLoader.classes: out.writeObject(classes);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        //TODO: howto store ClassLoader.classes: out.writeObject(classes);
    }
}