/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Feb 17, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.util.bean.def;

import java.io.Serializable;

/**
 * status base construct - used by bean values on validation. no severity is defined. the status {@link #OK},
 * {@link #WARN} and {@link #ERROR} should fulfill a validation status.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public interface IStatus extends Serializable {

    static final int OK = 0;
    static final int WARN = 1;
    static final int ERROR = 2;

    /** status names */
    static final String[] TAG = { "OK", "WARN", "ERROR" };

    /** returns true, if a status has no warnings or errors */
    boolean ok();

    /** if a status has an error or a warning, the message will be returned -otherwise it returns null */
    String message();

    /** if an error occurred, the exception will be returned, otherwise null */
    Throwable error();
//    int severity();

    /** simple ok status. should be used if you have a status without warnings and errors */
    static final IStatus STATUS_OK = new IStatus() {
        /** serialVersionUID */
        private static final long serialVersionUID = 1L;

        @Override
        public boolean ok() {
            return true;
        }

        @Override
        public String message() {
            return null;
        }

        @Override
        public Throwable error() {
            return null;
        }
        @Override
        public String toString() {
            return TAG[OK];
        }
    };
}
