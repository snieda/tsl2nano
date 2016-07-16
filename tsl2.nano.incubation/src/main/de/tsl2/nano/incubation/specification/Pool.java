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
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.execution.IPRunnable;

/**
 * Generic Pool holding all defined (loaded) instances of an {@link IPRunnable} implementation. Useful for e.g. Rules,
 * Queries and Actions.
 * <p/>
 * should be used as singelton by a service-factory. persists its runnables in a directory, given by
 * {@link #getDirectory()} (using the pool instance generic type as directory name).
 * <p/>
 * It is possible to hold different extensions of the given generic type. Then you access them through ghe getter
 * {@link #get(String, Class)} - otherwise it is sufficient to call {@link #get(String)} using internally the default
 * type, evaluated by {@link #getType()}.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Pool<T extends IPRunnable<?, ?>> {

    private static final Log LOG = LogFactory.getLog(Pool.class);
    Map<String, T> runnables;

    private Map<String, T> runnables() {
        if (runnables == null)
            loadRunnables();
        return runnables;
    }

    private void loadRunnables() {
        runnables = new HashMap<String, T>();
        String dirName = getDirectory();
        LOG.info("loading " + getType().getSimpleName().toLowerCase() + "(s) from " + dirName);
        File dir = new File(dirName);
        dir.mkdirs();
        if (!Modifier.isAbstract(getType().getModifiers())) {
            File[] runnableFiles = dir.listFiles();
            Class<T> type = getType();
            for (int i = 0; i < runnableFiles.length; i++) {
                try {
                    loadRunnable(runnableFiles[i].getPath(), type);
                } catch (Exception e) {
                    LOG.error(e);
                }
            }
        }
    }

    /**
     * getDirectory
     * 
     * @return
     */
    public String getDirectory() {
        return ENV.getConfigPathRel()
            + "specification/"
            + StringUtil.substring(BeanClass.getDefiningClass(this.getClass()).getSimpleName().toLowerCase(), null,
                Pool.class.getSimpleName().toLowerCase()) + "/";
    }

    /**
     * default type, given by generic of this instance
     * 
     * @return type to load
     */
    @SuppressWarnings("unchecked")
    protected Class<T> getType() {
        return (Class<T>) BeanUtil.getGenericType(this.getClass());
    }

    private <I extends T> I loadRunnable(String path, Class<I> type) {
        try {
            I r = ENV.load(path, type);
            runnables.put(r.getName(), r);
            return r;
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * gets the runnable by name
     * 
     * @param name rule/query to find
     * @return rule/query or null
     */
    public T get(String name) {
        return get(name, getType());
    }

    /**
     * gets the runnable by name
     * 
     * @param name runnable to find
     * @param type runnable type
     * @return runnable or null
     */
    @SuppressWarnings("unchecked")
    public <I extends T> I get(String name, Class<I> type) {
        T runnable = runnables().get(name);
        //perhaps not loaded (new or recursive)
        return (I) (runnable != null ? runnable : loadRunnable(getFileName(name), type));
    }

    protected String getFileName(String name) {
        return name.endsWith(".xml") ? name : getDirectory() + name + ".xml";
    }

    /**
     * delegates to {@link #add(String, IPRunnable)} using {@link IPRunnable#getName()}
     */
    public void add(T runnable) {
        add(runnable.getName(), runnable);
    }

    /**
     * adds the given runnable to the pool
     * 
     * @param name runnable name
     * @param runnable runnable to add
     */
    public void add(String name, T runnable) {
        runnables().put(name, runnable);
        String fileName = getFileName(runnable.getName());
        LOG.info("adding runnable '" + name + "' and saving it to " + fileName);
        ENV.save(fileName, runnable);
    }

    /**
     * reset
     */
    public void reset() {
        runnables = null;
    }

}
