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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;

/**
 * some utils for byte-arrays
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
        return type.equals(Serializable.class)
            || (type.isArray() && (type.isAssignableFrom(Byte[].class) || type.isAssignableFrom(byte[].class)));
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

    /**
     * deserialization of a byte-array
     * 
     * @param bytes to deserialize
     * @return deserialized bean
     */
    protected static Object convertToObject(byte[] bytes, final ClassLoader classLoader) {
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

}