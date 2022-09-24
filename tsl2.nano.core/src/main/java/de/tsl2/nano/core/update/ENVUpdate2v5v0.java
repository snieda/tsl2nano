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
public class ENVUpdate2v5v0 implements IVersionRunner {
	public String previousVersion() {return "2.4.3";}
    @Override
    public void update(ENV env, String currentVersion) {
        //TODO: implement ;-(
    	System.out.println("UPDATE 2.4.12 -> 2.5.0: PLEASE UPDATE THE FOLLOWING IN YOUR APPLICATION:\n"
    		+ "    change all presentations having actions: all boolean/int values and the shortDescription are now attributes!\n"
    		+ "    change all activites of workflows: do the same changes as on the actions!\n"
    		+ "    secureactions, methodactions -> the same\n"
            + "    the following incubation packages will be moved ('incubation' part will be removed): specification, vnet, terminal"
            + "    h5: the standalone libraries will be actualized to newest versions, default database will be changed from h2 to hsqldb");
    }
}
