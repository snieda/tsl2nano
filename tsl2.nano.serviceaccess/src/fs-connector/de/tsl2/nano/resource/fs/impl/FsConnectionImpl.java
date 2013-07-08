/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Erwin Guib, Thomas Schneider
 * created on: Oct 25, 2009
 * 
 * Copyright: (c) Thomas Schneider, all rights reserved
 */
package de.tsl2.nano.resource.fs.impl;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.resource.fs.FsConnection;

/**
 * The implementation for a {@link FsConnection}. A simple delegator to {@link FsManagedConnection}.
 * 
 * @author Erwin Guib, Thomas Schneider
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class FsConnectionImpl implements FsConnection {
    private static final Log LOG = LogFactory.getLog(FsConnectionImpl.class);

    private FsManagedConnection managedCon;

    /**
     * Set the managed connection.
     * 
     * @param managedCon the managed connection behind this connection
     */
    public void setManagedConnection(FsManagedConnection managedCon) {
        this.managedCon = managedCon;
    }

    /**
     * Check if the connection is still alive (not closed).
     * 
     * @throws IOException if closed
     */
    protected void checkManagedConnection() throws IOException {
        if (managedCon == null) {
            LOG.error("call for closed connection from", new Exception());
            throw new IOException("managed connection has been detached");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        try {
            checkManagedConnection();
            managedCon.detachConnection(this);
        } catch (IOException e) {
            ForwardedException.forward(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String fileName) throws IOException {
        checkManagedConnection();
        managedCon.delete(fileName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(String fileName) throws IOException {
        checkManagedConnection();
        return managedCon.exists(fileName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getInputStream(String fileName) throws IOException {
        checkManagedConnection();
        return managedCon.getInputStream(fileName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory(String fileName) throws IOException {
        checkManagedConnection();
        return managedCon.isDirectory(fileName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFile(String fileName) throws IOException {
        checkManagedConnection();
        return managedCon.isFile(fileName);
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void rename(String sourceName, String destinationName) throws IOException {
        checkManagedConnection();
        managedCon.rename(sourceName, destinationName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeFile(String destFileName, InputStream data, boolean overwrite) throws IOException {
        checkManagedConnection();
        managedCon.writeFile(destFileName, data, overwrite);
    }

    /**
     * @see de.idv.resource.fs.FsConnection#getDirectoryEntries(java.lang.String)
     */
    @Override
    public String[] getDirectoryEntries(String dirName) throws IOException {
        checkManagedConnection();
        return managedCon.getDirectoryEntries(dirName);
    }
    
    @Override
    public boolean isOpen() {
        return managedCon != null;
    }
}
