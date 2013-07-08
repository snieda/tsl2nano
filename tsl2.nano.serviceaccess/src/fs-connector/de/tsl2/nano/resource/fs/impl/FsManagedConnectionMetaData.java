/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Erwin Guib, Thomas Schneider
 * created on: Nov 1, 2009
 * 
 * Copyright: (c) Thomas Schneider, all rights reserved
 */
package de.tsl2.nano.resource.fs.impl;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;

/**
 * Managed connection metadata.
 * 
 * @author Erwin Guib, Thomas Schneider
 * @version $Revision$
 */
public class FsManagedConnectionMetaData implements ManagedConnectionMetaData {

    private final Subject subject;

    /**
     * Constructor.
     * 
     * @param subject the managed connection subject
     */
    public FsManagedConnectionMetaData(Subject subject) {
        this.subject = subject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEISProductName() throws ResourceException {
        return "File System Connector";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEISProductVersion() throws ResourceException {
        return "1.0.0";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxConnections() throws ResourceException {
        return 100;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserName() throws ResourceException {
        if (subject == null) {
            return "anonymous";
        }
        return subject.toString();
    }

}
