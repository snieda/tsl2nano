/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Erwin Guib, Thomas Schneider
 * created on: Oct 24, 2009
 * 
 * Copyright: (c) Thomas Schneider, all rights reserved
 */
package de.tsl2.nano.resource.fs;

import java.io.IOException;
import java.io.InputStream;

/**
 * A File system Connector connection.
 * 
 * @author Erwin Guib, Thomas Schneider
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public interface FsConnection {

    /**
     * Check if the file exists.
     * 
     * @param fileName a (relative) file name
     * @return true if existent
     * @throws IOException if failed
     */
    boolean exists(String fileName) throws IOException;

    /**
     * Check if the file is a directory.
     * 
     * @param fileName a (relative) file name
     * @return true if the file is a directry
     * @throws IOException if failed
     */
    boolean isDirectory(String fileName) throws IOException;

    /**
     * Check if the file is a file.
     * 
     * @param fileName a (relative) file name
     * @return true if a file
     * @throws IOException if failed
     */
    boolean isFile(String fileName) throws IOException;

    /**
     * @return all files and directories of the given directory path.
     * @throws IOException if failed
     */
    String[] getDirectoryEntries(String dirName) throws IOException;

    /**
     * Delete the file. Check if the file is a directory.
     * 
     * @param fileName a (relative) file name
     * @throws IOException if failed
     */
    void delete(String fileName) throws IOException;

    /**
     * rename
     * @param sourceName file to rename
     * @param destinationName new file name
     */
    void rename(String sourceName, String destinationName) throws IOException;
    
    /**
     * Write a file.
     * 
     * @param fileName a (relative) file name
     * @param data the file content
     * @param overwrite if true the file is overwritten
     * @throws IOException if failed or overwrite is false and the file exists
     */
    void writeFile(String fileName, InputStream data, boolean overwrite) throws IOException;

    /**
     * Get a stream to read a file.
     * 
     * @param fileName a (relative) file name
     * @return the stream
     * @throws IOException if failed
     */
    InputStream getInputStream(String fileName) throws IOException;

    /**
     * Close the connection.
     */
    void close();
    
    /**
     * isOpen
     * @return true, if a managed connection exists
     */
    boolean isOpen();
}
