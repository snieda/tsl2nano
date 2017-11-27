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

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.log.LogFactory;
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

    /**
     * update mechanism for ENV applications. Does update the current environment if application was started with new
     * update (see {@link #checkAndUpdate(String, String)} - this update should provide a new version update class).
     * <p/>
     * This method will do a backup of the given configFile and checks the current with the new version. if not equal, a
     * version update class of type {@link Runnable} with following name convention will be search, loaded and started
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
        if (currentVersion != null && !currentVersion.equals(newVersion)) {
            LogFactory.log("VERSION CHANGED: " + currentVersion + " -> " + newVersion);
            String vo = getVersionNo(currentVersion);
            String vn = getVersionNo(newVersion);
            try {
                FileUtil.copy(configFile, configFile + "." + vo);
                //runs all version updaters recursively between current and new
                versionUpdate(environment, currentVersion, vn);
                LogFactory.log("VERSION NOW: " + newVersion);
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
        Class<Runnable> updClass =
            (Class<Runnable>) BeanClass.load(getVersionUpdaterClass(environment.getClass(), newVersionNumber));
        BeanClass.createInstance(updClass, environment, currentVersion).run();
    }

    protected String getVersionNo(String txt) {
        return StringUtil.substring(txt, "h5-", "-");
    }

    protected String getVersionUpdaterClass(Class<? extends Object> cls, String version) {
        return cls.getPackage().getName() + ".update." + cls.getSimpleName() + "Update" + version.replace('.', 'v');
    }

    /**
     * checks for a new version and downloads it.
     * 
     * @param currentVersion
     * @param versionURL
     * @return
     */
    public boolean checkAndUpdate(String currentVersion, String versionURL) {
        try {
            if (NetUtil.isOnline()) {
                String newVersion = NetUtil.get(versionURL);
                String vo = getVersionNo(currentVersion);
                String vn = getVersionNo(newVersion);
                if (!vo.equals(vn)) {
                    String downloadURL = getDownloadURL(newVersion);
                    NetUtil.download(downloadURL, System.getenv("user.dir"));
                }
                return true;
            } else {
                LogFactory.log("offline -> no update-check possible");
            }
        } catch (Exception e) {
            LogFactory.log("couldn't download new version: " + e.toString());
        }
        return false;
    }

    protected String getDownloadURL(String newVersion) {
        return StringUtil.extract(newVersion, "http[s]?\\:\\/\\/.*-standalone[.]jar");
    }
}
