/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Dec 8, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.logictable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Structures a {@link #content} having nested blocks starting with {@link #begin} and ending with {@link #end}.
 * <p/>
 * The iteration of separated blocks will be done from bottom to top (as an equation would do).
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class Structure<CONTENT extends List<ITEM>, ITEM, BEGIN, END> implements Iterator<ITEM> {
    CONTENT content;
    BEGIN begin;
    END end;

    /** each tree-node having list of items will point to a more structured content */
    Map<Integer, CONTENT> tree;

    /**
     * constructor
     * 
     * @param content
     * @param begin
     * @param end
     */
    public Structure(CONTENT content, BEGIN begin, END end) {
        super();
        this.content = content;
        this.begin = begin;
        this.end = end;
        tree = new TreeMap<Integer, CONTENT>();
        tree.put(0, content);
        separate(content, tree);
    }

    protected CONTENT separate(CONTENT content, Map<Integer, CONTENT> tree) {
        int ibegin = content.indexOf(begin);
        int iend = content.lastIndexOf(end);
        if (iend == -1)
            iend = content.size();
        CONTENT subContent = (CONTENT) content.subList(ibegin + 1, iend - 1);
        tree.put(tree.size() + 1, subContent);
        if (ibegin < 0 || iend < 0)
            return subContent;
        else
            return separate(subContent, tree);
    }

    /**
     * getTree
     * @return
     */
    public Map<Integer, CONTENT> getTree() {
        return tree;
    }
    
    @Override
    public boolean hasNext() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ITEM next() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void remove() {
        // TODO Auto-generated method stub

    }
}
