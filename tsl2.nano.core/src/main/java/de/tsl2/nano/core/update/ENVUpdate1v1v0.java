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
@SuppressWarnings("static-access")
public class ENVUpdate1v1v0 implements Runnable {
    private String VERS_PREVIOUS = null;
    private ENV env;
    private String currentVersion;
    
    /**
     * constructor
     */
    public ENVUpdate1v1v0(Object env, String currentVersion) {
        this.env = (ENV) env;
        this.currentVersion = currentVersion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        runPreviousVersionUpdate();
        //reset to default
        env.getProperties().remove("app.frame.style");
    }

    void runPreviousVersionUpdate() {
        //nothing to do, no previous update available
        //TODO: refactore cycling access to updater
        new Updater().run(env.getConfigFile().getPath(), VERS_PREVIOUS, currentVersion, env);
    }
}