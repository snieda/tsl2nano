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

import de.tsl2.nano.collection.CollectionUtil;
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
@SuppressWarnings("unchecked")
public class LogicTable<H extends Format & Comparable<H>, ID> extends TableList<H, ID> {
    public static final String ERROR = "ERROR:";

    /** serialVersionUID */
    private static final long serialVersionUID = -7404922627823743263L;

    String regexpHeader = "[A-Z]+";
    transient Map<String, Object> valueMap;

    /**
     * constructor
     * 
     * @param cols
     * @param rows
     */
    public LogicTable(String name, int cols, int rows) {
        this(name, cols);
        fill(rows);
        valueMap = createValueMap();
    }

    /**
     * constructor
     * 
     * @param header
     */
    public LogicTable(H... header) {
        super(header);
    }

    public LogicTable(Object... header) {
        super((H[]) CollectionUtil.copyOfRange(header, 0, header.length, DefaultHeader[].class));
    }

    /**
     * see {@link TableList#TableList(Class, int)}
     */
    public LogicTable(String name, int columnCount) {
        super((H[]) createStandardHeader(columnCount));
        this.name = name;
        valueMap = createValueMap();
    }

    public Map<String, Object> getValueMap() {
        if (valueMap == null)
            valueMap = createValueMap();
        return valueMap;
    }

    protected static Object createDefaultHeader(Object source) {
        if (source instanceof Integer)
            return new DefaultHeader((Integer) source);
        else
            return new DefaultHeader(source.toString());
    }

    /**
     * createStandardHeader
     * 
     * @param columnCount
     * @return array of DefaultHeader (downcasted to Object[] to be compilable on standard-jdk)
     */
    private static final Object[] createStandardHeader(int columnCount) {
        DefaultHeader[] h = new DefaultHeader[columnCount];
        for (int i = 0; i < columnCount; i++) {
            h[i] = new DefaultHeader(i);
        }
        return h;
    }

    public boolean isFormula(Object cell) {
        return String.valueOf(cell).startsWith(EquationSolver.EQUATION);
    }

    /**
     * sets a new value through given column/row-id as strings
     * 
     * @param row row-id to be found through this string
     * @param column header-column to be found through this string
     * @param value value to set
     */
    public void set(String row, String column, Object value) {
        set(findRow(row), findHeader(column), value);
    }

    /**
     * gets a value through given column/row-id as strings
     * 
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
        if (row == -1)
            if (column == -1)
                return CELL_ROOT;
            else
                return String.valueOf(getHeader()[column]);
        else if (column == -1)
            return getRowID(row);
        Object e = super.get(row, column);
        if (e instanceof String) {
            String expression = (String) e;
            if (expression.startsWith(EquationSolver.EQUATION) || expression.startsWith(EquationSolver.ACTION)) {
                try {
                    EquationSolver solver = new EquationSolver(null, getValueMap());
                    return solver.eval(expression.substring(1));
                } catch (Exception ex) {
                    return ERROR + ex.toString() + "<<<" + e;
                }
            }
        }
        return e;
    }

    private H findHeader(String expression) {
        for (int i = 0; i < header.length; i++) {
            String h = header[i].toString();
            h = StringUtil.extract(h, regexpHeader);
            if (h.equals(expression)) {
                return header[i];
            }
        }
        throw new IllegalArgumentException("no header entry found for: " + expression);
    }

    private ID findRow(String expression) {
        for (int i = 0; i < rows.size(); i++) {
            ID r = getRowID(i);
            String rt = r.toString();
            if (rt.equals(expression)) {
                return r;
            }
        }
        throw new IllegalArgumentException("no rowID entry found for: " + expression);
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
                return size() == 0;
            }

            @Override
            public boolean containsKey(Object key) {
                return get(key) != null;
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
                    System.err.println(ex); //TODO print to log
                    return null;
                }
            }

            @Override
            public Object put(String key, Object value) {
                TEntry<H, ID> e = transform(key);
                return _this.set(e.value, e.key, String.valueOf(value));
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

    public String createKey(int row, int col) {
        if (row < 0)
            if (col < 0)
                return String.valueOf(0) + String.valueOf(0);
            else
                return getHeader()[col] + String.valueOf(0);
        if (col < 0)
            return String.valueOf(0) + getRowID(row);
        return getHeader()[col].toString() + getRowID(row);
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
        ID row = rowKey.equals("0") ? createRowID(0) : findRow(rowKey);
        return new TEntry<H, ID>(column, row);
    }

//    private ID getHeaderRow() {
//        return (ID) createRow(-1, getHeader());
//    }

    public void doOnValues(ICellVisitor visitor) {
        for (int i = -1; i < rows.size(); i++) {
            for (int j = -1; j < header.length; j++) {
                visitor.visit(i, j, i < 0 || j < 0 ? get(i, j) : super.get(i, j)); //not-calculated
            }
        }
    }

    @Override
    protected void dumpValues(final StringBuilder buf, final String div, boolean resolve) {
        if (resolve) {
            doOnValues(new ICellVisitor() {
                @Override
                public void visit(int row, int col, Object cell) {
                    if (row == -1) // header was already printed
                        return;
                    buf.append(cell + div);
                    if (col == getColumnCount() - 1)
                        buf.append(LF);
                }
            });
        } else {//the super-class knows only cell-content
            super.dumpValues(buf, DIV, resolve);
        }
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
