/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Apr 22, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.historize;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Properties;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.util.FileUtil;

/**
 * class to handle the historized entries.
 * 
 * @param <T>
 * @author Thomas Schneider
 * @version $Revision$
 */
public class HistorizedInput<T> extends Properties {
    /** serialVersionUID */
    private static final long serialVersionUID = -7825644997402507863L;
    File file;
    Class<T> itemType;
    int maxCount;
    LinkedHashSet<T> list;

    private static final String KEY_NAME_PREFIX = "entry";

    /**
     * constructor
     * 
     * @param path file path
     * @param name historizing name - will be used as file name, too
     * @param maxCount maximum count of historized items.
     * @param itemType wrapper class to store item into. must have a string constructor!
     */
    HistorizedInput(String path, String name, int maxCount, Class<T> itemType) {
        super();
        this.file = new File(path + FileUtil.getValidFileName(name) + ".hist");
        this.itemType = itemType;
        this.maxCount = maxCount;
        if (file.exists()) {
            try {
                load(new FileReader(file));
            } catch (final Exception e) {
                ManagedException.forward(e);
            }
        }
    }

    /**
     * property values
     * 
     * @return stored entries as simple string values.
     */
    public Collection<Object> getPropertyValues() {
        return values();
    }

    /**
     * getAsList
     * 
     * @return stored entries in an ordered list of the given type.
     */
    public Collection<T> getAsInstanceList() {
        if (list == null) {
            list = new LinkedHashSet<T>(size());
            for (int i = 0; i < maxCount; i++) {
                final String value = getProperty(KEY_NAME_PREFIX + i);
                if (value != null) {
                    try {
                        list.add(itemType.getConstructor(new Class[] { String.class })
                            .newInstance(new Object[] { value }));
                    } catch (final Exception e) {
                        ManagedException.forward(e);
                    }
                } else {
                    //--> no more elements
                    break;
                }
            }
        }
        return list;
    }

    /**
     * adds the new entry to the list of historized entries and saves the property-file.
     * 
     * @param newEntry new entry to historize
     */
    public void addAndSave(String newEntry) {
        if (newEntry == null || newEntry.length() == 0) {
            return;
        }
        if (values().contains(newEntry)) {
            //TODO: re-order properties!
//            setToFirstEntry(newEntry);
            return;
        }
        for (int i = 0; i < maxCount; i++) {
            final String value = getProperty(KEY_NAME_PREFIX + i);
            /*
             * search for the first empty field. if maxcount is reached, shift all entries one less.
             */
            if (value != null) {
                if (i == maxCount - 1) {
                    for (int j = 0; j < maxCount - 1; j++) {
                        setProperty(KEY_NAME_PREFIX + j, getProperty(KEY_NAME_PREFIX + (j + 1)));
                    }
                } else {
                    continue;
                }
            }
            setProperty(KEY_NAME_PREFIX + i, newEntry);
            break;
        }
        try {
            list = null;
            if (!file.exists()) {
                FileUtil.createPath(file.getPath());
//                if (!file.createNewFile())
//                    new ManagedException("couldn't store the given file");
            }
            store(new FileWriter(file), "Created by class HistorizedInput (NanoFix)");
        } catch (final Exception e) {
            ManagedException.forward(e);
        }
    }

    /**
     * deletes history input from file system
     */
    public boolean delete() {
        return file.delete();
    }
}
