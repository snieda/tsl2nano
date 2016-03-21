/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 12.03.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.scanner;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.tsl2.nano.core.ICallback;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.util.FileUtil;

/**
 * Provides a stream of calendar entries to be consumed by a callback.
 * 
 * @author Tom
 * @version $Revision$
 */
public class ICSCalendarReader {
    static final String BLOCK = "(?m)BEGIN[:]VEVENT.*END[:]VEVENT";
    public static final String START = "DTSTART";
    public static final String END = "DTEND";
    public static final String SUMMARY = "SUMMARY";
    public static final String CATEGORY = "CATEGORIES";
    public static final String CLASS = "CLASS";
    static final SimpleDateFormat DF = new SimpleDateFormat("yyyyMMdd");
    static final SimpleDateFormat TF = new SimpleDateFormat("HHmmss");

    /**
     * forEach
     * 
     * @param icsFile
     * @param callback
     * @return loop count
     */
    public static long forEach(String icsFile, final ICallback callback) {
        return FieldReader.forEach(FileUtil.getFile(icsFile), BLOCK, ":", new ICallback() {
            @Override
            public Object run(Map<Object, Object> passInfo) {
                Set<Object> keys = passInfo.keySet();
                Map<Object, Object> dateObjects = new HashMap<Object, Object>();
                Object v;
                for (Object k : keys) {
                    //replace date strings with date objects
                    if (k.toString().startsWith(START)) {
                        dateObjects.put(START, getDate(passInfo, k));
                    }else if (k.toString().startsWith(END)) {
                        dateObjects.put(END, getDate(passInfo, k));
                    }
                }
                passInfo.putAll(dateObjects);
                return callback.run(passInfo);
            }

            private Object getDate(Map<Object, Object> passInfo, Object k) {
                try {
                    return k.toString().contains("DATE") ? DF.parse((String) passInfo.get(k))
                        : TF.parse((String) passInfo.get(k));
                } catch (ParseException e) {
                    ManagedException.forward(e);
                    return null;
                }
            }
        });
    }
}
