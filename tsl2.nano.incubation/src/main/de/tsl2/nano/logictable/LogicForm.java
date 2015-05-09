/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Dec 14, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.logictable;

import java.text.Format;

/**
 * Adds direct cell formatting to the {@link LogicTable}.
 * <p/>
 * TODO: implement cell formatting
 * <p/>
 * TODO: implement import/export html
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class LogicForm<H extends Format & Comparable<H>, ID> extends LogicTable<H, ID> {
    static final String CONFIG_EXPRESSION = ">>";

    /**
     * constructor
     * 
     * @param columnCount
     */
    public LogicForm(int columnCount) {
        super(columnCount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(int row, int column) {
        Object e = super.get(row, column);
        if (e instanceof String) {
            String expression = (String) e;
            if (expression.contains(CONFIG_EXPRESSION)) {
                try {
                    String[] c = expression.split(CONFIG_EXPRESSION);
                    createFormat(c[1]);
                    return header[column].format(c[0]);
                } catch (Exception ex) {
                    return e;
                }
            }
        }
        return e;
    }

    private H createFormat(String string) {
        // TODO Auto-generated method stub
        return null;
    }
}
