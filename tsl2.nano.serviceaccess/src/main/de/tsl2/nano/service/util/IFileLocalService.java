/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Mar 18, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.service.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

import javax.ejb.Local;

import de.tsl2.nano.resource.fs.FsConnection;
import de.tsl2.nano.resource.fs.FsConnectionFactory;

//import de.tsl2.nano.resource.fs.FsConnection;
//import de.tsl2.nano.resource.fs.FsConnectionFactory;

/**
 * Local Services for server side file access through filesystem-connector.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
@Local
public interface IFileLocalService {
    /**
     * uses the fsConnector to read the file input stream. connection will not be closed! please call
     * {@link #closeConnection()} before leaving your method
     * 
     * @see FsConnection#getInputStream(String)
     * @see FsConnectionFactory#MODE_ABSOLUTE_PATH
     * 
     * 
     * @param fileName file name
     * @return reader
     */
    BufferedReader getFileReader(String fileName);

    /**
     * renames an existing file
     * 
     * @see FsConnection#rename(String, String)
     * @see FsConnectionFactory#MODE_ABSOLUTE_PATH
     * 
     * @param sourceName file to rename
     * @param destinationName new file name
     */
    void rename(String sourceName, String destinationName) throws IOException;

    /**
     * Write a file. connection will be closed!
     * 
     * @see FsConnection#writeFile(String, InputStream, boolean)
     * @see FsConnectionFactory#MODE_ABSOLUTE_PATH
     * 
     * @param fileName a (relative) file name
     * @param data the file content (use e.g. StringInputReader to create an InputStream for a simple string)
     * @param overwrite if true the file is overwritten
     * @throws IOException if failed or overwrite is false and the file exists
     */
    void writeFile(String destFileName, InputStream data, boolean overwrite) throws IOException;

    /**
     * delete. connection will be closed!
     * 
     * @see FsConnection#delete(String)
     * @see FsConnectionFactory#MODE_ABSOLUTE_PATH
     * 
     * @param fileName file name
     */
    void delete(String fileName);

    /**
     * exists. connection will not be closed! please call {@link #closeConnection()} before leaving your method.
     * 
     * @see FsConnection#exists(String)
     * @see FsConnectionFactory#MODE_ABSOLUTE_PATH
     * 
     * @param fileName file name
     * @return true, if file already exists
     */
    boolean exists(String fileName);

    /**
     * isDirectory. connection will not be closed! please call {@link #closeConnection()} before leaving your method.
     * 
     * @param fileName file or dir to check
     * @return true, if file points to a directory
     */
    boolean isDirectory(String fileName);

    /**
     * closes the file connection, if already open (all methods will use the same connection, if possible). because,
     * this is a stateless session, the close will be called internally on garbage collection. After closing this
     * connection, it is not possible to use this instance of FileService again!
     */
    void closeConnection();
}
