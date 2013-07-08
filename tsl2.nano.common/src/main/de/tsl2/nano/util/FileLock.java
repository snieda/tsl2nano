/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Oct 29, 2009
 * 
 * Copyright: (c) Thomas Schneider 2009, all rights reserved
 */
package de.tsl2.nano.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Creates an exclusive lock using the NIO channel functionality.
 * 
 * @author egu
 * @version $Revision$
 */
public class FileLock {
    protected static final Log LOG = LogFactory.getLog(FileLock.class);
    private java.nio.channels.FileLock lockFileLock;

    /**
     * @param dir directory for file lock
     * @param file lock file name
     * @return true, if lock was free
     * @throws IOException if lock couldn't be done
     */
    public boolean lock(String dir, String file) throws IOException {
        final File lockFile = new File(dir, file);
        if (!lockFile.exists()) {
            lockFile.createNewFile();
        }
        lockFile.deleteOnExit();
        final FileChannel ctlChannel = new FileOutputStream(lockFile).getChannel();
        lockFileLock = ctlChannel.tryLock();
        if (lockFileLock == null) {
            // cannot aquire lock
            return false;
        }
        return true;
    }

    /**
     * Release a possibly existing lock.
     */
    public void releaseLock() {
        if (lockFileLock != null) {
            try {
                lockFileLock.channel().close();
            } catch (final IOException e) {
                LOG.error("failed to close lockfile channel", e);
            }
        }
    }
}
