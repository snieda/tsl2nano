/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: EGU, Thomas Schneider
 * created on: Oct 26, 2009
 * 
 * Copyright: (c) Thomas Schneider, all rights reserved
 */
package de.idv.resource.fs.impl;

import java.io.Serializable;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

/**
 * {@link ResourceAdapter} implementation for FS connector. Currently only used as "rootDirPath" Property container.
 * 
 * @author EGU, Thomas Schneider
 * @version $Revision$
 */
public class FsResourceAdapter implements ResourceAdapter, Serializable {
    private static final long serialVersionUID = 8295263396733460640L;

    private String rootDirPath;

    /**
     * Set the rootDirPath.
     * 
     * @param rootDirPath the path
     */
    public void setRootDirPath(String rootDirPath) {
        this.rootDirPath = rootDirPath;
    }

    /**
     * Get the rootDirPath.
     * 
     * @return the path
     */
    public String getRootDirPath() {
        return rootDirPath;
    }

    /**
     * {@inheritDoc}
     */
    public void endpointActivation(MessageEndpointFactory arg0, ActivationSpec arg1) throws ResourceException {
        throw new NotSupportedException("no endpoint activation support");
    }

    /**
     * {@inheritDoc}
     */
    public void endpointDeactivation(MessageEndpointFactory arg0, ActivationSpec arg1) {
    }

    /**
     * {@inheritDoc}
     */
    public XAResource[] getXAResources(ActivationSpec[] arg0) throws ResourceException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void start(BootstrapContext arg0) throws ResourceAdapterInternalException {
    }

    /**
     * {@inheritDoc}
     */
    public void stop() {
    }

}
