/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 04.06.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.core.update;

import de.tsl2.nano.core.ENV;

/**
 * Version Updater, called by Updater.
 * @author Tom
 * @version $Revision$ 
 */
public class ENVUpdate2v4v3 implements IVersionRunner {
    public String previousVersion() {return "2.4.0";}

    @Override
    public void update(ENV env, String currentVersion) {
        //TODO: implement :-(
    	System.out.println("UPDATE 2.4.0 -> 2.4.3: PLEASE UPDATE THE FOLLOWING IN YOUR APPLICATION:\n"
    			+ "    remove old hibernate and h2 jars\n"
    			+ "    runserver.cmd, environment.xml: add -ifNotExists\n"
    			+ "    hash changed: re-create users.xml and <user-name>.xml, presentation-xml seals\n");
    }

}
