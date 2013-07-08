/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, www.idv-ag.de
 * created on: Apr 20, 2010
 * 
 * Copyright: (c) Thomas Schneider, www.idv-ag.de 2010, all rights reserved
 */
package de.tsl2.nano.historize;

import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import de.tsl2.nano.exception.FormattedException;

/**
 * Generic class to provide historized user input - perhaps for a text-field. Can be used for comboboxes to configure
 * their items.
 * 
 * <pre>
 * Use: 
 * - call the create method 
 * - use instance(name) with the created item name to get the created historized-input 
 * - use getAsList() to obtain the entries - use addAndSave(value) to add a new entry.
 * </pre>
 * 
 * @author Thomas Schneider, www.idv-ag.de
 * @version $Revision$
 */
@SuppressWarnings("unchecked")
public class HistorizedInputFactory extends Properties {
    /** serialVersionUID */
    private static final long serialVersionUID = -5485485250948235886L;
    private static Map<String, HistorizedInput> factoryMap = new Hashtable<String, HistorizedInput>();
    private static String path = "temp/";

    /**
     * sets standard file path for historized inputs
     * 
     * @param path file path
     */
    public static void setPath(String path) {
        HistorizedInputFactory.path = path;
    }

    /**
     * instance
     * 
     * @param name
     * @return
     */
    public static final HistorizedInput instance(String name) {
        final HistorizedInput historizedInput = factoryMap.get(name);
        if (historizedInput == null) {
            throw new FormattedException("please call the create-method before instance");
        }
        return historizedInput;
    }

    /**
     * checks, whether an {@link HistorizedInput} was already created
     * 
     * @param name name of {@link HistorizedInput}
     * @return true, if {@link HistorizedInput} was already created
     */
    public static final boolean exists(String name) {
        return factoryMap.get(name) != null;
    }

    /**
     * delegator using type string.
     * 
     * @see #create(String, int, Class)
     */
    public static final HistorizedInput<String> create(String name) {
        return create(name, 500, String.class);
    }

    /**
     * create new historized input with max-count entries of given type.
     * 
     * @param <T> entry type used in #getAsList()
     * @param name historized input name (unique id)
     * @param maxCount maximum amount of entries to store
     * @param type wrapper to put the entries into (must have a string-constructor)
     * @return new historized input map.
     */
    public static final <T> HistorizedInput<T> create(String name, int maxCount, Class<T> type) {
        HistorizedInput<T> historizedInput = factoryMap.get(name);
        if (historizedInput == null) {
            historizedInput = new HistorizedInput(path, name, maxCount, type);
            factoryMap.put(name, historizedInput);
        }
        return historizedInput;
    }

    /**
     * deletes the given input history
     * 
     * @param name history name
     * @return true, if file was deleted
     */
    public static final boolean delete(String name) {
        final HistorizedInput<?> historizedInput = factoryMap.remove(name);
        if (historizedInput == null) {
            throw new FormattedException("swartifex.implementationerror", new Object[] { name });
        }
        return historizedInput.delete();
    }

    /**
     * deletes all histories from file system.
     * 
     * @return true, if all files could be deleted.
     */
    public static final boolean deleteAll() {
        final Set<String> histories = factoryMap.keySet();
        boolean result = true;
        for (final String name : histories) {
            result &= delete(name);
        }
        return result;
    }
}
