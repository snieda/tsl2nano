/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Sep 16, 2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */
package de.tsl2.nano.service.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.resource.ResourceException;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.resource.fs.FsConnection;
import de.tsl2.nano.resource.fs.FsConnectionFactory;

/**
 * file access as local and remote service. provides reading and writing files on server through j2ee file-connector.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
@Stateless
@Local(IFileLocalService.class)
@Remote(IFileService.class)
public class FileServiceBean implements IFileService, IFileLocalService {

    private static final Log LOG = LogFactory.getLog(FileServiceBean.class);

    @Resource(mappedName = "java:kion/fsConnectionFactory")
    FsConnectionFactory fsConnectionFactory;

    FsConnection con = null;

    /**
     * @return Returns the con.
     * @throws ResourceException
     */
    public FsConnection getConnection() throws ResourceException {
        if (con == null) {
            con = fsConnectionFactory.getConnection();
        } else if (!con.isOpen()) {
            LOG.warn("connection was already closed! creating a new one...");
            con = fsConnectionFactory.getConnection();
        }
        return con;
    }

    /**
     * for local access {@inheritDoc}
     */
    @Override
    public BufferedReader getFileReader(String fileName) {
        try {
            final FsConnection con = getConnection();
            return new BufferedReader(new InputStreamReader(con.getInputStream(fileName)));
        } catch (final Exception e) {
            closeConnection();
            ManagedException.forward(e);
        }
        return null;
    }

    /**
     * for remote access {@inheritDoc}
     */
    @Override
    public byte[] getFileContent(String fileName) {
        try {
            final FsConnection con = getConnection();
            final InputStream inputStream = con.getInputStream(fileName);
            final byte[] bytes = new byte[inputStream.available()];
            LOG.debug("reading " + bytes.length + " bytes from file " + fileName);
            inputStream.read(bytes, 0, bytes.length);
            return bytes;
        } catch (final Exception e) {
            ManagedException.forward(e);
        } finally {
            closeConnection();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rename(String sourceName, String destinationName) throws IOException {
        try {
            final FsConnection con = getConnection();
            LOG.debug("renaming " + sourceName + " to file " + destinationName);
            con.rename(sourceName, destinationName);
        } catch (final Exception e) {
            ManagedException.forward(e);
        } finally {
            closeConnection();
        }
    }

    /**
     * for local access {@inheritDoc}
     */
    @Override
    public void writeFile(String destFileName, InputStream data, boolean overwrite) throws IOException {
        try {
            final FsConnection con = getConnection();
            LOG.debug("writing " + data.available() + " bytes to file " + destFileName);
            con.writeFile(destFileName, data, overwrite);
        } catch (final Exception e) {
            ManagedException.forward(e);
        } finally {
            closeConnection();
        }
    }

    /**
     * for remote access {@inheritDoc}
     */
    @Override
    public void writeFile(String destFileName, byte[] data, boolean overwrite) throws IOException {
        try {
            final FsConnection con = getConnection();
            final InputStream stream = new ByteArrayInputStream(data);
            //TODO: stream serializing problem
            LOG.debug("writing " + data.length + " bytes to file " + destFileName);
            con.writeFile(destFileName, stream, overwrite);
        } catch (final Exception e) {
            ManagedException.forward(e);
        } finally {
            closeConnection();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String fileName) {
        try {
            final FsConnection con = getConnection();
            con.delete(fileName);
        } catch (final Exception e) {
            ManagedException.forward(e);
        } finally {
            closeConnection();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(String fileName) {
        try {
            final FsConnection con = getConnection();
            return con.exists(fileName);
        } catch (final Exception e) {
            closeConnection();
            ManagedException.forward(e);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory(String fileName) {
        try {
            return getConnection().isDirectory(fileName);
        } catch (final Exception e) {
            closeConnection();
            ManagedException.forward(e);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getDirectoryEntries(String dirName) {
        try {
            return getConnection().getDirectoryEntries(dirName);
        } catch (final Exception e) {
            closeConnection();
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeConnection() {
        if (con != null) {
            LOG.debug("closing connection " + con);
            con.close();
            con = null;
        }
    }

}
