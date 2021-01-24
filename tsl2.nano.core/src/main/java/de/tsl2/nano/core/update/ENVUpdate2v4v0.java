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
public class ENVUpdate2v4v0 implements IVersionRunner {
	public String previousVersion() {return "1.1.0";}
    @Override
    public void update(ENV env, String currentVersion) {
        //TODO:
    	System.out.println(previousVersion() + " -> 2.4.0: \n\t"
    			+ "package incubation,specification -> specification\n\t"
    			+ "rules: specification: rule, rulescript, ruledecisiontable, action, query, webclient\n\t"
    			+ "replace in environment.xml : action.layout.width Integer -> String\n\t"
    			+ "add in environment.xml: frame.style add style.template from body to all nav tags (at the end)");
    }
}
