/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Nov 19, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.collection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.logging.Log;
import org.xml.sax.helpers.DefaultHandler;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.CollectionUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;

/**
 * Defines a table of values. A header defines the columns. The header array shouldn't hold any null values! All rows
 * hold an index, a row-identifier and fixed sized array of values. The values are not typed, as it is possible to have
 * different types in one row. Overwrite this class, if you have a static value type per column.
 * <p/>
 * To work properly, your row-id type must implement the equals(Object) method correctly (see {@link #indexOf(Object)}.
 * 
 * @param <H> column-header type. to use the binarySearch of Arrays, H must implement Comparable
 * @param <ID> row identifier type
 * @author Thomas Schneider
 * @version $Revision$
 */

@SuppressWarnings("unchecked")
public class TableList<H extends Comparable<H>, ID> implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -9192974285797951377L;

    protected String name;
    protected Class<H> headerType;
    protected H[] header;
    protected List<Row<ID>> rows;

    private static final Log LOG = LogFactory.getLog(TableList.class);

    /**
     * used by dumps
     */
    protected static final String DIV = "\t";
    protected static final String LF = System.getProperty("line.separator");
    public static final String FILE_EXT = ".csv";
    public static final String CELL_ROOT = "--";
    
    protected TableList() {}
    
    public TableList(String name, int cols, int rows) {
        this(name, cols);
        fill((Class<ID>) DefaultHandler.class, rows);
    }
    
    /**
     * column headers will be of type String.  see {@link #TableList(Class, int)}
     */
    public TableList(String name, int columnCount) {
        //don't use the generic type H (Class<H>) to be compilable on standard jdk javac.
        this((Class) String.class, columnCount);
        this.name = name;
    }

    /**
     * constructor to create any columns
     * 
     * @param headerType header type. if {@link String}, the column names will have their index as column-text. On
     *            default, the default constructor of the header type will be called to create an instance.
     * @param columnCount count of columns to create
     */
    public TableList(Class<H> headerType, int columnCount) {
        this((H[]) Array.newInstance(headerType, columnCount));
        this.headerType = headerType;
        if (headerType.equals(String.class)) {
            for (int i = 0; i < header.length; i++) {
                // using dynamic cast to be compilable on standard jdk16 compilers
                header[i] = headerType.cast(String.valueOf(i));
            }
        } else {//--> default construction
            BeanClass<H> bc = BeanClass.getBeanClass(headerType);
            for (int i = 0; i < header.length; i++) {
                header[i] = bc.createInstance();
            }
        }
    }

    /**
     * constructor
     * 
     * @param header array columns. must not be null and must contain at least one element!
     */
    public TableList(H... header) {
        this((Class<H>) (header[0] != null ? header[0].getClass() : Object.class), header);
    }

    /**
     * base constructor
     * 
     * @param header column definitions
     */
    protected TableList(Class<H> headerType, H... header) {
        super();
        assert header != null && header.length > 0;
        this.header = header;
        this.headerType = headerType;
        rows = new ArrayList<Row<ID>>();
    }

    public <T extends TableList<H, ID>> T fill(int rowCount) {
        return fill((Class<ID>) Row.class, rowCount);
    }
    
    /**
     * pre fill (e.g. a fixed sized table) with empty values
     * 
     * @param idType row identifier type
     * @param rowCount rows to fill
     */
    public <T extends TableList<H, ID>> T fill(Class<ID> idType, int rowCount) {
        return fill((ID[]) Array.newInstance(idType, rowCount));
    }

    /**
     * pre fill the table - all values will be null
     * 
     * @param ids row identifiers
     */
    public <T extends TableList<H, ID>> T fill(ID... ids) {
        /*
         * the row list is declarated to be a list - only at this point we try to optimize
         * the creation of a min-fixed-sized array.
         */
        if (rows instanceof ArrayList) {
            ((ArrayList<Row<ID>>) rows).ensureCapacity(ids.length);
        }
        for (int i = 0; i < ids.length; i++) {
            add(ids[i], null);
        }
        return (T) this;
    }

    /**
     * @return Returns the header.
     */
    public H[] getHeader() {
        return header;
    }

    /**
     * getColumns
     * 
     * @return headers as string array
     */
    public String[] getColumns() {
        if (headerType.equals(String.class)) {
            // using dynamic cast to be compilable on standard jdk16 compilers
            return String[].class.cast(header);
        } else {
            String[] columns = new String[header.length];
            for (int i = 0; i < header.length; i++) {
                columns[i] = header[i].toString();
            }
            return columns;
        }
    }

    public TableList<H, ID> addAll(boolean includedRowIds, List values) {
        return addAll(includedRowIds, values.toArray());
    }
    
    /**
     * addAll
     * @param includedRowIds if true, row-ids are included in the values
     * @param values to be splitted and packed into col/rows
     * @return tablelist itself
     */
    public TableList<H, ID> addAll(boolean includedRowIds, Object... values) {
        int colSize = getColumnCount() + (includedRowIds ? 1 : 0);
        Object[][] rows = CollectionUtil.split(values, colSize);
        Object rowId;
        for (int i = 0; i < rows.length; i++) {
            if (includedRowIds && i % colSize == 0) {
                rowId = rows[i][0];
            } else {
                rowId = createRowID(i);
            }
            add((ID) rowId, CollectionUtil.copyOfRange(rows[i], 1, rows[i].length));
        }
        return this;
    }
    
    protected ID createRowID(int i) {
        return (ID)Integer.valueOf(i);
    }

    protected Row<ID> createRow(int index, Object...values) {
        return new Row<ID>(index, createRowID(index), values);
    }
    /**
     * adds a new row to the table. if no values are given, an empty row will be added. if some values were given, but
     * not the size of the header, an exception will be thrown
     * 
     * @param rowId (optional) row identifier
     * @param values row values
     * @return the table object itself
     */
    public TableList<H, ID> add(ID rowId, Object... values) {
        return add(rows.size(), rowId, values);
    }

    /**
     * adds a new row to the table. if no values are given, an empty row will be added. if some values were given, but
     * not the size of the header, an exception will be thrown
     * 
     * @param index row index
     * @param rowId (optional) row identifier
     * @param values row values
     * @return the table object itself
     */
    public TableList<H, ID> add(int index, ID rowId, Object... values) {
        if (values == null || values.length == 0) {
            values = new Object[header.length];
        }
        rows.add(index, new Row<ID>(index, rowId, values));
        return this;
    }

    /**
     * resets an existing row of the table. if no values are given, an empty row will be added. if some values were
     * given, but not the size of the header, an exception will be thrown
     * 
     * @param index row index
     * @param rowId (optional) new row identifier
     * @param values row values
     * @return the table object itself
     */
    public TableList<H, ID> set(int index, ID rowId, Object... values) {
        if (values == null || values.length == 0) {
            values = new Object[header.length];
        } else {
            checkRowSize(values);
        }
        rows.set(index, new Row<ID>(index, rowId, values));
        return this;
    }

    /**
     * convenience to refresh the values of the given row (the table-value instances wont change!). the instances of
     * values might be gotten through {@link #get(Object, Class)}.no check will be done for the compatibility of the
     * given array type!
     * 
     * @param rowId row identifier
     * @param values new values to be copied to the table row
     * @return table itself
     */
    public <S> TableList<H, ID> set(ID rowId, S... values) {
        Object[] v = get(rowId);
        int count = Math.min(values.length, v.length);
        System.arraycopy(values, 0, v, 0, count);
        return this;
    }

    /**
     * resets an existing value of a given row and column of the table.
     * 
     * @param row index index
     * @param column column index
     * @param values row values
     * @return the table object itself
     */
    public TableList<H, ID> set(int row, int column, Object value) {
        checkSizes(row, column);
        rows.get(row).values[column] = value;
        return this;
    }

    /**
     * resets an existing value of a given row and column of the table.
     * 
     * @param row index index
     * @param column column index
     * @param values row values
     * @return the table object itself
     */
    public TableList<H, ID> set(int row, H column, Object value) {
        int c = Arrays.binarySearch(header, column);
        return set(row, c, value);
    }

    /**
     * resets an existing value of a given row and column of the table.
     * 
     * @param row index index
     * @param column column index
     * @param values row values
     * @return the table object itself
     */
    public TableList<H, ID> set(ID rowID, int column, Object value) {
        int r = indexOf(rowID);
        return set(r, column, value);
    }

    /**
     * resets an existing value of a given row and column of the table.
     * 
     * @param row index index
     * @param column column index
     * @param values row values
     * @return the table object itself
     */
    public TableList<H, ID> set(ID rowID, H column, Object value) {
        int r = indexOf(rowID);
        return set(r, column, value);
    }

    /**
     * remove row
     * 
     * @param rowId row identifier
     */
    public void remove(ID rowId) {
        for (Row<?> r : rows) {
            if (r.rowId.equals(rowId)) {
                rows.remove(r);
            }
        }
    }

    /**
     * remove row at given index
     * 
     * @param index row index
     * @return removed row
     */
    public Row<ID> remove(int index) {
        return rows.remove(index);
    }

    /**
     * base implementation of getting a cell value. overwrite this method to do extended formatting.
     * 
     * @param rowId row identifer
     * @param column column identifier
     * @return cell value
     * @throws NullPointerException if rowId couldn't be found
     */
    public Object get(int row, int column) {
        checkSizes(row, column);
        return rows.get(row).values[column];
    }

    /**
     * get
     * 
     * @param rowId row identifer
     * @param column column identifier
     * @return cell value
     * @throws NullPointerException if rowId couldn't be found
     */
    public Object get(ID row, H column) {
        int i = indexOf(row);
        return get(i, column);
    }

    /**
     * get
     * 
     * @param row row index
     * @param column column identifier
     * @return cell value or null
     */
    public Object get(int row, H column) {
        int i = Arrays.binarySearch(header, column);
        return get(row, i);
    }

    /**
     * getRowID
     * 
     * @param row row index
     * @return row identifier
     */
    public ID getRowID(int row) {
        return rows.get(row).rowId;
    }

    /**
     * gets row content directly. be careful using that method from outside. the base method {@link #get(int, int)} is
     * not used.
     * 
     * @param rowId row identifier
     * @param arrType array type. this must be the type, you gave on constructing this class! the value-array will not
     *            be changed!
     * @return row values
     */
    public Object[] get(ID rowId) {
        int i = indexOf(rowId);
        return rows.get(i).values;
    }

    /**
     * convenience to get a row wrapped into a known object type. delegates to {@link #get(Object)}. no check will be
     * done for the compatibility of the given array type! the returned array is a copy of th origin table row. please
     * see {@link #set(Object, Object...)} to refresh your changed values.
     * 
     * @param <S> array type to pack the given row into.
     * @param rowId row values to be returned
     * @param arrayType array type
     * @return array filled with row values
     */
    public <S> S[] get(ID rowId, Class<S> arrayType) {
        Object[] objects = get(rowId);
        S[] arr = (S[]) Array.newInstance(arrayType, objects.length);
        System.arraycopy(objects, 0, arr, 0, objects.length);
        return arr;
    }

    /**
     * get cell value
     * 
     * @param rowId row identifer
     * @param column column identifier
     * @return cell value
     * @throws ArrayIndexOutOfBoundsException if rowId couldn't be found
     */
    public Object get(ID rowId, int column) {
        return get(indexOf(rowId), column);
    }

    /**
     * like in {@link List}. works only, if row ID type implements its equals(Object) correctly.
     * 
     * @param rowId
     * @return
     */
    public int indexOf(ID rowId) {
        Row.temp.rowId = rowId;
        if (rowId.equals(Integer.valueOf(0))) // header row
            return -1;
        int i = rows.indexOf(Row.temp);
        if (i == -1) {
            throw new IllegalArgumentException("The row-id " + rowId + " couldn't be found on table " + this);
        }
        return i;
//        int i = 0;
//        for (Row r : rows) {
//            if (r.rowId.equals(rowId)) {
//                return i;
//            }
//            i++;
//        }
//        return -1;
    }

    /**
     * size
     * 
     * @return count of rows
     */
    public int size() {
        return rows.size();
    }

    /**
     * getColumnCount
     * 
     * @return count of header columns
     */
    public int getColumnCount() {
        return header.length;
    }

    public int getRowCount() {
        return size();
    }

    protected final void checkSizes(int row, int column) {
        /*
         * to be a little bit faster, we throw a readable exception only on debugging
         */
        if (LOG.isDebugEnabled()) {
            checkRowSize(row);
            checkColumnSize(column);
        }
    }

    protected final void checkRowSize(int row) {
        if (row == -1 || row >= rows.size()) {
            throw new IllegalArgumentException("The given row index " + row
                + " is unavailable. Only "
                + rows.size()
                + " rows are available!");
        }
    }

    protected final void checkColumnSize(int column) {
        if (column == -1 || column >= header.length) {
            LOG.debug(dump());
            throw new IllegalArgumentException("The given column index " + column
                + " is unavailable. Only "
                + header.length
                + " columns are available!");
        }
    }

    protected final void checkRowSize(Object... values) {
        assert values.length != header.length : "value array must have same size as header array!";
    }

    public void save(String relDir) {
        String path = relDir + name + FILE_EXT;
        new File(path).getParentFile().mkdirs();
        FileUtil.writeBytes(dump().getBytes(), path, false);
    }
    
    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    public static <T> T load(String file) {
        return (T) load(file, TableList.class);
    }
    public static <T extends TableList> T load(String file, Class<T> type) {
        return load(file, type, DIV);
    }
    public static <T extends TableList> T load(String file, Class<T> type, String div) {
        //TODO: load from dump...
        T logic = null;
        try {
            Scanner sc = new Scanner(new File(file));
            if (sc.hasNextLine()) {//header
                String[] header = sc.nextLine().split(div);
                Object[] args = new Object[1];
                String[] header1 = CollectionUtil.copyOfRange(header, 1, header.length);
                //convert the header(strings) into types defaultHeaderType 
                Comparable[] h = new Comparable[header1.length];
                for (int i = 0; i < header1.length; i++) {
                    h[i] = (Comparable) BeanClass.call(type, "createDefaultHeader", new Class[]{Object.class}, header1[i]);
                }
                args[0] = h;
                //create the table instance
                logic = BeanClass.createInstance(type, args);
                logic.name = StringUtil.substring(file, "/", ".", true);
                String[] ss;
                while (sc.hasNextLine()) {
                    ss = sc.nextLine().split(div);
                    Object[] row = CollectionUtil.copyOfRange(ss, 0, ss.length, Object[].class);
                    //TODO: row-id?
                    logic.addAll(true, row);
                }
            }
        } catch (FileNotFoundException e) {
            ManagedException.forward(e);
        }
        return logic;
    }
    
    /**
     * wraps the given source string into a default header instance of this class. this implementatino simply returns
     * the source string. each extension class shuold provide its own 'createDefaultHeader(string)'
     * 
     * @param source header to be wrapped
     * @return wrapped source
     */
    protected static Object createDefaultHeader(Object source) {
        return source;
    }
    
    public String dump() {
        return dump(DIV, true);
    }
    
    /**
     * dump
     * 
     * @return
     */
    public String dump(String div, boolean resolve) {
        StringBuilder buf = new StringBuilder(20 + header.length
            * 5
            + rows.size()
            * 20
            + rows.size()
            * header.length
            * 4);
        buf.append(CELL_ROOT + div);//row-id header
        for (int i = 0; i < header.length; i++) {
            buf.append(header[i] + div);
        }
        buf.append(LF);
        dumpValues(buf, div, resolve);
        return buf.toString();
    }

    protected void dumpValues(StringBuilder buf, String div, boolean resolve) {
        for (Row<ID> r : rows) {
            buf.append(r.dump() + LF);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "("
            + header.length
            + ", "
            + rows.size()
            + "), rowIDs: "
            + StringUtil.toString(rows, 80);
    }
}

