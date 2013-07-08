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

import java.text.Format;

import de.tsl2.nano.format.DefaultFormat;

/**
 * Simple extension of {@link TableList} to get formatted values.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class FormTable<H extends Format & Comparable<H>, ID> extends TableList<H, ID> {

    /**
     * see {@link TableList#TableList(Class, int)}
     */
    @SuppressWarnings("unchecked")
    public FormTable(int columnCount) {
        //don't use the generic type H (Class<H>) to be compilable on standard jdk javac.
        super((Class) DefaultFormat.class, columnCount);
    }

    /**
     * see {@link TableList#TableList(Object...)}
     */
    public FormTable(Format... header) {
        super((H[]) header);
    }

    /**
     * get formatted cell value. see {@link TableList#get(int, int)}
     */
    @Override
    public Object get(int row, int column) {
        Object value = super.get(row, column);
        return header[column].format(value);
    }
}
