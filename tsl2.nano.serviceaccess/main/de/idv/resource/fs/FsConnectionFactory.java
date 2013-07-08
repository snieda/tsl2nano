/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Erwin Guib, Thomas Schneider
 * created on: Oct 24, 2009
 * 
 * Copyright: (c) Thomas Schneider, all rights reserved
 */
package de.idv.resource.fs;

import javax.resource.ResourceException;

/**
 * The connection factory for a file system connection.
 * 
 * @author Erwin Guib, Thomas Schneider
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public interface FsConnectionFactory {

	/**
	 * constant to be used as rootDir, if no rootDir should be used, but the
	 * absolute file path of a given filename on
	 * {@link FsConnection#getInputStream(String)} and
	 * {@link FsConnection#writeFile(String, java.io.InputStream, boolean)}.
	 * <p>
	 * the rootDirPath is configured and deployed in an fsconnection xml file.
	 */
	public static final String MODE_ABSOLUTE_PATH = "MODE_ABSOLUTE_PATH";

	/**
	 * Get a connection.
	 * 
	 * @return the connection.
	 * @throws ResourceException
	 *             if failed
	 */
	FsConnection getConnection() throws ResourceException;

}