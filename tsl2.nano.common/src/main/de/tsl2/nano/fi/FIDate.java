/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jun 29, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.fi;


/**
 * 
 * @author Thomas Schneider
 * @version $Revision$ 
 */
public class FIDate<T extends DateCallback> {
    T callback;
    /**
     * constructor
     */
    public FIDate(T callback) {
        this.callback = callback;
    }
    
    public T today() {
        
        return callback;
    }
    public T add() {
        return callback;
    }
}
