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

import javax.resource.spi.ConnectionRequestInfo;

/**
 * A connection request info for the FS connection.
 * 
 * @author Erwin Guib, Thomas Schneider
 * @version $Revision$
 */
public class FsConnectionRequestInfo implements ConnectionRequestInfo, Serializable {
    private static final long serialVersionUID = -1804328002625818694L;

    private String rootDirPath;

    /**
     * Default constructor.
     */
    public FsConnectionRequestInfo() {
    }

    /**
     * Constructor with rootDirPath.
     * 
     * @param rootDirPath the path
     */
    public FsConnectionRequestInfo(String rootDirPath) {
        setRootDirPath(rootDirPath);
    }

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
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((rootDirPath == null) ? 0 : rootDirPath.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FsConnectionRequestInfo other = (FsConnectionRequestInfo) obj;
        if (rootDirPath == null) {
            if (other.rootDirPath != null) {
                return false;
            }
        } else if (!rootDirPath.equals(other.rootDirPath)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(getClass().getSimpleName());
        str.append("[rootDirPath=").append(rootDirPath);
        str.append("]");
        return str.toString();
    }
}
