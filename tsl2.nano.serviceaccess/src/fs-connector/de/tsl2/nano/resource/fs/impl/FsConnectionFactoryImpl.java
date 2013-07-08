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

import de.tsl2.nano.resource.fs.FsConnection;
import de.tsl2.nano.resource.fs.FsConnectionFactory;

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
    private final ConnectionManager manager;
    private final ManagedConnectionFactory factory;
    private final ConnectionRequestInfo info;

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
    @Override
    public FsConnection getConnection() throws ResourceException {
        return (FsConnection) manager.allocateConnection(factory, info);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReference(Reference reference) {
        this.reference = reference;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Reference getReference() throws NamingException {
        return reference;
    }
}
