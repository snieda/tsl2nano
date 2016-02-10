/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 09.02.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.scanner;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.MatchResult;

import de.tsl2.nano.collection.FloatArray;
import de.tsl2.nano.core.util.FileUtil;

/**
 * reads any fields from any text file using a {@link Scanner}. all lines starting with {@link #COMMENT} will be
 * ignored.
 * 
 * <pre>
 * Features:
 * - key/value properties.
 * - any field-structure per line
 * </pre>
 * 
 * you can combine these features to read complex data structures.
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings("rawtypes")
public class FieldReader {
    private static final String COMMENT = "#";
    public static final String HEADER = "header";
    public static final String DEL_KEYVALUE = "\\s*=:\\s*";
    public static final String DEL_PROP = "\\s*[\r\n;]+";
    public static final String DEL_CSV = "\\s+";

    /*
     * READING TABLES
     */

    /**
     * delegates to {@link #readTable(InputStream, Class...)}
     */
    public static Map<Object, List> readTable(String file, Class... columnTypes) {
        return readTable(FileUtil.getFile(file), columnTypes);
    }

    /**
     * delegates to {@link #readTable(InputStream, String, Locale, boolean, boolean, boolean, Class...)} for simple
     * tables without header and rowIDs.
     */
    public static Map<Object, List> readTable(InputStream stream, Class... columnTypes) {
        return readTable(stream, DEL_CSV, Locale.getDefault(), true, false, false, columnTypes);
    }

    /**
     * reads values in columns and rows from any file using a {@link Scanner}.
     * 
     * @param stream input stream
     * @param fieldDelimiter delimiter between two values. normally {@link #DEL_CSV}.
     * @param locale locale to format formats etc. normally the default locale {@link Locale#getDefault()}.
     * @param horizontal if false, the map will be rotated to provide columns instead of rows
     * @param header if true, the first line will be read as header information
     * @param rowIDs if true, all first column values will be read as row-id objects - otherwise a line counter will be
     *            used.
     * @param columnTypes indicates the column value types. known Scanner types are: byte, boolean, short, int, long,
     *            float, double, BigInteger, BigDecimal. the given locale defines the format of that types.
     * @return map holding its keys in the sequentially order. an optional header is stored on key {@link #HEADER}. all
     *         other keys are rowIDs or line indexes.
     */
    public static Map<Object, List> readTable(InputStream stream,
            String fieldDelimiter,
            Locale locale,
            boolean horizontal,
            boolean header,
            boolean rowIDs,
            Class... columnTypes) {
        Map<Object, List> table = new LinkedHashMap<Object, List>();
        Scanner sc = new Scanner(stream);
        try {
            if (header) {
                String line = nextLine(sc);
                if (line != null)
                    table.put(HEADER, Arrays.asList(line.split(DEL_CSV)));
            }
            Object rowId;
            int i = 0;
            while (sc.hasNext()) {
                rowId = rowIDs ? sc.next() : ++i;
                List row = readRow(nextLine(sc), fieldDelimiter, locale, columnTypes);
                if (row == null)
                    break;
                table.put(rowId, row);
            }
        } finally {
            sc.close();
        }
        return horizontal ? table : rotate(table);
    }

    /**
     * delegates to {@link #readTable(InputStream, boolean, boolean, boolean, String)} for a simple table without header
     * and row-ids.
     */
    public static Map<Object, List> readTable(InputStream stream, String rowPattern) {
        return readTable(stream, true, false, false, rowPattern);
    }

    /**
     * reads values in columns and rows from any file using a {@link Scanner}. giving a rows regular expression defines
     * the column formats for each row. see {@link Scanner} for examples. the row pattern should have grouping with
     * brackets like '(\w+) fish (\w+) fish (\w+)'.
     * 
     * @param stream input stream
     * @param fieldDelimiter delimiter between two values. normally {@link #DEL_CSV}.
     * @param locale locale to format formats etc. normally the default locale {@link Locale#getDefault()}.
     * @param horizontal if false, the map will be rotated to provide columns instead of rows
     * @param header if true, the first line will be read as header information
     * @param rowIDs if true, all first column values will be read as row-id objects - otherwise a line counter will be
     *            used.
     * @param rowPattern
     * @return map holding its keys in the sequentially order. an optional header is stored on key {@link #HEADER}. all
     *         other keys are rowIDs or line indexes.
     */
    public static Map<Object, List> readTable(InputStream stream,
            boolean horizontal,
            boolean header,
            boolean rowIDs,
            String rowPattern) {
        Map<Object, List> table = new LinkedHashMap<Object, List>();
        Scanner sc = new Scanner(stream);
        try {
            if (header) {
                String line = nextLine(sc);
                if (line != null)
                    table.put(HEADER, Arrays.asList(line.split(DEL_CSV)));
            }
            Object rowId;
            int i = 0;
            while (sc.hasNext()) {
                rowId = rowIDs ? sc.next() : ++i;
                List row = readFormattedRow(nextLine(sc), rowPattern);
                if (row == null)
                    break;
                table.put(rowId, row);
            }
        } finally {
            sc.close();
        }
        return horizontal ? table : rotate(table);
    }

    private static List readFormattedRow(String line, String rowPattern) {
        if (line == null)
            return null;
        List row = new LinkedList();
        Scanner sc = new Scanner(line);
        line = sc.findInLine(rowPattern);
        MatchResult mr = sc.match();
        for (int i = 0; i < sc.match().groupCount(); i++) {
            row.add(mr.group(i));
        }
//        sc.close();
        return row;
    }

    /**
     * returns the next line that is no {@link #COMMENT}.
     * 
     * @param sc current scanner
     * @return readable line
     */
    private static String nextLine(Scanner sc) {
        String line = null;
        while (sc.hasNextLine()) {
            line = sc.nextLine();
            if (!line.trim().startsWith(COMMENT))
                break;
        }
        return line;
    }

    /**
     * rotates the given table to provide list of column values instead of rows.
     * 
     * @param table to be rotated
     * @return map holding lists of columns
     */
    private static Map<Object, List> rotate(Map<Object, List> table) {
        Map<Object, List> rot = new LinkedHashMap<Object, List>();
        if (table.containsKey(HEADER))
            rot.put(HEADER, table.get(HEADER));
        int length = table.values().iterator().next().size();
        Set<Object> keys = table.keySet();
        List row;
        for (int i = 0; i < length; i++) {
            List col = new ArrayList(keys.size());
            Object name = null;
            for (Object k : keys) {
                if (k.equals(HEADER)) {
                    name = table.get(k).get(i);
                    continue;
                }
                row = table.get(k);
                if (row.size() > i)
                    col.add(row.get(i));
            }
            rot.put(name != null ? name : ++i, col);
        }
        return rot;
    }

    /**
     * reads a row of values for the given column types. if columnTypes is only one array type (like float[].class), the
     * row will be read until end of line - with unlimited count of values.
     * 
     * @param line text to be splitted into values of given columnTypes
     * @param fieldDelimiter delimiter between two values. normally {@link #DEL_CSV}.
     * @param locale locale to format formats etc. normally the default locale {@link Locale#getDefault()}.
     * @param columnTypes indicates the column value types. known Scanner types are: byte, boolean, short, int, long,
     *            float, double, BigInteger, BigDecimal. the given locale defines the format of that types.
     * @return list holding all values of given types
     */
    static List readRow(String line, String fieldDelimiter, Locale locale, Class... columnTypes) {
        if (line == null)
            return null;
        Scanner sc = new Scanner(line);
        sc.useDelimiter(fieldDelimiter);
        sc.useLocale(locale);
        List row;
        if (columnTypes.length == 1 && columnTypes[0].isArray()) {
//            boolean isFloatArray = columnTypes[0].equals(float[].class);
            row = /*isFloatArray ? new FloatArray(columnTypes.length, columnTypes.length) : */new ArrayList(columnTypes.length);
            Class type = columnTypes[0].getComponentType();
            while(sc.hasNext()) {
                row.add(next(sc, type));
            }
        } else {
            row = new ArrayList(columnTypes.length);
            for (int i = 0; i < columnTypes.length; i++) {
                row.add(sc.hasNext() ? next(sc, columnTypes[i]) : null);
            }
        }
//        sc.close();
        return row;
    }

    /**
     * reads the next value as given type
     * 
     * @param sc scanner holding values
     * @param type type to read
     * @return value of given type
     */
    private static Object next(Scanner sc, Class type) {
        if (type.equals(byte.class))
            return sc.nextByte();
        if (type.equals(boolean.class))
            return sc.nextBoolean();
        else if (type.equals(short.class))
            return sc.nextShort();
        else if (type.equals(int.class))
            return sc.nextInt();
        else if (type.equals(long.class))
            return sc.nextLong();
        else if (type.equals(float.class))
            return sc.nextFloat();
        else if (type.equals(double.class))
            return sc.nextDouble();
        else if (type.equals(BigInteger.class))
            return sc.nextBigInteger();
        else if (type.equals(BigDecimal.class))
            return sc.nextBigDecimal();
        else
            return sc.next();
    }

    /*
     * READING PROPERTIES
     */
    /**
     * delegates to {@link #read(InputStream)}
     */
    public static Properties read(String file) {
        return read(FileUtil.getFile(file), true);
    }

    /**
     * delegates to {@link #read(InputStream, String, String, Locale)}
     */
    public static Properties read(InputStream stream, boolean doClose) {
        return read(stream, DEL_KEYVALUE, DEL_PROP, Locale.getDefault(), doClose);
    }

    /**
     * reads key/value properties from any file using a {@link Scanner}.
     * 
     * @param stream input stream
     * @param keyValueDelimiter delimiter between key and value. normally {@link #DEL_KEYVALUE}.
     * @param propDelimiter delimiter between two property entries. normally {@link #DEL_PROP}
     * @param locale locale to format formats etc. normally the default locale {@link Locale#getDefault()}.
     * @return property map
     */
    public static Properties read(InputStream stream,
            String keyValueDelimiter,
            String propDelimiter,
            Locale locale,
            boolean doClose) {
        Properties p = new Properties();
        Scanner sc = new Scanner(stream);
        try {
            sc.useDelimiter(propDelimiter);
            while (sc.hasNext()) {
                KeyValue kv = extract(nextLine(sc), keyValueDelimiter, locale);
                if (kv == null)
                    break;
                p.put(kv.key, kv.val);
            }
        } finally {
            if (doClose)
                sc.close();
        }
        return p;
    }

    /**
     * delegates to {@link #extract(String, String, Locale)}
     */
    static KeyValue extract(String line) {
        return extract(line, DEL_KEYVALUE, Locale.getDefault());
    }

    /**
     * extract exactly one key-value-pair from given line.
     * 
     * @param line text to extract a key-value-pair from
     * @param keyValueDelimiter delimiter between key and value. normally {@link #DEL_KEYVALUE}.
     * @param locale locale to format formats etc. normally the default locale {@link Locale#getDefault()}.
     * @return one {@link KeyValue}
     */
    static KeyValue extract(String line, String keyValueDelimiter, Locale locale) {
        Scanner sc = new Scanner(line);
        sc.useDelimiter(keyValueDelimiter);
        sc.useLocale(locale);
        KeyValue kv = null;
        if (sc.hasNext()) {
            kv = new KeyValue();
            kv.key = sc.next();
            if (sc.hasNext())
                kv.val = sc.next();
            else
                kv = null;
        }
        sc.close();
        return kv;
    }

}

/**
 * struct holding a key and a value
 * 
 * @author Tom
 * @version $Revision$
 */
class KeyValue {
    String key;
    String val;
}