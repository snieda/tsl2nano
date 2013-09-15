/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts, Thomas Schneider
 * created on: 11.09.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.h5;

import de.tsl2.nano.AppLoader;

/**
 * Loader for {@link NanoH5}.
 * 
 * @author ts, Thomas Schneider
 * @version $Revision$
 */
public class Loader extends AppLoader {
    public static void main(String[] args) {
        new Loader().start("de.tsl2.nano.h5.NanoH5", null, args);
    }
}
