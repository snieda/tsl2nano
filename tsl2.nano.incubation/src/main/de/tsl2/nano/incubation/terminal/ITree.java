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

import java.util.List;

/**
 * 
 * @author Tom
 * @version $Revision$ 
 */
public interface ITree<I> extends IItem<List<I>> {
    I getValue(int i);
    List<IItem<I>> getNodes();
    boolean add(IItem<I> item);
    boolean remove(IItem<I> item);
}
