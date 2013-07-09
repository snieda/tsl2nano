/*
 * SVN-INFO: $Id: FileUtil.java,v 1.0 11.12.2008 15:38:19 15:03:02 ts Exp $ 
 * 
 * Copyright © 2002-2008 Thomas Schneider
 * Schwanthaler Strasse 69, 80336 München. Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 *
 */
package de.tsl2.nano.util;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tsl2.nano.exception.ForwardedException;

/**
 * file helper class.
 * 
 * @author ts 11.12.2008
 * @version $Revision: 1.0 $
 * 
 */
public class FileUtil {
    static final Log LOG = LogFactory.getLog(FileUtil.class);

    /**
     * Returns a file array containing all filenames inside the given jar/zip file.
     * 
     * @param zipfile
     * @param filter e.g. *.txt
     * @return
     */
    public static String[] readFileNamesFromZip(String zipfile, String filter) {
        filter = filter.replace("*", ".*");
        //open a zip-file
        ZipInputStream sourceStream = null;
        try {
            final File zip = new File(zipfile);
            if (!zip.exists()) {
                return null;
            }
            //open the source data file
            final FileInputStream fis = new FileInputStream(zip);
            sourceStream = new ZipInputStream(fis);
            //search sources
            final List files = new LinkedList();
            ZipEntry zipEntry = null;
            while ((zipEntry = sourceStream.getNextEntry()) != null) {
                if (zipEntry.getName().matches(filter)) {
                    files.add(zipEntry.getName());
                }
                sourceStream.closeEntry();
            }
            return (String[]) files.toArray(new String[0]);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (sourceStream != null) {
                try {
                    sourceStream.close();
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * converts a file path string to a package path string. Example: java/lang/Class.class will be java.lang.Class
     * 
     * @param classFilePath
     * @return package path of the given classFilePath
     */
    public static String getPackagePath(String classFilePath) {
        classFilePath = classFilePath.replace('/', '.');
        final int extensionIndex = classFilePath.indexOf(".class");
        if (extensionIndex != -1) {
            return classFilePath.substring(0, extensionIndex);
        } else {
            return classFilePath;
        }
    }

    /**
     * guarantees existing the full path.
     * 
     * @param fileWithPath file with path
     */
    public static void createPath(String fileWithPath) {
        final File file = new File(fileWithPath);
        final File parent = file.getParentFile();
        if (parent != null) {
            file.getParentFile().mkdirs();
        }
    }

    /**
     * Serialize a Java object to XML. All attributes that have getter and setter methods will be serialized to
     * elements.
     * 
     * @param serializable Java object that implements Serializable.
     * @param outputStream Output stream to write XML to.
     */
    public static void saveXml(Serializable serializable, OutputStream outputStream) throws Exception {
        if (outputStream != null) {
            final XMLEncoder encoder = new XMLEncoder(outputStream);
            encoder.writeObject(serializable);
            encoder.close();
        }
    }//serialize()

    /**
     * saveXml
     * 
     * @param serializable object to store
     * @param fileName file name to store object in.
     */
    public static final void saveXml(Serializable serializable, String fileName) {
        createPath(fileName);
        OutputStream stream = null;
        try {
            LOG.debug("serializing to xml: " + fileName);
            stream = new FileOutputStream(new File(fileName));
            saveXml(serializable, stream);
        } catch (final Exception e) {
            ForwardedException.forward(e);
        } finally {
            if (stream != null)
                try {
                    stream.close();
                } catch (IOException e) {
                    ForwardedException.forward(e);
                }
        }
    }

    /**
     * Deserialize a Java object from XML that was serialized via the serialize method.
     * 
     * @param inputStream Input stream to read XML from.
     * @return Serializable Java object from XML.
     * @throws Exception
     * @see de.icomps.xml#serialize(Serializable, OutputStream)
     */
    public static Serializable loadXml(InputStream inputStream) throws Exception {
        Serializable result = null;

        if (inputStream != null) {
            final XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(inputStream));
            result = (Serializable) decoder.readObject();
            decoder.close();
        }

        return result;
    }//deserialize()

    public static final Serializable loadXml(String fileName) {
        LOG.info("FileUtil.loadXml from --> " + fileName);
        try {
            return loadXml(new FileInputStream(new File(fileName)));
        } catch (final Exception e) {
            return ForwardedException.forward(e);
        }
    }

    /**
     * loads a property file through main application plugin.
     * 
     * @param resourceFile properties to load
     * @param classLoader special classloader to use
     * @return filled properties
     */
    public static Properties loadProperties(String resourceFile, ClassLoader classLoader) {
        Thread.currentThread().setContextClassLoader(classLoader);
        final InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceFile);
        final Properties properties = new Properties();
        try {
            LOG.info("loading resource: " + resourceFile);
            properties.load(resource);
            return properties;
        } catch (final Exception e) {
            ForwardedException.forward(e);
            return null;
        }
    }

    /**
     * saves properties to a  file..
     * 
     * @param resourceFile properties to load
     * @param p properties to save
     */
    public static void saveProperties(String resourceFile, Properties p) {
        try {
            p.store(new FileOutputStream(new File(resourceFile)),
                "generated at " + DateFormat.getDateTimeInstance().format(new Date())
                    + " by user "
                    + System.getProperty("user.name"));
        } catch (Exception e) {
            ForwardedException.forward(e);
        }
    }

    /**
     * Gets resources for WebStart or Applets
     */
    public static final InputStream getResource(String name) {
        try {
            return getResource(name, Thread.currentThread().getContextClassLoader());
        } catch (Exception e) {
            ForwardedException.forward(e);
            return null;
        }
    }

    /**
     * Gets resources for WebStart or Applets
     */
    public static final InputStream getResource(String name, ClassLoader classLoader) throws Exception {
//        name = !name.contains("://") ? "file://" + name : name;
        return classLoader.getResourceAsStream(name);
    }

    public static final InputStream getFile(String name) {
        try {
            return new FileInputStream(new File(name));
        } catch (final FileNotFoundException e) {
            ForwardedException.forward(e);
            return null;
        }
    }

    /**
     * Read File into a byte-array
     */
    public static synchronized byte[] getFileBytes(String strFile, ClassLoader classLoader) {
        LOG.info("Try to open File " + strFile);
        InputStream file = null;
        try {
            if (classLoader == null)
                classLoader = Thread.currentThread().getContextClassLoader();
            file = getResource(strFile, classLoader);
            if (file == null) {
                file = getFile(strFile);
            }
            final int length = file.available();
            final byte data[] = new byte[length];
            file.read(data);
            file.close();
            LOG.info(length + " Bytes read");
            return data;
        } catch (final Exception e) {
            ForwardedException.forward(e);
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (final IOException e) {
                    ForwardedException.forward(e);
                }
            }
        }
        return null;
    }

    /**
     * Write 'data' into the file 'file' and perhaps appends it (if append==true)
     */
    public static void writeBytes(byte[] data, String file, boolean append) {
        LOG.info("writing " + data.length + " bytes into file " + file);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file, append);
        } catch (/*FileNotFound*/final Exception ex) {
            ForwardedException.forward(ex);
            return;
        }
        try {
            out.write(data);
            out.close();
        } catch (/*IO*/final Exception ex) {
            ForwardedException.forward(ex);
        }
    }

    /**
     * deserialize
     */
    public static Object load(String filename) {
        LOG.info("deserializing object from: " + filename);
        Object l_return = null;
        try {
            FileInputStream file = new FileInputStream(filename);
            ObjectInputStream o = new ObjectInputStream(file);
            l_return = o.readObject();
            o.close();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return l_return;
    }

    /**
     * serialize the given object
     * 
     * @param filename save name
     * @param object save object
     */
    public static void save(String filename, Object object) {
        LOG.info("serializing object to file: " + filename);
        try {
            final FileOutputStream file = new FileOutputStream(filename);
            final ObjectOutputStream o = new ObjectOutputStream(file);
            o.writeObject(object);
            o.close();
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * creates a valid file name (without spaces, slashes etc.)
     * 
     * @param originName name
     * @return file name
     */
    public static String getValidFileName(String originName) {
        return originName.replaceAll("[^a-zA-Z0-9-._]", "_");
    }

    /**
     * getFileData
     * 
     * @param fileName
     * @param encoding
     * @return
     */
    public static synchronized char[] getFileData(String fileName, String encoding) {
        try {
            LOG.debug("reading file " + fileName);
            return getFileData(new FileInputStream(new File(fileName)), encoding);
        } catch (final FileNotFoundException e) {
            ForwardedException.forward(e);
            return null;
        }
    }

    /**
     * getFileData
     * 
     * @param strFile
     * @param encoding
     * @return
     */
    public static synchronized char[] getFileData(InputStream stream, String encoding) {
        InputStreamReader file;
        try {
            file = encoding != null ? new InputStreamReader(stream, encoding) : new InputStreamReader(stream);

            final int length = stream.available();
            final char data[] = new char[length];
            file.read(data);
            file.close();
            LOG.info(length + " bytes read");
            return data;
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * copy srcFile to destFile
     * 
     * @param srcFile source file
     * @param destFile destination file
     */
    public static void copy(String srcFile, String destFile) {
        try {
            final File f1 = new File(srcFile);
            final File f2 = new File(destFile);
            final InputStream in = new FileInputStream(f1);

            //For Append the file.
            //  OutputStream out = new FileOutputStream(f2,true);

            //For Overwrite the file.
            final OutputStream out = new FileOutputStream(f2);

            final byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            LOG.info("file " + srcFile + " copied to " + destFile);
        } catch (final Exception ex) {
            LOG.error(ex.getMessage());
        }
    }

    /**
     * see {@link #removeToBackup(String, String)}. using ".bak" as backup extension
     * 
     * @param filePath file
     * @return new file name (=backup-filename), if file could be removed, otherwise null
     */
    public static final String removeToBackup(String filePath) {
        return removeToBackup(filePath, ".bak", false);
    }

    /**
     * use this method, if you would like to delete a file - but creating a backup of it. this method will rename the
     * file, if possible.
     * 
     * @param fileName file to remove
     * @param backupExtension
     * @param multiple if true, a timestamp will be inserted before the backupExtension
     * @return new file name (=backup-filename), if file could be removed, otherwise null
     */
    public static final String removeToBackup(String fileName, String backupExtension, boolean multiple) {
        final File f = new File(fileName);
        String newName = null;
        if (f.exists() && f.canWrite()) {
            if (fileName.endsWith(File.separator) || fileName.endsWith("/")) {
                fileName = fileName.substring(0, fileName.length() - 1);
            }
            final File bakFile = new File(fileName + (multiple ? DateUtil.getFormattedTimeStamp() : "") + ".bak");
            if (!multiple && bakFile.exists() && bakFile.canWrite()) {
                if (!bakFile.delete()) {
                    LOG.warn("couldn't delete bak file:" + bakFile.getPath());
                }
            }
            if (f.renameTo(bakFile)) {
                newName = bakFile.getPath();
            }
        }
        return newName;
    }

    /**
     * see {@link #restoreFrom(String, String)}. using ".bak" as backup extension
     * 
     * @param filePath file
     * @return true, if file could be restored
     */
    public static final boolean restoreFrom(String filePath) {
        return restoreFrom(filePath, ".bak");
    }

    /**
     * use this method, if you have previously called {@link #removeToBackup(String, String)} and would like to restore
     * that removed file.
     * 
     * @param fileName file name for restoring
     * @param backupExtension file name + backup extension must exists
     * @return true, if file could be restored
     */
    public static final boolean restoreFrom(String fileName, String backupExtension) {
        final File bakFile = new File(fileName + backupExtension);
        if (bakFile.exists() && bakFile.canWrite()) {
            final File f = new File(fileName);
            if (f.exists() && f.canWrite()) {
                if (!f.delete()) {
                    LOG.warn("couldn't delete file:" + bakFile.getPath());
                }
            }
            return bakFile.renameTo(f);
        } else {
            return false;
        }
    }

    /**
     * getHomePath
     * 
     * @return user.home path
     */
    public static final String getHomePath() {
        return System.getProperty("user.home") + File.separator;
    }

    /**
     * usable to evaluate file names for new generated files
     * 
     * @param baseName origin file name
     * @return unique new file name
     */
    public static final String getUniqueFileName(String baseName) {
        final int iext = baseName != null ? baseName.lastIndexOf('.') : -1;
        return iext != -1 ? baseName.substring(0, iext) + "-"
            + DateUtil.getFormattedTimeStamp()
            + baseName.substring(iext) : baseName + DateUtil.getFormattedTimeStamp();
    }

    /**
     * encodeBase64. the base64 output file will be saved on fileName + .base64
     * 
     * @param fileName
     */
    public static void encodeBase64(String fileName) {
        byte[] bytes = getFileBytes(fileName, null);
        byte[] base64 = Base64.encodeBase64(bytes);
        writeBytes(base64, fileName + ".base64", false);
    }

    /**
     * decodeBase64. the decoded output file will be saved on fileName + .base64dec
     * 
     * @param fileName
     */
    public static void decodeBase64(String fileName) {
        byte[] bytes = getFileBytes(fileName, null);
        byte[] base64 = Base64.decodeBase64(bytes);
        writeBytes(base64, fileName + ".base64decoded", false);
    }
}
