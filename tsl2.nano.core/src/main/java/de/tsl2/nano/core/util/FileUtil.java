/*
 * SVN-INFO: $Id: FileUtil.java,v 1.0 11.12.2008 15:38:19 15:03:02 ts Exp $ 
 * 
 * Copyright © 2002-2008 Thomas Schneider
 * Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 *
 */
package de.tsl2.nano.core.util;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.execution.IRunnable;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.secure.Crypt;
import de.tsl2.nano.core.util.FileUtil.FileDetail;

/**
 * file helper class.
 * 
 * @author ts 11.12.2008
 * @version $Revision: 1.0 $
 * 
 */
public class FileUtil {
    static final Log LOG = LogFactory.getLog(FileUtil.class);

    enum FileDetail {
        name,
        date,
        size;
    };

    private static ZipInputStream getZipInputStream(String zipfile) {
        final File zip = new File(zipfile);
        if (!zip.exists()) {
            LOG.warn("zip-file " + zipfile + " not existing!");
            return null;
        }
        //open the source data file
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(zip);
            return new ZipInputStream(new BufferedInputStream(fis));
        } catch (FileNotFoundException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * Returns a file array containing all filenames inside the given jar/zip file.
     * 
     * @param zipfile
     * @param filter e.g. *.txt
     * @return
     */
    public static String[] readFileNamesFromZip(String zipfile, String filter) {
        ZipInputStream zipStream = getZipInputStream(zipfile);
        return zipStream != null ? readFileNamesFromZip(zipStream, filter, true) : null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static String[] readFileNamesFromZip(ZipInputStream sourceStream, String filter, boolean closeStream) {
        filter = filter != null ? filter.replace("*", ".*") : ".*";
        //open a zip-file
        try {
            //search sources
            final List files = new LinkedList();
            ZipEntry zipEntry = null;
            while ((zipEntry = sourceStream.getNextEntry()) != null) {
                if (zipEntry.getName().matches(filter)) {
                    files.add(zipEntry.getName());
                }
                if (closeStream) {
                    sourceStream.closeEntry();
                }
            }
            return (String[]) files.toArray(new String[0]);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (sourceStream != null && closeStream) {
                try {
                    sourceStream.close();
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * getJarInputStream
     * 
     * @param jarName
     * @return
     */
    public static final ZipInputStream getJarInputStream(String jarName) {
        InputStream jarStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(jarName);
        ZipInputStream zipStream = new ZipInputStream(jarStream);
        return zipStream;
    }

    /**
     * Returns the content of the given file inside the given zipfile.
     * 
     * @param zipfile
     * @param file
     * @return
     */
    public static byte[] readFromZip(String zipfile, String file) {
        return readFromZip(getZipInputStream(zipfile), file);
    }

    public static byte[] readFromZip(ZipInputStream sourceStream, String file) {
        return readFromZip(sourceStream, file, true);
    }

    public static byte[] readFromZip(ZipInputStream sourceStream, String file, boolean closeStream) {
        //open a zip-file
        ZipEntry zipEntry = null;
        try {
            //search source 
            while ((zipEntry = sourceStream.getNextEntry()) != null) {
                if (zipEntry.getName().equals(file)) {
                    break;
                } else {
                    sourceStream.closeEntry();
                }
            }
            if (zipEntry == null) {
                return null;
            }

            //read source
            return readBytes(sourceStream/*, zipEntry.getName(), (int) zipEntry.getSize()*/);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (sourceStream != null && closeStream) {
                try {
                    sourceStream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * extract given zipStream to destDir using regExFilter. not performance-optimized!
     * 
     * @param zipStream
     * @param destDir
     * @param regExFilter
     * @return extracted files
     */
    public static List<File> extractNestedZip(String zipFile, String destDir, String regExFilter) {
        ZipInputStream zipStream = FileUtil.getJarInputStream(zipFile);
        String[] zipFiles = FileUtil.readFileNamesFromZip(zipStream, regExFilter, true);
        /*
         * as the inflaterzipstream is not able to reset it's read-position, we have
         * to reopen the zip-stream
         */
        zipStream = FileUtil.getJarInputStream(zipFile);

        //reopen it - the zipEntries are closed
        List<File> extracted = new ArrayList<File>(zipFile.length());
        for (String file : zipFiles) {
            byte[] data = FileUtil.readFromZip(zipStream, file, false);
            if (data == null || data.length == 0) {
                new File(destDir + file).mkdirs();
            } else if (!new File(destDir + file).exists()) {
                FileUtil.writeBytes(data, destDir + file, false);
                extracted.add(new File(destDir + file));
            }
        }
        return extracted;
    }

    /**
     * extract given zipStream to destDir using regExFilter. not performance-optimized!
     * 
     * @param zipStream
     * @param destDir
     * @param regExFilter
     */
    public static void extract(String zipFile, String destDir, String regExFilter) {
        ZipInputStream zipStream = FileUtil.getZipInputStream(zipFile);
        String[] zipFiles = FileUtil.readFileNamesFromZip(zipStream, regExFilter, true);
        /*
         * as the inflaterzipstream is not able to reset it's read-position, we have
         * to reopen the zip-stream
         */
        zipStream = FileUtil.getZipInputStream(zipFile);

        //reopen it - the zipEntries are closed
        for (String file : zipFiles) {
            byte[] data = FileUtil.readFromZip(zipStream, file, false);
            if (data == null || data.length == 0) {
                new File(destDir + file).mkdirs();
            } else if (!new File(destDir + file).exists()) {
                FileUtil.writeBytes(data, destDir + file, false);
            }
        }

    }

    //perhaps we can use it in future
    private static byte[] readBytes(InputStream stream, String entryName, int len) throws IOException {
        LOG.debug("loading stream-entry " + entryName + " with " + len + " bytes");
        byte[] b;
        int read = 0;
        int offset = 0;
        b = new byte[len];
        do {
            read = stream.read(b, offset, len - offset);
            offset += read;
        } while (read > 0);
        return b;
    }

    public static byte[] readBytes(InputStream stream) throws IOException {
        return readBytes(stream, new ByteArrayOutputStream()).toByteArray();
    }

    public static <O extends OutputStream> O readBytes(InputStream stream, O output) throws IOException {
        int r;
        while ((r = stream.read()) != -1) {
            output.write(r);
        }
        return output;
    }

    /**
     * delegates to {@link #writeToZip(String, String, byte[])}
     */
    public static void writeToZip(String zipfile, String file, String data) {
        writeToZip(zipfile, file, data.getBytes());
    }

    /**
     * Writes the given file with data to the given zipfile.
     * 
     * @param zipfile zip file
     * @param file file-name in zip-file
     * @param data data to store in file in zip-file
     */
    public static void writeToZip(String zipfile, String file, byte[] data) {
        //open a zip-file
        ZipOutputStream targetStream = null;
        try {
            File zip = new File(zipfile);
            if (!zip.exists()) {
                zip.getParentFile().mkdirs();
                zip.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(zip);
            targetStream = new ZipOutputStream(fos);
            targetStream.setMethod(ZipOutputStream.DEFLATED);

            //open the source data file
            //FileInputStream fis = new FileInputStream(file);
            //BufferedInputStream sourceStream = new BufferedInputStream(fis);

            //create the zip entry
            ZipEntry zipEntry = new ZipEntry(file);
            targetStream.putNextEntry(zipEntry);

            //read source and write the data to the zip output stream
            /*  int DATA_BLOCK_SIZE = 1024;
                byte[] data = new byte[DATA_BLOCK_SIZE];
                int bCnt;
                while((bCnt = sourceStream.read(data, 0, DATA_BLOCK_SIZE)) != -1)
                {
                    targetStream.write(data, 0, bCnt);
                }
            */
            targetStream.write(data);
            targetStream.flush();

            System.out.println("Writing into [" + zipfile
                + "]:"
                + zipEntry.getName()
                + " ("
                + zipEntry.getCompressedSize()
                + " / "
                + zipEntry.getSize()
                + ")");
            targetStream.closeEntry();
            //sourceStream.close();
        } catch (Exception ex) {
            LOG.error(ex);
            ManagedException.forward(ex);
        } finally {
            //close the zip entry and other open streams
            close(targetStream, false);
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
     * converts a class path string to a file path string. Example: java.lang.Class will be java/lang/Class
     * 
     * @param classPath
     * @return file path of the given classPath
     */
    public static String getFilePath(String classPath) {
        return classPath.replace('.', '/');
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
            ManagedException.forward(e);
        } finally {
            close(stream, true);
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
            return ManagedException.forward(e);
        }
    }

    public static Properties loadPropertiesFromFile(String resourceFile) {
        File f = new File(resourceFile);
        if (!f.canRead()) {
            return null;
        }
        final Properties properties = new Properties();
        try {
            LOG.info("loading resource: " + resourceFile);
            properties.load(new FileReader(f));
            return properties;
        } catch (final Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    public static Properties loadProperties(String resourceFile) {
    	return loadProperties(resourceFile, null);
    }
    
    /**
     * loads a property resource file through main application plugin.
     * 
     * @param resourceFile properties to load
     * @param classLoader special classloader to use
     * @return filled properties
     */
    public static Properties loadProperties(String resourceFile, ClassLoader classLoader) {
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        Thread.currentThread().setContextClassLoader(classLoader);
        final InputStream resource = classLoader.getResourceAsStream(resourceFile);
        if (resource == null) {
            throw new IllegalArgumentException("file: " + resourceFile + " not found");
        }
        final Properties properties = new Properties();
        try {
            LOG.info("loading resource: " + resourceFile);
            properties.load(resource);
            return properties;
        } catch (final Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * saves properties to a file..
     * 
     * @param resourceFile properties to load
     * @param p properties to save
     */
    public static void saveProperties(String resourceFile, Properties p) {
        try {
            p.store(
                new FileOutputStream(new File(resourceFile)),
                "generated at " + DateFormat.getDateTimeInstance()
                    .format(new Date()) + " from code " + ConcurrentUtil.getCaller() + " by user "
                    + System.getProperty("user.name"));
        } catch (Exception e) {
            ManagedException.forward(e);
        }
    }

    /**
     * hasResource
     * 
     * @param name resource name
     * @return true, if resource was found by the current threads classloader
     */
    public static final boolean hasResource(String name) {
        return Thread.currentThread().getContextClassLoader().getResource(name) != null;
    }

    /**
     * Gets resources for WebStart or Applets
     */
    public static final InputStream getResource(String name) {
        try {
            return getResource(name, Thread.currentThread().getContextClassLoader());
        } catch (Exception e) {
            ManagedException.forward(e);
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

    /**
     * gets the content of the given {@link URL} and writes it to the given file.
     * <p/>
     * Important: works not on image files. they will be handled as URLImageSource using awt.
     * 
     * @param url url to save
     * @param fileName urls backup file name
     */
    public static final void saveResourceToFileSystem(URL url, final String fileName) {
        try {
            write((InputStream) url.getContent(), new FileOutputStream(fileName) {
                @Override
                public String toString() {
                    return fileName;
                }
            }, true);
        } catch (IOException e) {
            //this copy-process should not break the application
            LOG.error(e);
        }
    }

    public static final InputStream getFile(String name) {
        try {
            return new FileInputStream(new File(name));
        } catch (final FileNotFoundException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    public static final OutputStream getFileOutput(String name) {
        try {
            return new FileOutputStream(new File(name));
        } catch (final FileNotFoundException e) {
            ManagedException.forward(e);
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
            if (classLoader == null) {
                classLoader = Thread.currentThread().getContextClassLoader();
            }
            file = getResource(strFile, classLoader);
            if (file == null) {
                file = getFile(strFile);
            }
            final int length = file.available();
            final byte data[] = new byte[length];
            file.read(data);
            file.close();
            LOG.info(ByteUtil.amount(length));
            return data;
        } catch (final Exception e) {
            ManagedException.forward(e);
        } finally {
            close(file, true);
        }
        return null;
    }

    /**
     * Write 'data' into the file 'file' and perhaps appends it (if append==true)
     */
    public static void writeBytes(byte[] data, String file, boolean append) {
        LOG.info("writing " + ByteUtil.amount(data.length) + " into file " + file);
        FileOutputStream out = null;
        try {
            File f = new File(file);
            if (f.getParentFile() != null)
                f.getParentFile().mkdirs();
            out = new FileOutputStream(f, append);
        } catch (/*FileNotFound*/final Exception ex) {
            ManagedException.forward(ex);
            return;
        }
        try {
            out.write(data);
            out.close();
        } catch (/*IO*/final Exception ex) {
            ManagedException.forward(ex);
        }
    }

    /**
     * deserialize
     */
    public static Object load(String filename) {
        LOG.info("deserializing object from: " + filename);
        Object l_return = null;
        ObjectInputStream o = null;
        try {
            FileInputStream file = new FileInputStream(filename);
            o = new ObjectInputStream(file);
            l_return = o.readObject();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            close(o, false);
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
        ObjectOutputStream o = null;
        try {
            final FileOutputStream file = new FileOutputStream(filename);
            o = new ObjectOutputStream(file);
            o.writeObject(object);
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            close(o, false);
        }
    }

    /**
     * creates a valid file name (without spaces, slashes etc.)
     * 
     * @param originName name
     * @return file name
     */
    public static String getValidFileName(String originName) {
        String name = originName.replaceAll("[^a-zA-Z0-9-._]", "_");
        return name.length() > 128 ? name.substring(0, 128) : name;
    }

    /**
     * creates a valid file-path name (without spaces and other non-word characters). the difference to
     * {@link #getValidFileName(String)} is, that slashes are valid.
     * 
     * @param originName name
     * @return file name
     */
    public static String getValidPathName(String originName) {
        String name = originName.replaceAll("[^a-zA-Z0-9-/._]", "_");
        return name.length() > 128 ? name.substring(0, 128) : name;
    }

    /**
     * getFileData
     * 
     * @param fileName
     * @param encoding
     * @return
     */
    public static synchronized char[] getFileData(final String fileName, String encoding) {
        try {
            LOG.debug("reading file " + fileName);
            return getFileData(new FileInputStream(new File(fileName)) {
                @Override
                public String toString() {
                    return fileName;
                }
            }, encoding);
        } catch (final FileNotFoundException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * getFileData
     * 
     * @param strFile
     * @param encoding (optional)
     * @return content
     */
    public static synchronized char[] getFileData(InputStream stream, String encoding) {
        InputStreamReader file = null;
        try {
            file = encoding != null ? new InputStreamReader(stream, encoding) : new InputStreamReader(stream);

            final int length = stream.available();
            final char data[] = new char[length];
            file.read(data);
            file.close();
            LOG.info(ByteUtil.amount(length) + " read from stream " + stream);
            return data;
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            close(file, false);
        }
    }

    /**
     * delegates to {@link #getTransformingReader(Reader, char, char)} using a buffered reader
     */
    public static Reader getTransformingReader(InputStream stream,
            final char transform,
            final char replace,
            final boolean ignoreFirstLine) {
        return getTransformingReader(new BufferedReader(new InputStreamReader(stream)),
            transform,
            replace,
            ignoreFirstLine);
    }

    /**
     * creates a reader that will replace each char that equals the transform character. usable to prefilter a file.
     * <p/>
     * to ignore quotations, read by a StringTokenizer (using reader.read()) you can call:<br>
     * FileUtil.getTransformingReader(FileUtil.getFile(testFile), '\"', (char)0);
     * 
     * @param stream stream to read
     * @param transform char to transform
     * @param replace replacing transform character. if 0 the single char reading will ignore the transform characters.
     * @return new reader instance
     */
    public static Reader getTransformingReader(Reader reader,
            final char transform,
            final char replace,
            final boolean ignoreFirstLine) {
        Reader r = new FilterReader(reader) {
            boolean firstLineRead = false;

            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                if (ignoreFirstLine && !firstLineRead) {
                    skipLine();
                    firstLineRead = true;
                }
                int count = super.read(cbuf, off, len);

                char[] carr = String.valueOf(cbuf).replace(transform, replace).toCharArray();
                System.arraycopy(carr, 0, cbuf, 0, count);
                return count;
            }

            @Override
            public int read() throws IOException {
                if (ignoreFirstLine && !firstLineRead) {
                    skipLine();
                    firstLineRead = true;
                }
                int c = super.read();
                if (replace == 0) {
                    return (char) c == transform ? super.read() : c;
                } else {
                    return (char) c == transform ? replace : c;
                }
            }

            void skipLine() throws IOException {
                int c;
                while ((c = super.read()) != -1 && (char) c != '\n') {
                    ;
                }
            }
        };
        return r;
    }

    /**
     * copy srcFile to destFile
     * 
     * @param srcFile source file
     * @param destFile destination file
     */
    public static boolean copy(String srcFile, String destFile) {
        try {
            final File f1 = new File(srcFile);
            final File f2 = new File(destFile);
            if (f2.getParentFile() != null)
                f2.getParentFile().mkdirs();
            write(new FileInputStream(f1), new FileOutputStream(f2), destFile, true);
            LOG.info("file " + srcFile + " copied to " + destFile);
            return true;
        } catch (final Exception ex) {
            LOG.error(ex.getMessage());
            return false;
        }
    }

    /**
     * write given input stream to given file
     * 
     * @param in stream
     * @param fileName file to write the input stream into
     */
    public static long write(InputStream in, final String fileName) {
        try {
            return write(in, new FileOutputStream(new File(fileName)) {
                @Override
                public String toString() {
                    return fileName;
                }
            }, true);
        } catch (FileNotFoundException e) {
            ManagedException.forward(e);
            return -1;
        }
    }

    /**
     * @delegates to {@link #write(InputStream, OutputStream, String, boolean)}
     */
    public static long write(InputStream in, OutputStream out, boolean closeStreams) {
        return write(in, out, null, closeStreams);
    }

    /**
     * write
     * 
     * @param in stream to read
     * @param out stream to write
     * @param outLogName (optional) name of outputstream to be logged
     * @param closeStreams whether to close the given stream on finishing
     * @return read/written byte count
     */
    public static long write(InputStream in, OutputStream out, String outLogName, boolean closeStreams) {
        final byte[] buf = new byte[1024];
        int len;
        try {
            long count = 0;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
                count += len;
            }
            LOG.info(ByteUtil.amount(count) + " written to " + outLogName != null ? outLogName : out);
            return count;
        } catch (IOException e) {
            ManagedException.forward(e);
            return -1;
        } finally {
            if (closeStreams) {
                close(in, false);
                close(out, true);
            }
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

    /**
     * delegates to {@link #getFileset(String, String, FileDetail, boolean)}.
     */
    public static List<File> getFileset(String dir, String include) {
        return getFileset(dir, include, null, true);
    }

    /**
     * ant-like fileset. returns all files matching the given include expression.
     * 
     * @param dir base directory to start from
     * @param include expression to be matched by file name
     * @return all files, matching the given include expression.
     */
    public static List<File> getFileset(String dir, String include, FileDetail sortBy, boolean sortUp) {
        return getTreeFiles(dir, transformAntToRegEx(include), sortBy, sortUp, true);
    }

    /**
     * delegates to {@link #getTreeFiles(String, String, FileDetail, boolean)}.
     */
    public static List<File> getTreeFiles(String basePath,
            final String regExFilename) {
        return getTreeFiles(basePath, regExFilename, null, true, true);
    }

    /**
     * delegates to {@link #getTreeFiles(String, String, FileDetail, boolean)}.
     */
    public static List<File> getTreeFiles(String basePath,
            final String regExFilename, boolean caseSensitive) {
        return getTreeFiles(basePath, regExFilename, null, true, caseSensitive);
    }

    /**
     * walks through the file tree, starting from basePath, collecting all files, that matches the given regExFilename.
     * 
     * @param basePath starting dir
     * @param regExFilename regular expression for the file name. Must not contain windows path separators: '\'. Please
     *            use '/' of java and linux!
     * @return all files in tree, matching regExFilename
     */
    public static List<File> getTreeFiles(String basePath,
            final String regExFilename,
            FileDetail sortBy,
            boolean sortUp,
            boolean caseSensitive) {
        LinkedList<File> result = new LinkedList<File>();
        try {
            getTreeFiles(basePath, basePath, regExFilename, result, caseSensitive);
            if (sortBy != null) {
                Collections.sort(result, new FileComparator(sortBy, sortUp));
            }
            LOG.debug("fileset(" + basePath + regExFilename + " --> " + StringUtil.toString(result, 200));
            return result;
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    static Collection<File> getTreeFiles(String basePath, String path, String regExFilename, Collection<File> result, boolean caseSensitive)
            throws Exception {
        File[] files = new File(path).listFiles();
        if (files == null) {
            throw new IllegalArgumentException("'" + path + "' is not a directory");
        }
        regExFilename = caseSensitive ? regExFilename : regExFilename.toLowerCase();
        String canonPath = new File(basePath).getCanonicalPath();
        path = caseSensitive ? canonPath : canonPath.toLowerCase();
        String pattern =
            "\\Q" + path + "\\E"
                + regExFilename.replace("/", "\\Q" + File.separator + "\\E");
        for (File file : files) {
            if ((caseSensitive && file.getCanonicalPath().matches(pattern))
                    || (!caseSensitive && file.getCanonicalPath().toLowerCase().matches(pattern))) {
                result.add(file);
            }
            //no else-if, a directory can match, too. --> recursion
            if (file.isDirectory()) {
                getTreeFiles(basePath, file.getPath(), regExFilename, result, caseSensitive);
            }
        }
        return result;
    }

    /**
     * getFiles
     * 
     * @param dirPath directory to search files for
     * @param regExFilename file name matching filter
     * @return files of directory 'dirPath' matching 'regExFilename'
     */
    public static File[] getFiles(String dirPath, final String regExFilename) {
        return new File(dirPath).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches(regExFilename);
            }
        });
    }

    /**
     * transforms a given ant file-filter to a regular-expression. <br/>
     * Example (please replace the plus with star):
     * 
     * <pre>
     * ++/+.jar --> .+/+.jar
     * </pre>
     * 
     * @param antFileFilter
     * @return regular expression
     */
    public static final String transformAntToRegEx(String antFileFilter) {
        return antFileFilter.replace("**/", ".*").replace("**", ".*").replaceAll("([^.])\\*", "$1.*");
    }

    /**
     * delegates to {@link #forTree(String, String, IRunnable, Comparator)}.
     */
    public static <T> Iterable<T> forTree(String dirPath, final String include, final IRunnable<T, File> action) {
        return forTree(dirPath, include, action, null);
    }

    /**
     * evaluates all files in dirPath matching include pattern.
     * 
     * @param dirPath base directory
     * @param include ant fileset include pattern.
     * @param action action to be done for each matching file in tree.
     * @return collected results of all calls of action.
     */
    public static <T> Iterable<T> forTree(String dirPath,
            final String include,
            final IRunnable<T, File> action,
            Comparator<File> sorter) {
        List<File> files = getFileset(dirPath, include);
        if (sorter != null) {
            Collections.sort(files, sorter);
        }
        Collection<T> result = new ArrayList<T>();
        for (File file : files) {
            result.add(action.run(file));
        }
        return result;
    }

    /**
     * action for {@link #forEach(String, String, IRunnable)}
     */
    public static final IRunnable<Object, File> DO_DELETE = new IRunnable<Object, File>() {
        @Override
        public Object run(File context, Object... extArgs) {
            return context.delete();
        }
    };

    /**
     * action for {@link #forEach(String, String, IRunnable)}
     */
    public static final IRunnable<Object, File> DO_COPY = new IRunnable<Object, File>() {
        @Override
        public Object run(File context, Object... extArgs) {
            copy(context.getPath(), (String) extArgs[0]);
            return new File((String) extArgs[0]);
        }
    };

    /**
     * starts the given action for the matching file set.
     * 
     * @param dirPath root path to work on
     * @param regExFilename regular expression for file name that must be matched to start the action on that file.
     * @param action (optional) action on a matching file
     * @param args (optional) arguments to be used by the given action.
     * @return files that matched the regular expression.
     */
    public static File[] forEach(String dirPath,
            final String regExFilename,
            final IRunnable<Object, File> action,
            final Object... args) {
        return new File(dirPath).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                boolean accept = name.matches(regExFilename);
                if (accept && action != null) {
                    action.run(new File(dir.getPath() + "/" + name), args);
                }
                return accept;
            }
        });
    }

    public static final boolean isAbsolute(String path) {
        return path.startsWith(File.separator) || path.contains(":" + File.separator);
    }
    
    /**
     * getRelativePath
     * 
     * @param file
     * @return path relative to current user.dir (application start)
     */
    public static String getRelativePath(String file) {
        return getRelativePath(file, System.getProperty("user.dir"));
    }

    public static String getRelativePath(File file, String currentPath) {
        return getRelativePath(file.getPath(), new File(currentPath).getPath());
    }

    /**
     * getRelativePath
     * 
     * @param file
     * @return path relative to current user.dir (application start)
     */
    public static String getRelativePath(String file, String currentPath) {
        String relpath =
            StringUtil.substring(replaceWindowsSeparator(file), replaceWindowsSeparator(currentPath), null);
        return relpath.startsWith("/") && (File.separatorChar == '\\' || !new File(relpath).exists()) ? relpath
            .substring(1) : relpath;
    }

    public static final String replaceWindowsSeparator(String path) {
        return path.replace(File.separatorChar, '/');
    }

    /**
     * checks if given path exists. if path is an URI, the URI will be checked
     * 
     * @param pathOrURL
     * @return true, if file or URL exists
     */
    public static File getURIFile(String pathOrURL) {
        return new File(URI.create(replaceWindowsSeparator(pathOrURL)).getSchemeSpecificPart());
    }

    public static InputStream getURLStream(String url) {
        return NetUtil.getURLStream(url);
    }

    public static String getDetails(File file) {
        return "name    : " + decorate(file)
            + "\npath    : " + file.getParent()
            + "\nmodified: " + DateUtil.getFormattedDateTime(new Date(file.lastModified()))
            + "\naccess  : " + (file.canRead() ? "r" : "") + (file.canWrite() ? "w" : "")
            + (file.canExecute() ? "x" : "")
            + "\nsize    : " + BitUtil.amount(file.length());
    }

    private static String decorate(File file) {
        StringBuilder prefix = new StringBuilder(3);
        StringBuilder postfix = new StringBuilder(3);
        if (file.isDirectory()) {
            prefix.append("[");
            postfix.append("]");
        }
        if (file.isHidden()) {
            prefix.append("<");
            postfix.insert(0, ">");
        }
        return prefix + file.getName() + postfix;
    }

    /**
     * convenience to close any inputstream
     * 
     * @param inputStream stream to close
     * @param forwardException if true and an {@link IOException} was thrown, it will be re-thrown - otherwise it will
     *            only be logged.
     * @return null, if close() was successful, otherwise the given instances
     */
    public static final <T extends InputStream> T close(T inputStream, boolean forwardException) {
        if (inputStream != null)
            try {
                inputStream.close();
                return null;
            } catch (IOException e) {
                if (forwardException)
                    ManagedException.forward(e);
                else
                    LOG.error("can't close inputstream " + inputStream, e);
            }
        return inputStream;
    }

    /**
     * convenience to close any outputstream
     * 
     * @param outputStream stream to close
     * @param forwardException if true and an {@link IOException} was thrown, it will be re-thrown - otherwise it will
     *            only be logged.
     * @return null, if close() was successful, otherwise the given instances
     */
    public static final <T extends OutputStream> T close(T outputStream, boolean forwardException) {
        if (outputStream != null)
            try {
                outputStream.close();
                return null;
            } catch (IOException e) {
                if (forwardException)
                    ManagedException.forward(e);
                else
                    LOG.error("can't close inputstream " + outputStream, e);
            }
        return outputStream;
    }

    /**
     * convenience to close any reader
     * 
     * @param reader stream to close
     * @param forwardException if true and an {@link IOException} was thrown, it will be re-thrown - otherwise it will
     *            only be logged.
     * @return null, if close() was successful, otherwise the given instances
     */
    public static final <T extends Reader> T close(T reader, boolean forwardException) {
        if (reader != null)
            try {
                reader.close();
                return null;
            } catch (IOException e) {
                if (forwardException)
                    ManagedException.forward(e);
                else
                    LOG.error("can't close inputstream " + reader, e);
            }
        return reader;
    }

    /**
     * convenience to close any writer
     * 
     * @param writer stream to close
     * @param forwardException if true and an {@link IOException} was thrown, it will be re-thrown - otherwise it will
     *            only be logged.
     * @return null, if close() was successful, otherwise the given instances
     */
    public static final <T extends Writer> T close(T writer, boolean forwardException) {
        if (writer != null)
            try {
                writer.close();
                return null;
            } catch (IOException e) {
                if (forwardException)
                    ManagedException.forward(e);
                else
                    LOG.error("can't close writer " + writer, e);
            }
        return writer;
    }

    /**
     * deletes all sub-directories of the given directory
     * 
     * @param dir directory to delete with all sub-directories
     * @return true, if given dir could be deleted
     */
    public static boolean deleteRecursive(File dir) {
        if (!dir.canWrite())
            LOG.error(dir.getPath() + " can't be deleted!");
        File[] files = dir.listFiles();
        if (files != null) {
            LOG.debug("deleting " + files.length + " sub-directories/files of " + dir.getPath() + "...");
            for (int i = 0; i < files.length; i++) {
                deleteRecursive(files[i]);
            }
        }

        return dir.delete();
    }

    /**
     * creates a checksum for the data of the given file with the given algorithm 
     * @param file to check
     * @param algorithm to use
     * @return
     */
    public static String getChecksum(String file, String algorithm) {
        return Crypt.hashHex(getFile(file), algorithm);
    }

    /**
     * checks given file for given expected checksum hash through given algorithm like MD5, SHA-1, SHA-256, SHA-512 etc.
     * 
     * @param file to check
     * @param algorithm to use
     * @param expectedHash to check against
     */
    public static void checksum(String file, String algorithm, String expectedHash) {
        if (!getChecksum(file, algorithm).equals(expectedHash))
            throw new IllegalStateException(
                file + ": file hash error. file seems to be corrupt (expected hash: " + expectedHash);
    }
}

class FileComparator implements Comparator<File> {
    FileDetail sortDetail;
    boolean sortUp;

    /**
     * constructor
     * 
     * @param sortDetail
     * @param sortUp
     */
    public FileComparator(FileDetail sortDetail, boolean sortUp) {
        super();
        this.sortDetail = sortDetail;
        this.sortUp = sortUp;
    }

    @Override
    public int compare(File o1, File o2) {
        int direction = (sortUp ? 1 : -1);
        switch (sortDetail) {
        case name:
            return direction * o1.getName().compareTo(o2.getName());
        case date:
            return direction * Long.valueOf(o1.lastModified()).compareTo(Long.valueOf(o2.lastModified()));
        case size:
            return direction * Long.valueOf(o1.length()).compareTo(Long.valueOf(o2.length()));
        }
        throw new IllegalArgumentException(sortDetail + " not allowed!");
    }

}