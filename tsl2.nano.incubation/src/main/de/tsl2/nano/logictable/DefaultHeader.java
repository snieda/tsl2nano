package de.tsl2.nano.logictable;

import de.tsl2.nano.core.util.DefaultFormat;

/**
 * simple column-formatter using alphabetic index
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class DefaultHeader extends DefaultFormat implements Comparable<DefaultHeader> {
    /** serialVersionUID */
    private static final long serialVersionUID = 4873057720074938549L;
    
    static final char charStart = 'A';
    int columnIndex;
    String title;

    protected DefaultHeader() {
    }
    
    public DefaultHeader(int columnIndex) {
        this.columnIndex = columnIndex;
    }

    /**
     * constructor
     * @param title
     */
    public DefaultHeader(String title) {
        this.title = title;
    }


    @Override
    public String toString() {
        if (title == null) {
            title = getTitle(columnIndex);
        }
        return title;
    }

    private String getTitle(int i) {
        StringBuilder buf = new StringBuilder();
        //TODO: implement i > alphabetic-count
            buf.append(getCharacter(i));
            return buf.toString();
    }

    private char getCharacter(int i) {
        char c = (char) (i + charStart);
        return c;
    }

    @Override
    public int compareTo(DefaultHeader o) {
        return toString().compareTo(o.toString());
    }
}
