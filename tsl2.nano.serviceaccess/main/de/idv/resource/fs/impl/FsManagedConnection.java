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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.idv.resource.fs.FsConnection;
import de.idv.resource.fs.FsConnectionFactory;

/**
 * The managed connection (connector internal) behind a FsConnection.
 * 
 * @author Erwin Guib, Thomas Schneider
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class FsManagedConnection implements ManagedConnection, FsConnection {
	private static final Log LOG = LogFactory.getLog(FsManagedConnection.class);

	private ArrayList<ConnectionEventListener> listeners = new ArrayList<ConnectionEventListener>();
	private Subject subject;
	private FsConnectionRequestInfo info;

	private List<FsConnectionImpl> connections = new ArrayList<FsConnectionImpl>();

	private File rootDir;

	/**
	 * Constructor.
	 * 
	 * @param subject
	 *            the subject (from App-Server)
	 * @param info
	 *            the connection info
	 */
	public FsManagedConnection(Subject subject, FsConnectionRequestInfo info) {
		LOG.info("new subject=" + subject + " info=" + info);
		this.subject = subject;
		this.info = info;
		if (!isUseAbsoluteFilePath()) {
			rootDir = new File(info.getRootDirPath());
			LOG.info("absolute rootDirPath: " + rootDir.getAbsolutePath());
		}
	}

	/**
	 * Check if this connection is matching for the given parameters (used by
	 * {@link FsManagedConnectionFactory#matchManagedConnections(java.util.Set, Subject, ConnectionRequestInfo)}
	 * 
	 * @param subject
	 *            a subject
	 * @param info
	 *            a info
	 * @return true when matching
	 */
	public boolean isMatch(Subject subject, ConnectionRequestInfo info) {
		return this.info.equals(info);
	}

	/**
	 * {@inheritDoc}
	 */
	public Object getConnection(Subject subject, ConnectionRequestInfo info)
			throws ResourceException {
		if (!isMatch(subject, info)) {
			LOG.error("getConnection called with wrong info=" + info
					+ " internal info=" + this.info);
			throw new ResourceException("Info missmatch: parameter 'info'="
					+ info + " 'internal info'=" + this.info);
		}
		FsConnectionImpl con = new FsConnectionImpl();
		associateConnection(con);
		return con;
	}

	/**
	 * {@inheritDoc}
	 */
	public void associateConnection(Object connection) throws ResourceException {
		LOG.debug("associateConnection con=" + connection);
		FsConnectionImpl fsCon = (FsConnectionImpl) connection;
		connections.add(fsCon);
		fsCon.setManagedConnection(this);
	}

	/**
	 * @param con
	 */
	protected void detachConnection(FsConnectionImpl con) {
		LOG.debug("detachConnection con=" + con);
		con.setManagedConnection(null);
		connections.remove(con);
		ConnectionEvent event = new ConnectionEvent(this,
				ConnectionEvent.CONNECTION_CLOSED);
		event.setConnectionHandle(con);
		fireConnectionEvent(event);
	}

	/**
	 * {@inheritDoc}
	 */
	public LocalTransaction getLocalTransaction() throws ResourceException {
		throw new NotSupportedException();
	}

	/**
	 * {@inheritDoc}
	 */
	public XAResource getXAResource() throws ResourceException {
		throw new NotSupportedException();
	}

	/**
	 * {@inheritDoc}
	 */
	public ManagedConnectionMetaData getMetaData() throws ResourceException {
		return new FsManagedConnectionMetaData(subject);
	}

	/**
	 * {@inheritDoc}
	 */
	public void cleanup() throws ResourceException {
		LOG.info("cleanup");
		// Attention: detachConnection will remove the connection from list =>
		// cannot use iterator
		while (connections.size() > 0) {
			FsConnectionImpl con = connections.get(0);
			detachConnection(con);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void destroy() throws ResourceException {
		// nothing to do
		LOG.info("destroy");
	}

	/**
	 * {@inheritDoc}
	 */
	public void addConnectionEventListener(ConnectionEventListener listener) {
		listeners.add(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeConnectionEventListener(ConnectionEventListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Fire an event.
	 * 
	 * @param event
	 *            the event
	 */
	protected void fireConnectionEvent(ConnectionEvent event) {
		for (ConnectionEventListener l : listeners) {
			switch (event.getId()) {
			case ConnectionEvent.CONNECTION_CLOSED:
				l.connectionClosed(event);
				break;
			case ConnectionEvent.CONNECTION_ERROR_OCCURRED:
				l.connectionErrorOccurred(event);
				break;
			case ConnectionEvent.LOCAL_TRANSACTION_COMMITTED:
				l.localTransactionCommitted(event);
				break;
			case ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK:
				l.localTransactionRolledback(event);
				break;
			case ConnectionEvent.LOCAL_TRANSACTION_STARTED:
				l.localTransactionStarted(event);
				break;
			default:
				LOG.error("unknown event id=" + event.getId());
			}
		}
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
	 * {@inheritDoc}
	 */
	public void close() {
		// nothing to do
		throw new IllegalStateException("this method must never be called");
	}

	/**
	 * {@inheritDoc}
	 */
	public void delete(String fileName) throws IOException {
		File f = getFile(fileName);
		if (f.isDirectory()) {
			try {
				FileUtils.deleteDirectory(f);
			} catch (IOException e) {
				LOG.error("Error deleting directory: " + fileName, e);
				throw e;
			}
		} else {
			f.delete();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeFile(String destFileName, InputStream data,
			boolean overwrite) throws IOException {
		File f = getFile(destFileName);
		FileOutputStream out = new FileOutputStream(f);
		if (!overwrite && f.exists()) {
			String msg = "Error writing file (already exists):" + destFileName;
			LOG.error(msg);
			throw new IOException(msg);
		}
		File parentDir = f.getParentFile();
		if (!parentDir.exists() && !parentDir.mkdirs()) {
			String msg = "failed to create parent dir=" + parentDir;
			LOG.error(msg);
			throw new IOException(msg);
		}
		try {
			IOUtils.copy(data, out);
		} catch (IOException e) {
			LOG.error("Error writing file:" + destFileName, e);
			throw e;
		} finally {
			IOUtils.closeQuietly(data);
			IOUtils.closeQuietly(out);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public InputStream getInputStream(String fileName) throws IOException {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(getFile(fileName));
		} catch (FileNotFoundException e) {
			LOG.error("file not found f=" + fileName, e);
			throw e;
		}
		BufferedInputStream bis = new BufferedInputStream(fis);
		return bis;
	}

	/**
	 * @param fileName
	 * @return absolute file
	 */
	protected File getFile(String fileName) {
		File f = isUseAbsoluteFilePath() ? new File(fileName) : new File(rootDir,
				fileName);
		LOG.debug("accessing file: " + f.getAbsolutePath());
		return f;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean exists(String fileName) {
		File f = getFile(fileName);
		return f.exists();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isDirectory(String fileName) {
		File f = getFile(fileName);
		return f.isDirectory();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isFile(String fileName) {
		File f = getFile(fileName);
		return f.isFile();
	}

	/**
	 * if rootDir is @link{FsConnectionFactory#MODE_ABSOLUTE_PATH}, the given
	 * full file path will be used - rootDir will be ignored.
	 */
	public final boolean isUseAbsoluteFilePath() {
		return isUseAbsoluteFilePath(info.getRootDirPath());
	}

	/**
	 * if rootDir is @link{FsConnectionFactory#MODE_ABSOLUTE_PATH}, the given
	 * full file path will be used - rootDir will be ignored.
	 */
	public static final boolean isUseAbsoluteFilePath(String rootDirPath) {
		return rootDirPath.equals(FsConnectionFactory.MODE_ABSOLUTE_PATH);
	}

	/**
	 * @see de.idv.resource.fs.FsConnection#getDirectoryEntries(java.lang.String)
	 */
	public String[] getDirectoryEntries(String dirName) throws IOException {
		if (!exists(dirName))
			throw new IllegalArgumentException(dirName + " does not exist!");
		if (!isDirectory(dirName))
			throw new IllegalArgumentException(dirName + " must be of type directory - but is a file!");
		return getFile(dirName).list();
	}

}
