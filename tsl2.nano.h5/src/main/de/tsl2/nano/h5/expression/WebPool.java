/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 26.02.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5.expression;

import de.tsl2.nano.incubation.specification.Pool;

/**
 * Pool of {@link WebClient} definitions - to be found on file-system as described in {@link Pool#getDirectory()}
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class WebPool extends Pool<WebClient<?>> {

    /**
     * constructor
     */
    protected WebPool() {
        super();
    }
}
