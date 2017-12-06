/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 28.10.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.incubation.vnet.workflow;

import java.util.HashMap;

/**
 * dummy implementation to be used by vnet, needing nodes with comparable cores.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class ComparableMap<K, V> extends HashMap<K, V> implements Comparable<ComparableMap<K, V>> {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    @Override
    public int compareTo(ComparableMap<K, V> o) {
        return 0;
    }

}
