/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 10.03.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.core.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.sql.Blob;
import java.util.Arrays;

import javax.sql.rowset.serial.SerialBlob;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.execution.IRunnable;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.util.test.inverse.InverseFunction;

/**
 * some utils for byte-arrays
 * 
 * @author Tom
 * @version $Revision$
 */
public class ByteUtil extends Util {
    protected static final Log LOG = LogFactory.getLog(ByteUtil.class);

    /**
     * isByteStream
     * 
     * @param type class to analyse
     * @return true, if type is a byte (or Byte) array, or simple Serializable interface.
     */
    public static boolean isByteStream(Class<?> type) {
        return (type.equals(Serializable.class) && isFrameworkClass(type)) // e.g. IPresentable.getLyout() should not be a byte stream
            || (type.isArray() && (Byte[].class.isAssignableFrom(type) || byte[].class.isAssignableFrom(type)))
            || ByteBuffer.class.isAssignableFrom(type) || InputStream.class.isAssignableFrom(type) || Blob.class.isAssignableFrom(type);
    }

    /**
     * converts given bytes to any stream, defined by type. is able to fill {@link Blob}, {@link String},
     * {@link ByteBuffer}, {@link Byte}[] and any serializeable. see {@link #toByteArray(InputStream)}.
     * 
     * @param bytes source bytes
     * @param type to create an instance for, filled with source bytes
     * @return new instance of type
     */
    @SuppressWarnings("unchecked")
    @InverseFunction(methodName = "getBytes", parameters = {Object.class}, compareParameterIndex = 0)
    public static <T> T toByteStream(byte[] bytes, Class<T> type) {
        if (byte[].class.isAssignableFrom(type)) {
            return (T) bytes;
        } else if (Blob.class.isAssignableFrom(type)) {
            try {
                return (T) new SerialBlob(bytes);
            } catch (Exception e) {
                ManagedException.forward(e);
                return null;
            }
        } else if (Byte[].class.isAssignableFrom(type)) {
            Byte[] b = new Byte[bytes.length];
            System.arraycopy(bytes, 0, b, 0, bytes.length);
            return (T) b;
        } else if (String.class.isAssignableFrom(type)) {
            return (T) new String(bytes);
        } else if (ByteBuffer.class.isAssignableFrom(type)) {
            return (T) ByteBuffer.wrap(bytes);
        } else if (InputStream.class.isAssignableFrom(type)) {
            return (T) new ByteArrayInputStream(bytes);
        } else {
            return (T) convertToObject(bytes, null);
        }
    }

    /**
     * simple convenience to add string bytes to the given output stream
     * @param out
     * @param data
     * @param close
     */
    public static void addToByteStream(OutputStream out, String data, boolean close) {
        try {
            out.write(data.getBytes());
        } catch (IOException e) {
            ManagedException.forward(e);
        }
        if (close)
            FileUtil.close(out, true);
    }
    /**
     * getBytes
     * 
     * @param o
     * @return bytes of the current object
     */
    public static byte[] getBytes(Object o) {
        if (o instanceof byte[])
            return (byte[]) o;
        if (o instanceof Byte[])
            return serialize(o);
        else if (o instanceof String)
            return ((String) o).getBytes();
        else if (o instanceof ByteBuffer)
            return ((ByteBuffer) o).array();
        else if (o instanceof InputStream)
            return toByteArray((InputStream) o);
        else if (o instanceof Blob)
            try {
                return toByteArray(((Blob) o).getBinaryStream());
            } catch (Exception e) {
                ManagedException.forward(e);
                return null;
            }
        else
            return serialize(o);
    }

    /**
     * Serialization of a bean-object to a byte-array.
     * 
     * @param instance to serialize to byte array
     * @return serialized bean
     */
    public static byte[] serialize(Object instance) {
        if (!(instance instanceof Serializable)) {
            if (instance != null) {
                LOG.warn("trying to serialize a non-serializeable object: " + instance.getClass().getName());
            }
            return null;//throw new ManagedException("bean must implement serializeable!");
        }
        return convertToByteArray(instance);
    }

    /**
     * Serialization of a bean object to a byte-array
     * 
     * @param bean to serialize to byte array
     * @return serialized bean
     */
    protected static byte[] convertToByteArray(Object bean) {
        try {
            LOG.debug("creating byte array through serializing object of type " + bean.getClass());
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final ObjectOutputStream o = new ObjectOutputStream(bos);
            o.writeObject(bean);
            o.close();
            LOG.debug("serialized byte array for type " + bean.getClass() + " size: " + bos.size() + " bytes");
            return bos.toByteArray();
        } catch (final IOException ex) {
            ManagedException.forward(ex);
            return null;
        }

    }

    public static Object convertToObject(byte[] bytes) {
    	return convertToObject(bytes, null);
    }
    
