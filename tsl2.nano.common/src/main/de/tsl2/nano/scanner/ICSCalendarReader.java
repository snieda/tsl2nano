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
    static final String BLOCK = "BEGIN[:]VEVENT.*END[:]VEVENT";
    static final String PRE_START = "DTSTART";
    static final String PRE_END = "DTEND";
    static final String SUMMARY = "SUMMARY";
    static final String CATEGORY = "CATEGORY";
    static final String CLASS = "CLASS";
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
                Object v;
                for (Object k : keys) {
                    //replace date strings with date objects
                    if (k.toString().startsWith(PRE_START) || k.toString().startsWith(PRE_END)) {
                        passInfo.put(k, getDate(passInfo, k));
                    }
                }
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
