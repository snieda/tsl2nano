/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Erwin Guib, Thomas Schneider
 * created on: Oct 24, 2009
 * 
 * Copyright: (c) Thomas Schneider, all rights reserved
 */
package de.idv.resource.fs.impl;

import java.io.File;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.security.auth.Subject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.idv.resource.fs.FsConnectionFactory;

/**
 * The manages connection factory implementation.
 * 
 * @author Erwin Guib, Thomas Schneider
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class FsManagedConnectionFactory implements ManagedConnectionFactory,
		ResourceAdapterAssociation {
	private static final long serialVersionUID = -7740098500317224157L;

	private static final Log LOG = LogFactory
			.getLog(FsManagedConnectionFactory.class);

	private FsResourceAdapter resourceAdapter;
	private String rootDirPath = "undefinedRootDirPath";

	/**
	 * {@inheritDoc}
	 */
	public void setResourceAdapter(ResourceAdapter resourceAdapter)
			throws ResourceException {
		this.resourceAdapter = (FsResourceAdapter) resourceAdapter;
	}

	/**
	 * {@inheritDoc}
	 */
	public ResourceAdapter getResourceAdapter() {
		return resourceAdapter;
	}

	/**
	 * {@inheritDoc}
	 */
	public Object createConnectionFactory() throws ResourceException {
		throw new UnsupportedOperationException("not for unmanaged environment");
	}

	/**
	 * {@inheritDoc}
	 */
	public Object createConnectionFactory(ConnectionManager cm)
			throws ResourceException {
		FsConnectionRequestInfo info = new FsConnectionRequestInfo(
				getRootDirPath());
		checkRootDirPath();
		return new FsConnectionFactoryImpl(cm, this, info);
	}

	/**
	 * {@inheritDoc}
	 */
	public ManagedConnection createManagedConnection(Subject subject,
			ConnectionRequestInfo info) throws ResourceException {
		if (info == null) {
			LOG.warn("createManagedConnection without info, use default");
			info = new FsConnectionRequestInfo(getRootDirPath());
		}
		FsManagedConnection managedCon = new FsManagedConnection(subject,
				(FsConnectionRequestInfo) info);
		return managedCon;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public ManagedConnection matchManagedConnections(Set connectionSet,
			Subject subject, ConnectionRequestInfo info)
			throws ResourceException {
		LOG.debug("matchManagedConnections");
		for (Iterator<ManagedConnection> iter = connectionSet.iterator(); iter
				.hasNext();) {
			ManagedConnection managedCon = iter.next();
			if (managedCon instanceof FsManagedConnection) {
				if (((FsManagedConnection) managedCon).isMatch(subject, info)) {
					LOG.debug("matched con=" + managedCon);
					return managedCon;
				}
			}
		}
		LOG.debug("no match, return null");
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public PrintWriter getLogWriter() throws ResourceException {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setLogWriter(PrintWriter writer) throws ResourceException {
	}

	/**
	 * Check if the rootDirPath exists or can be created.
	 * 
	 * @throws ResourceException
	 *             if the rootDirPath does not exist and cannot be created
	 * 
	 */
	protected void checkRootDirPath() throws ResourceException {
		if (isUseAbsoluteFilePath()) {
			LOG.info("checkRootDirPath: "
					+ FsConnectionFactory.MODE_ABSOLUTE_PATH
					+ " ==> using absolute filepath - ignoring rootDir");
		} else {
			LOG.info("checkRootDirPath rootDirPath=" + getRootDirPath());
			File rootDir = new File(getRootDirPath());

			if (rootDir.exists() == false) {
				if (!rootDir.mkdirs()) {
					LOG.error("checkRootDirPath failed, could not create directory");
					throw new ResourceException("failed to create rootDir="
							+ getRootDirPath());
				}
			}
		}
	}

	/**
	 * Set the rootDirPath.
	 * 
	 * @param rootDirPath
	 *            the path
	 */
	public void setRootDirPath(String rootDirPath) {
		if (!isUseAbsoluteFilePath()) {
			LOG.info("setRootDirPath=" + rootDirPath);
			this.rootDirPath = rootDirPath;
			File rPath = new File(rootDirPath);
			// check and log it...
			LOG.info("absolute rootDirPath is now: " + rPath.getAbsolutePath());
			if (!rPath.exists() || !rPath.isDirectory())
				LOG.warn("rootDirPath " + rPath.getAbsolutePath()
						+ " does not exist or is no valid directory");
		}
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
	 * if rootDir is @link{FsConnectionFactory#MODE_ABSOLUTE_PATH}, the given
	 * full file path will be used - rootDir will be ignored.
	 */
	public final boolean isUseAbsoluteFilePath() {
		return FsManagedConnection.isUseAbsoluteFilePath(rootDirPath);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((rootDirPath == null) ? 0 : rootDirPath.hashCode());
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
		FsManagedConnectionFactory other = (FsManagedConnectionFactory) obj;
		if (rootDirPath == null) {
			if (other.rootDirPath != null) {
				return false;
			}
		} else if (!rootDirPath.equals(other.rootDirPath)) {
			return false;
		}
		return true;
	}
}
