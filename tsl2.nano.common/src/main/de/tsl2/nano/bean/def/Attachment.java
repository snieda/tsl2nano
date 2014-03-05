/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 27.02.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.lang.reflect.Method;

import org.simpleframework.xml.Attribute;

import de.tsl2.nano.bean.IAttribute;
import de.tsl2.nano.messaging.EventController;
import de.tsl2.nano.util.FileUtil;

/**
 * byte[]-value from file-system.
 * 
 * @author Tom
 * @version $Revision$
 */
public class Attachment implements IValueAccess<byte[]>, IAttribute<byte[]> {
    /** serialVersionUID */
    private static final long serialVersionUID = -1460468414949211876L;

    transient byte[] data;
    @Attribute
    String name;
    @Attribute
    String file;
    transient EventController eventController;

    public Attachment() {
    }
    
    /**
     * constructor
     * 
     * @param object
     * @param type
     */
    public Attachment(String name, String file) {
        this.name = name;
        this.file = file;
    }

    @Override
    public int compareTo(IAttribute<byte[]> o) {
        return getId().compareTo(o.getId());
    }

    @Override
    public Class getDeclaringClass() {
        return Object[].class;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public byte[] getValue(Object instance) {
        if (data == null)
            data = FileUtil.getFileBytes(file, null);
        return data;
    }

    @Override
    public void setValue(Object instance, byte[] value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getId() {
        return getType().getSimpleName() + "." + name;
    }

    @Override
    public boolean hasWriteAccess() {
        return false;
    }

    @Override
    public Method getAccessMethod() {
        return null;
    }

    @Override
    public boolean isVirtual() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        return hashCode() == obj.hashCode();
    }

    @Override
    public String toString() {
        return getId();
    }

    @Override
    public byte[] getValue() {
        return getValue(null);
    }

    @Override
    public void setValue(byte[] value) {
        setValue(null, value);
    }

    @Override
    public Class<byte[]> getType() {
        return byte[].class;
    }

    @Override
    public EventController changeHandler() {
        if (eventController == null)
            eventController = new EventController();
        return eventController;
    }
}