/**
 * internal row/line holder. the value array cannot be typed - each row or column may hold different value types.
 * 
 * @param <ID> optional row-id type
 * @author Thomas Schneider
 * @version $Revision$
 */
class Row<ID> {

    static final Row<Object> temp = new Row<Object>(0, null, (Object[]) null);

    /** on default, the index of that row an its TableList */
    int index;
    /**
     * optional row identifier (object reference belonging to the given values). if null, the {@link #NO_ID} will be
     * used!
     */
    ID rowId;
    /** cell values (belonging to the index and row identifier */
    Object[] values;

    /**
     * constructor
     * 
     * @param index
     * @param rowId
     * @param values
     */
    @SuppressWarnings("unchecked")
    public Row(int index, ID rowId, Object... values) {
        super();
        this.index = index;
        this.rowId = (ID) (rowId != null ? rowId : String.valueOf(index + 1));
        this.values = values;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return rowId.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Row)) {
            return false;
        }
        return rowId.equals(((Row) obj).rowId);
    }

    public String dump() {
        return dump(TableList.DIV);
    }
    
    public String dump(String div) {
        StringBuilder buf = new StringBuilder(values.length * 4 + 20);
        buf.append(rowId + div);
        for (int i = 0; i < values.length; i++) {
            buf.append(nonull(values[i]) + div);
        }
        return buf.toString();
    }

    
    protected String nonull(Object value) {
        return value == null ? "-" : value.toString();
    }

    @Override
    public String toString() {
        return index + ":" + rowId;
    }
}