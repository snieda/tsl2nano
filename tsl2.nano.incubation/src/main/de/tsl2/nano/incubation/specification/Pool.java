/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 26.02.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.incubation.specification;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.XmlUtil;
import de.tsl2.nano.execution.IPRunnable;

/**
 * Generic Pool holding all defined (loaded) instances of an {@link IPRunnable} implementation. Useful for e.g. Rules
 * and Queries.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Pool<T extends IPRunnable<?, ?>> {

    private static final Log LOG = LogFactory.getLog(Pool.class);
    Map<String, T> runnables;

    private Map<String, T> runnables() {
        if (runnables == null) {
            loadRunnables();
        }
        return runnables;
    }

    private void loadRunnables() {
        runnables = new HashMap<String, T>();
        String dirName = getDirectory();
        LOG.info("loading " + getType().getSimpleName().toLowerCase() + "(s) from " + dirName);
        File dir = new File(dirName);
        dir.mkdirs();
        File[] ruleFiles = dir.listFiles();
        for (int i = 0; i < ruleFiles.length; i++) {
            loadRunnable(ruleFiles[i].getPath());
        }
    }

    /**
     * getDirectory
     * 
     * @return
     */
    private String getDirectory() {
        return ENV.getConfigPath() + "specification/" + getType().getSimpleName().toLowerCase() + "/";
    }

    private Class<T> getType() {
        return (Class<T>) BeanUtil.getGenericType(this.getClass());
    }

    private T loadRunnable(String path) {
        try {
            T r = ENV.get(XmlUtil.class).loadXml(path, getType());
            runnables.put(r.getName(), r);
            return r;
        } catch (Exception e) {
            LOG.error(e);
            return null;
        }
    }

    /**
     * getRule
     * 
     * @param name rule to find
     * @return rule or null
     */
    public T get(String name) {
        T rule = runnables().get(name);
        //perhaps not loaded (new or recursive)
        return rule != null ? rule : loadRunnable(getFileName(name));
    }

    private String getFileName(String name) {
        return name.endsWith(".xml") ? name : getDirectory() + name + ".xml";
    }

    /**
     * adds the given rule to the pool
     * 
     * @param name rule name
     * @param runnable rule to add
     */
    public void add(String name, T runnable) {
        runnables().put(name, runnable);
        String fileName = getFileName(runnable.getName());
        LOG.info("adding runnable '" + name + "' and saving it to " + fileName);
        ENV.get(XmlUtil.class).saveXml(fileName, runnable);
    }

    /**
     * reset
     */
    public void reset() {
        runnables = null;
    }

}
