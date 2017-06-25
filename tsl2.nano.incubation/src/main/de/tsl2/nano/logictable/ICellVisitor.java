/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 10.06.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.logictable;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public interface ICellVisitor {
    void visit(int col, int row, Object cell);
}