    /**
     * deserialization of a byte-array
     * 
     * @param bytes to deserialize
     * @return deserialized bean
     */
    public static Object convertToObject(byte[] bytes, final ClassLoader classLoader) {
        try {
            final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            final ObjectInputStream i;
            /*
             * if a classloader was given, we will use it - otherwise the native evaluated classloader will work.
             */
            if (classLoader != null) {
                i = new ObjectInputStream(bis) {
                    @Override
                    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                        String name = desc.getName();
                        try {
                            return Class.forName(name, false, classLoader);
                        } catch (Exception ex) {
                            return super.resolveClass(desc);
                        }
                    }
                };
            } else {
                i = new ObjectInputStream(bis);
            }
            /*
             * now, do the standard things
             */
            final Object object = i.readObject();
            i.close();
            return object;
        } catch (final Exception ex) {
            return ManagedException.forward(ex);
        }

    }

    /**
     * Compares two Objects by serializing them comparing their equivalent byte-arrays.
     * <p/>
     * Condition: the comparable objects need to implement the Serializable-interface and have to be of same class.
     * 
     * @param bean1 first object
     * @param bean2 second object
     * @return true if both objects are equal
     */
    public static boolean equals(Object bean1, Object bean2) {
        if (bean1 == bean2) {
            return true;
        }
        if ((bean1 == null) || (bean2 == null)) {
            return false;
        }

        if (!(bean1 instanceof Serializable)) {
            return false;
        }
        if (!(bean2 instanceof Serializable)) {
            return false;
        }

        if (!bean1.getClass().equals(bean2.getClass())) {
            return false;
        }
        if (bean1.equals(bean2)) {
            return true;
        }

        return equals(convertToByteArray(bean1), convertToByteArray(bean2));
    }

    /**
     * Equals to serialized bean-object as byte-arrays.
     * 
     * @param bean1 first bean
     * @param bean2 second bean
     * @return true if both bean-objects are equal
     */
    public static boolean equals(byte[] bean1, byte[] bean2) {
        return Arrays.equals(bean1, bean2);
    }

    /**
     * copy serializing bean - doing a deep copy. may fail, if classloader (the current threads loader) is unable to
     * load nested classes.
     * <p/>
     * to copy only values, have a look at {@link BeanClass#copyValues(Object, Object, String...)} and
     * {@link #clone(Object)}.
     * 
     * @param <T> serializable bean
     * @return new instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T copy(T bean) {
        final byte[] ser = convertToByteArray(bean);
        return (T) convertToObject(ser, Thread.currentThread().getContextClassLoader());
    }

    /**
     * amount
     * 
     * @param c count of bytes
     * @return string representating the given amount of bytes
     */
    public static final String amount(long c) {
        return c > 1200000000 ? (c / (1024 * 1024 * 1024)) + "GB" : c > 1200000 ? (c / (1024 * 1024)) + "MB" : c > 1200
            ? (c / 1024) + "KB" : c + "b";
    }

    /**
     * getInputStream
     * 
     * @param data
     * @return all bytes packed into an input stream
     */
    public static final InputStream getInputStream(byte[] data) {
        return new ByteArrayInputStream(data);
    }

    /**
     * toByteArray
     * 
     * @param stream
     * @return all bytes of given stream
     */
    public static final byte[] toByteArray(InputStream stream) {
        try {
            return FileUtil.readBytes(stream);
        } catch (IOException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * toString
     * 
     * @param stream source
     * @param encoding (optional) if null, the system file.encoding will be used. e.g.: UTF-8
     * @return stream read into a string
     */
    public static final String toString(InputStream stream, String encoding) {
        return String
            .valueOf(FileUtil.getFileData(stream, encoding != null ? encoding : get("file.encoding", "UTF-8")));
    }

    /**
     * @return piped stream
     */
    public static PrintStream getPipe() {
        return getPipe(new PipedInputStream());
    }

    /**
     * @param in input stream
     * @return piped stream
     */
    public static PrintStream getPipe(PipedInputStream in) {
        try {
            return new PrintStream(new PipedOutputStream(in));
        } catch (IOException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * reads the full input stream and calls the given tranformation action with each byte array of length bufferlength.
     * <p/>
     * Tip: to have a result for the caller of this function, your action could write to a print stream that can be
     * piped and read from the connected piped input stream. the foreach function doesn't return a value because to
     * avoid memory problems.
     * 
     * @param stream to read
     * @param bufferlength byte array length that will be read per loop and sent to the given ation
     * @param action action to be done for each byte array.
     */
    public static void forEach(InputStream stream, int bufferlength, final IRunnable<Object, byte[]> action) {
        byte[] buffer = new byte[bufferlength];
        try {
            while (stream.available() > 0) {
                stream.read(buffer);
                action.run(buffer);
            }
        } catch (IOException e) {
            ManagedException.forward(e);
        }
    }

    public static byte[] fromHex(String hex) {
        return stringToBytes(hex, 16);
    }

    public static byte[] stringToBytes(String txt, int base) {
        byte[] buf = new byte[txt.length() / 2];
        for (int i = 0; i < txt.length(); i += 2) {
            buf[i / 2] = (byte) Integer.parseInt(txt.substring(i, i + 2), base);
        }
        return buf;
    }

}
