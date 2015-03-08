/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 23.12.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.incubation.terminal;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Properties;

/**
 * interface for items having child items
 * 
 * @author Tom
 * @version $Revision$
 */
public interface IContainer<I> extends IItem<List<I>> {
    I getValue(int i);

    /**
     * @return child items
     */
    List<IItem<I>> getNodes();

    IItem<I> next(InputStream in, PrintStream out, Properties env);
    
    /**
     * adds a child
     * 
     * @param item item
     * @return true, if successful added
     */
    boolean add(IItem<I> item);

    /**
     * removes a child
     * 
     * @param item item
     * @return true, if successful removed
     */
    boolean remove(IItem<I> item);
}
