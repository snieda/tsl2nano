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

import java.io.File;
import java.lang.reflect.Method;
import java.util.UUID;

import org.simpleframework.xml.Attribute;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.cls.IValueAccess;
import de.tsl2.nano.core.messaging.EventController;
import de.tsl2.nano.core.util.BitUtil;
import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * byte[]-value from file-system.
 * 
 * @author Tom
 * @version $Revision$
 */
public class Attachment implements IValueAccess<byte[]>, IAttribute<byte[]> {
    /** serialVersionUID */
    private static final long serialVersionUID = -1460468414949211876L;

    /** attachment data */
    transient byte[] data;
    /** attribute name */
    @Attribute
    String name;
    /** full file path (including the extension, that may be used for presentation aspects) */
    @Attribute
    String file;
    transient EventController eventController;

    private static final String ICON_EMPTY = "icons/blocked.png";

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
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public byte[] getValue(Object instance) {
        if (data == null && file != null) {
            data = FileUtil.getFileBytes(file, null);
        }
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
        if (eventController == null) {
            eventController = new EventController();
        }
        return eventController;
    }

    /**
     * should be used, if the bean attribute stores the content of the attachment
     * @param id attribute id
     * @param source attachment content
     * @return attachment content
     */
    public static byte[] getFileBytes(String id, Object source) {
        return FileUtil.getFileBytes(getValueFile(id, source).getPath(), null);
    }

    /**
     * should be used, if the bean attribute stores only the file name of the attachment
     * @param instance bean values instance
     * @param attribute attribute name
     * @param file file name
     * @return attachment content
     */
    public static byte[] getFileBytes(Object instance, String attribute, String file) {
        return FileUtil.getFileBytes(getFilename(instance, attribute, file), null);
    }

    public static String getFilename(Object instance, String attribute, String name, boolean trailOnPath) {
    	String filename = getFilename(instance, attribute, name);
    	if (new File(filename).exists()) {
    		return filename;
    	} else {
    		name = FileUtil.replaceToJavaSeparator(name);
    		return getFilename(instance, attribute, StringUtil.substring(name, "/", null, true));
    	} 
    }
    
    /**
     * getAttachmentFilename
     * 
     * @param instance
     * @param attribute
     * @param name
     * @return
     */
    public static String getFilename(Object instance, String attribute, String name) {
        return ENV.getTempPathRel()
            + FileUtil.getValidFileName(BeanValue.getBeanValue(instance, attribute).getValueId() + "."
                + Util.asString(name));
    }

    /**
     * can be used, if for example a bean value is a byte-array to be used from outside (perhaps on an html-page loading
     * an image). if the value is not a byte-array, it will be created through serialization. this byte-array will be
     * saved (if not saved before) to a file inside a temp-directory of your environment.
     * @param id attribute id (bean-name+attribute-name)
     * @param v byte object (of bean value) to be saved and transferred
     * @return temporary file-path of the current bean-value, saved as byte-array.
     */
    public static File getValueFile(String id, Object v) {
        if (v == null)
            return null;
        byte[] data = ByteUtil.getBytes(v);
        String fname = data != null ? id + "-" + UUID.nameUUIDFromBytes(data) : ICON_EMPTY;
        String ext = getExtension(data);
        /*
         * writing to the servers temp path needs a path, starting from 'user.dir' (--> application start path).
         * transferring a file as source to the client to be shown inside the html page,
         * we need the relative path starting from the html page.
         * Example:
         * servers temp path: myappconfigpath/temp/
         * html source path : /temp/
         */
        File file = new File(ENV.getTempPathRel() + fname + ext);
        if (!file.exists() && data != null) {
            FileUtil.writeBytes(data, file.getPath(), false);
        }
        return new File(ENV.getTempPathURL() + fname + ext);
    }

    /**
     * evaluate the data type of the given byte stream. it's only a simple workaround for svg and pdf files. some
     * browsers (like Chrome) need a file extension to show an svg file on an img-tag.
     * 
     * @param data
     * @return file extension
     */
    private static String getExtension(byte[] data) {
        if (data == null) {
            return "";
        }
        String str = new String(data);
        if (str.startsWith("%PDF")) {
            return ".pdf";
        } else if (str.contains("<svg")) {
            return ".svg";
        } else {
            return "";
        }
    }

    /**
     * isAttachment
     * 
     * @param a an attribute
     * @return true, if presentable type is {@link IPresentable#TYPE_ATTACHMENT} and the attributes value type is
     *         byte[].
     */
    public static final boolean isAttachment(IAttributeDefinition<?> a) {
        int ptype = a.getPresentation() != null ? a.getPresentation().getType() : -1;
        return BitUtil.hasBit(ptype, IPresentable.TYPE_ATTACHMENT)
            && isData(a);
    }

    public static final boolean isData(IAttributeDefinition<?> a) {
        return ByteUtil.isByteStream(a.getType());
    }
}
