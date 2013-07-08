/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Erwin Guib, Thomas Schneider
 * created on: Nov 1, 2009
 * 
 * Copyright: (c) Thomas Schneider, all rights reserved
 */
package de.idv.resource.fs.impl;

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

    private Subject subject;

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
    public String getEISProductName() throws ResourceException {
        return "File System Connector";
    }

    /**
     * {@inheritDoc}
     */
    public String getEISProductVersion() throws ResourceException {
        return "1.0.0";
    }

    /**
     * {@inheritDoc}
     */
    public int getMaxConnections() throws ResourceException {
        return 100;
    }

    /**
     * {@inheritDoc}
     */
    public String getUserName() throws ResourceException {
        if (subject == null) {
            return "anonymous";
        }
        return subject.toString();
    }

}
