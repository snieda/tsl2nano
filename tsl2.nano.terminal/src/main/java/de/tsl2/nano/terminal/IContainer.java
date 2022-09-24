/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 23.12.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.terminal;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * interface for items having child items
 * 
 * @author Tom
 * @version $Revision$
 */
public interface IContainer<I> extends IItem<I> {
    I getValue(int i);

    /**
     * @return child items depending on the current context.
     */
    @SuppressWarnings("rawtypes")
    public List<? extends IItem<I>> getNodes(final Map context);

    /**
     * adds a child
     * 
     * @param item item
     * @return true, if successful added
     */
    <II extends IItem<I>> boolean add(II item);

    /**
     * removes a child
     * 
     * @param item item
     * @return true, if successful removed
     */
    <II extends IItem<I>> boolean remove(II item);

    /**
     * evaluates the next child node depending on the current environment
     * @param in input stream
     * @param out terminal output
     * @param env current terminal environment
     * @return normally the container itself
     */
    <II extends IItem<I>> II next(InputStream in, PrintStream out, Properties env);
}
