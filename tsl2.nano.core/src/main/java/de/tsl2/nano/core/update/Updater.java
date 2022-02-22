/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 05.06.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.core.update;

import java.util.Date;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;

/**
 * Version Updater, see {@link #run(String, String, String, Object)} and {@link #checkAndUpdate(String, String)}.
 * 
 * @author Tom
 * @version $Revision$
 */
public class Updater {

	private String appName = "h5"; //default on tsl2.nano.h5 framework
	private String downloadURL;
	private String currentVersion;
	private Date lastUpdate;
	private int dayInterval;

	
	public Updater() {
	}

	public Updater(String appName, String downloadURL, String currentVersion, Date lastUpdate, int dayInterval) {
		this.appName = appName;
		this.downloadURL = downloadURL;
		this.currentVersion = currentVersion;
		this.lastUpdate = lastUpdate;
		this.dayInterval = dayInterval;
	}

    public boolean run(String configFile, String newVersion, Object environment) {
    	return run(configFile, currentVersion, newVersion, environment);
    }
    
	/**
     * update mechanism for ENV applications. Does update the current environment if application was started with new
     * update (see {@link #checkAndUpdate(String, String)} - this update should provide a new version update class).
     * <p/>
     * This method will do a backup of the given configFile and checks the current with the new version. if not equal, a
     * version update class of type {@link Runnable} with following name convention will be searched, loaded and started
     * (through {@link Runnable#run()}:
     * <p/>
     * ${ENV-PACKAGE-PATH} '.update.' + ENVUpdate${MAJOR}v${MINOR}v${BUILD}
     * <p/>
     * Example: de.tsl2.nano.core.ENVUpdate1v1v0
     * <p/>
     * that update class must have a constructor with one object argument.
     * 
     * @param configFile to be backup-ed
     * @param currentVersion to be checked
     * @param newVersion to be checked against currenVersion
     * @param environment to be given to the update implementations
     * @return true, if version was updated to newVersion, otherwise false
     */
    public boolean run(String configFile, String currentVersion, String newVersion, Object environment) {
        if (currentVersion != null && newVersion != null && !currentVersion.equals(newVersion)) {
            String vo = getVersionNo(currentVersion);
            String vn = getVersionNo(newVersion);
            try {
                FileUtil.copy(configFile, configFile + "." + vo);
                //runs all version updaters recursively between current and new
                versionUpdate(environment, currentVersion, vn);
                return true;
            } catch (Exception e) {
                //TODO: exception handling
                LogFactory.log("NOTHING TO DO: " + currentVersion + " -> " + newVersion + " (" + e.toString() + ")");
                return false;
            }
        }
        return false;
    }

    /**
     * versionUpdate
     * 
     * @param environment
     * @param newVersionNumber
     */
    void versionUpdate(Object environment, String currentVersion, String newVersionNumber) {
        Class<IVersionRunner> updClass = findLastVersionUpdaterClass(environment, newVersionNumber);
        if (updClass != null)
        	BeanClass.createInstance(updClass).run((ENV)environment, currentVersion);
    }

	private Class<IVersionRunner> findLastVersionUpdaterClass(Object environment, String versionNumber) {
		Class<IVersionRunner> updClass = null;
		try {
			updClass = (Class<IVersionRunner>) BeanClass.load(getVersionUpdaterClass(environment.getClass(), versionNumber), null, false);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			updClass = findLastVersionUpdaterClass(environment, evalPreviousVersion(versionNumber));
		}
		return updClass;
	}

    protected String getVersionNo(String txt) {
        return StringUtil.substring(txt, appName + "-", "-");
    }

    protected String getVersionUpdaterClass(Class<? extends Object> cls, String version) {
        return cls.getPackage().getName() + ".update." + cls.getSimpleName() + "Update" + version.replace('.', 'v');
    }

    protected static String evalPreviousVersion(String version) {
    	final String V = ".";
    	String part, v = version;
    	Integer n;
		while ((part = StringUtil.substring(v, V, null, true, true)) != null) {
    		v = StringUtil.substring(v, null, V, true);
	    	n = Integer.valueOf(part);
	    	if (n == 0) {
	    		continue;
	    	}
	    	return v + V + --n;
    	}
    	return null;
    }
    public boolean checkAndUpdate() {
    	return checkAndUpdate(currentVersion, downloadURL);
    }
    
    /**
     * checks for a new version and downloads it.
     * 
     * @param currentVersion
     * @param versionURL
     * @return
     */
    public boolean checkAndUpdate(String currentVersion, String versionURL) {
    	if (!hasToCheck(lastUpdate, dayInterval)) {
    		LogFactory.log("Updater: next update not before: " + lastUpdate + " + " + dayInterval + " days");
    		return false;
    	}
        try {
            if (NetUtil.isOnline()) {
                String newVersion = NetUtil.get(versionURL);
                String vo = getVersionNo(currentVersion);
                String vn = getVersionNo(newVersion);
                if (!vo.equals(vn)) {
                	LogFactory.log("updating tsl2.nano: " + vo + " -> " + vn );
                    String downloadURL = getDownloadURL(newVersion);
                    NetUtil.download(downloadURL, System.getenv("user.dir"));
                }
                return true;
            } else {
                LogFactory.log("offline -> no update-check possible");
            }
        } catch (Throwable e) {
        	// why throwable: it's only an update and on downloading the Error ExceptionInInitializerError
        	// may be thrown, if the jdk has an error on loading the unlimited jce-policy files
            LogFactory.log("UPDATE-ERROR: couldn't download new version: " + e.toString());
            LogFactory.log("may be you should download and install the unlimited jce-policy files in your jdk");
        }
        return false;
    }

    protected String getDownloadURL(String newVersion) {
        return StringUtil.extract(newVersion, "http[s]?\\:\\/\\/.*-standalone[.]jar");
    }

	public static boolean hasToCheck(Date lastUpdate, int dayInterval) {
		return lastUpdate == null || DateUtil.addDays(lastUpdate, dayInterval).before(new Date());
	}
}
