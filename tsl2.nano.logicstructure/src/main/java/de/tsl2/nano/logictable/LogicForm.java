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

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.CollectionUtil;
import de.tsl2.nano.core.util.FormatUtil;
import de.tsl2.nano.execution.IPRunnable;
import de.tsl2.nano.incubation.specification.Pool;

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
	transient private boolean locked;

    /**
     * constructor
     * @param header
     */
    public LogicForm(H... header) {
        super(header);
    }

    public LogicForm(Object... header) {
        super((H[]) CollectionUtil.copyOfRange(header, 0, header.length, DefaultHeader[].class));
    }

    /**
     * constructor
     * 
     * @param columnCount
     */
    public LogicForm(String name, int columnCount) {
        super(name, columnCount);
    }

    /**
     * constructor
     * @param cols
     * @param rows
     */
    public LogicForm(String name, int cols, int rows) {
        super(name, cols, rows);
    }
    
    protected static Object createDefaultHeader(Object source) {
        return new DefaultHeader((String)source);
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
			} else if (expression.matches(ENV.get(Pool.class).getFullExpressionPattern())) {
				IPRunnable r = ENV.get(Pool.class).get(expression);
				if (r != null) {
					if (locked) {
						return null;
					} else {
						try {
							locked = true;
							return r.run(this.getValueMap());
						} finally {
							locked = false;
						}
					}
				}
			}
		}
		return e;
	}

    @SuppressWarnings("unchecked")
    private H createFormat(String string) {
        return (H) FormatUtil.getDefaultFormat(string, true);
    }
}
