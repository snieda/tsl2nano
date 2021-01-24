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
public class ENVUpdate1v1v0 implements IVersionRunner {
	public String previousVersion() {return null;}
    @Override
    public void update(ENV env, String currentVersion) {
        //reset to default
    	System.out.println(previousVersion() + " -> 1.1.0: removing property 'app.frame.style'");
        env.getProperties().remove("app.frame.style");
    }
}
