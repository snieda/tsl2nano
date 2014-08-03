/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts, Thomas Schneider
 * created on: 11.09.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package my.app;

import de.tsl2.nano.core.AppLoader;

/**
 * Loader for {@link MyApp}.
 * 
 * @author ts, Thomas Schneider
 * @version $Revision$
 */
public class Loader extends AppLoader {
    public static void main(String[] args) {
        new Loader().start("my.app.MyApp", args);
    }
}
