/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Nov 28, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.logictable;

import java.text.Format;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import de.tsl2.nano.collection.TableList;
import de.tsl2.nano.core.util.StringUtil;

/**
 * An Extension of {@link TableList} providing logic expressions to be filled into cells like in excel.
 * <p/>
 * Header-columns and row-ids are flexible but as default like in excel.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class LogicTable<H extends Format & Comparable<H>, ID> extends TableList<H, ID> {
    String regexpHeader = "[A-Z]+";
    Map<String, Object> valueMap;

    /**
     * see {@link TableList#TableList(Class, int)}
     */
    public LogicTable(int columnCount) {
        super((H[]) createStandardHeader(columnCount));
        valueMap = createValueMap();
    }

    /**
     * createStandardHeader
     * @param columnCount
     * @return array of DefaultHeader (downcasted to Object[] to be compilable on standard-jdk)
     */
    private static final Object[] createStandardHeader(int columnCount) {
        DefaultHeader[] h = new DefaultHeader[columnCount];
        for (int i = 0; i < columnCount; i++) {
            h[i] = new DefaultHeader(i, columnCount);
        }
        return h;
    }

    /**
     * sets a new value through given column/row-id as strings
     * @param row row-id to be found through this string
     * @param column header-column to be found through this string
     * @param value value to set
     */
    public void set(String row, String column, Object value) {
        set(findRow(row), findHeader(column), value);
    }

    /**
     * gets a value through given column/row-id as strings
     * @param row row-id to be found through this string
     * @param column header-column to be found through this string
     * @return cell value
     */
    public Object get(String row, String column) {
        return get(findRow(row), findHeader(column));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(int row, int column) {
        Object e = super.get(row, column);
        if (e instanceof String) {
            String expression = (String) e;
            if (expression.startsWith(EquationSolver.EQUATION)) {
                try {
                    EquationSolver solver = new EquationSolver(null, valueMap);
                    return solver.eval(expression.substring(1));
                } catch (Exception ex) {
                    return e;
                }
            }
        }
        return e;
    }

    private H findHeader(String expression) {
        for (int i = 0; i < header.length; i++) {
            String h = header[i].toString();
            h = StringUtil.extract(h, regexpHeader);
            if (h.equals(expression))
                return header[i];
        }
        throw new IllegalArgumentException("no header entry found for: " + expression);
    }

    private ID findRow(String expression) {
        for (int i = 0; i < rows.size(); i++) {
            ID r = getRowID(i);
            String rt = r.toString();
            if (rt.equals(expression))
                return r;
        }
        throw new IllegalArgumentException("no header entry found for: " + expression);
    }

    /**
     * create a simple readable map, transforming a key to header/row information
     * 
     * @return new created readable map
     */
    public Map<String, Object> createValueMap() {
        final TableList<H, ID> _this = this;
        return new Map<String, Object>() {
            @Override
            public int size() {
                return _this.size() * getColumnCount();
            }

            @Override
            public boolean isEmpty() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean containsKey(Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean containsValue(Object value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Object get(Object key) {
                try {
                    TEntry<H, ID> e = transform(key);
                    return _this.get(e.value, e.key);
                } catch (Exception ex) {
                    //TODO: log error
                    return null;
                }
            }

            @Override
            public Object put(String key, Object value) {
                TEntry<H, ID> e = transform(key);
                return _this.set(e.value, e.key, value);
            }

            @Override
            public Object remove(Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void putAll(Map<? extends String, ? extends Object> m) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<String> keySet() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Collection<Object> values() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<java.util.Map.Entry<String, Object>> entrySet() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * transforms a flat string into header and row info to address a table-cell
     * 
     * @param key flat string, concatenating a header-column with a row-id (like: 'A1')
     * @return separated header-column and row-id
     */
    protected TEntry<H, ID> transform(Object key) {
        String k = (String) key;
        H column = findHeader(StringUtil.extract(k, regexpHeader));
        String rowKey = StringUtil.substring(k, column.toString(), null);
        ID row = findRow(rowKey);
        return new TEntry<H, ID>(column, row);
    }
}

/**
 * used to transform a flat string into header and row info to address a table-cell
 * 
 * @param <K>
 * @param <V>
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
class TEntry<K, V> {
    K key;
    V value;

    TEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }
}
