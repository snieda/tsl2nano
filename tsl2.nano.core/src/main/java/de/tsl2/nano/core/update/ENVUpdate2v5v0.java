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
 * Version Updater, called by Updater. Does not run a previous version updater, because this is the first one!
 * @author Tom
 * @version $Revision$ 
 */
public class ENVUpdate2v5v0 implements Runnable {
    private String VERS_PREVIOUS = null;
    private ENV env;
    private String currentVersion;
    
    /**
     * constructor
     */
    public ENVUpdate2v5v0(Object env, String currentVersion) {
        this.env = (ENV) env;
        this.currentVersion = currentVersion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        // runPreviousVersionUpdate();
        //TODO: implement ;-(
    	System.out.println("UPDATE 2.4.3 -> 2.5.0: PLEASE UPDATE THE FOLLOWING IN YOUR APPLICATION:\n"
    		+ "    change all presentations having actions: all boolean/int values and the shortDescription are now attributes!\n"
    		+ "    change all activites of workflows: do the same changes as on the actions!\n"
    		+ "    secureactions, methodactions -> the same\n");
    }

    void runPreviousVersionUpdate() {
        //nothing to do, no previous update available
        //TODO: refactore cycling access to updater
        new Updater().run(env.getConfigFile().getPath(), VERS_PREVIOUS, currentVersion, env);
    }
}