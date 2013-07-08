/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Erwin Guib, Thomas Schneider
 * created on: Oct 25, 2009
 * 
 * Copyright: (c) Thomas Schneider, all rights reserved
 */
package de.idv.resource.fs.impl;

import java.io.Serializable;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.Referenceable;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnectionFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.idv.resource.fs.FsConnection;
import de.idv.resource.fs.FsConnectionFactory;

/**
 * Implementation for {@link FsConnectionFactory}.
 * 
 * @author Erwin Guib, Thomas Schneider
 * @version $Revision$
 */
public class FsConnectionFactoryImpl implements FsConnectionFactory, Referenceable, Serializable {
    private static final long serialVersionUID = 6717225855889317280L;

    private static final Log LOG = LogFactory.getLog(FsConnectionFactoryImpl.class);

    private Reference reference;
    private ConnectionManager manager;
    private ManagedConnectionFactory factory;
    private ConnectionRequestInfo info;

    /**
     * Constructor.
     * 
     * @param manager the connection manager (from App Server)
     * @param factory the factory
     * @param info the info
     */
    public FsConnectionFactoryImpl(ConnectionManager manager,
            ManagedConnectionFactory factory,
            ConnectionRequestInfo info) {
        LOG.info("new manager=" + manager + " fact=" + factory + " info=" + info);
        this.manager = manager;
        this.factory = factory;
        this.info = info;
    }

    /**
     * {@inheritDoc}
     */
    public FsConnection getConnection() throws ResourceException {
        return (FsConnection) manager.allocateConnection(factory, info);
    }

    /**
     * {@inheritDoc}
     */
    public void setReference(Reference reference) {
        this.reference = reference;
    }

    /**
     * {@inheritDoc}
     */
    public Reference getReference() throws NamingException {
        return reference;
    }
}
