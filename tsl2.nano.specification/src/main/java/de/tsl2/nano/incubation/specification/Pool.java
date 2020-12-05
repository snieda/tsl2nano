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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.execution.IPRunnable;

/**
 * Generic Pool holding all defined (loaded) instances of an {@link IPRunnable} implementation. Useful for e.g. Rules,
 * Queries and Actions.<p/>
 * The runnable types must registere itself through {@link #registerTypes(Class...)} and must implement IPrefixed, used
 * has prefix for the key name. So, on calling get(..) you should give the name with prefix or you provide the runnable type.
 * <p/>
 * should be used as singelton by a service-factory. persists its runnables in a directory, given by
 * {@link #getDirectory()} (using the pool instance generic type as directory name).
 * <p/>
 * It is possible to hold different extensions of the given generic type. Then you access them through 
 * {@link #get(String, Class)} - otherwise it is sufficient to call {@link #get(String)} using internally the default
 * type, evaluated by {@link #getType()}.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings("rawtypes")
public class Pool {

    private static final Log LOG = LogFactory.getLog(Pool.class);
    Map<String, IPRunnable> runnables;


    transient private String expressionPattern;

    private static Set<Class<? extends IPRunnable>> registeredTypes = new HashSet<>();
    
    public static void registerTypes(Class<? extends IPRunnable>...types) {
        for (int i = 0; i < types.length; i++) {
            if (!IPrefixed.class.isAssignableFrom(types[i]))
    		throw new IllegalArgumentException("type " + types[i] + " must implement IPrefixed!");
            registeredTypes.add(types[i]);
        }
    }
    
    private Map<String, IPRunnable> runnables() {
        if (runnables == null)
            loadRunnables();
        return runnables;
    }

    public String getFullExpressionPattern() {
    	if (registeredTypes.isEmpty())
    		throw new IllegalStateException("no types registered at pool. please register at least one runnable type!");
        if (expressionPattern == null) {
            StringBuilder buf = getExpressionPattern();
    		buf.append(".*");
            expressionPattern = buf.toString();
        }
        return expressionPattern;
    }

	public StringBuilder getExpressionPattern() {
		StringBuilder buf = new StringBuilder("[");
		for (Class<? extends IPRunnable> t : registeredTypes) {
		    buf.append(((IPrefixed)BeanClass.createInstance(t)).prefix());
		}
		buf.append("]");
		return buf;
	}
    
    public void saveAll() {
    	runnables().forEach( (k, v) -> add(k, v));
    }
    
    public void loadRunnables() {
        runnables = new HashMap<>();
        for (Class<? extends IPRunnable> type : registeredTypes) {
            String dirName = getDirectory(type);
            LOG.info("loading " + type.getSimpleName().toLowerCase() + "(s) from " + dirName);
            File dir = FileUtil.userDirFile(dirName);
            File[] runnableFiles = dir.listFiles();
            for (int i = 0; i < runnableFiles.length; i++) {
                try {
                	if (runnableFiles[i].getPath().endsWith(ENV.getFileExtension()))
                    loadRunnable(runnableFiles[i].getPath(), type);
                } catch (Exception e) {
                    LOG.error(e);
                }
            }
        }
    }

    public String getDirectory(Class<? extends IPRunnable> rType) {
        String dir = ENV.getConfigPathRel() + "specification/" + (rType != null ? rType.getSimpleName().toLowerCase() + "/" : "");
        FileUtil.userDirFile(dir).mkdirs();
        return dir;
    }

    private <I extends IPRunnable> I loadRunnable(String path, Class<I> type) {
        try {
            I r = ENV.load(path, type, false);
            runnables.put(getPrefixedName(r), r);
            return r;
        } catch (Exception e) {
       		ManagedException.forward(e);
            return null;
        }
    }

    private String getPrefixedName(IPRunnable r) {
		return ((IPrefixed)r).prefix() + r.getName();
	}

	/**
     * gets the runnable by name
     * 
     * @param name rule/query to find
     * @return rule/query or null
     */
    public IPRunnable get(String name) {
        return get(name, null);
    }

    /**
     * gets the runnable by name
     * 
     * @param name runnable to find
     * @param type runnable type
     * @return runnable or null
     */
    @SuppressWarnings("unchecked")
    public <I extends IPRunnable> I get(String name, Class<I> type) {
        I runnable = (I) runnables().get(name);
        if (runnable == null && type != null && !name.matches(getFullExpressionPattern())) {
        	String n = ((IPrefixed)BeanClass.createInstance(type)).prefix() + name;
        	runnable = (I) runnables().get(n);
        } else if (runnable == null && type == null && !name.matches(getFullExpressionPattern())) {
        	//search on all runnable types
        	StringBuilder pattern = getExpressionPattern();
			String prefixes = pattern.substring(1, pattern.length()-1);
			for (int i=0; i<prefixes.length(); i++) {
				runnable = (I) runnables().get(prefixes.charAt(i) + name);
				if (runnable != null)
					break;
			}
        }
        if (runnable == null && type == null)
        	throw new IllegalArgumentException("Wrong runnable name: " + name 
        	+ "\nPlease provide a runnable name with a prefix (expression: " + getFullExpressionPattern() 
        	+ ")\n\tor the type as one of:\n" + StringUtil.toFormattedString(registeredTypes, -1));
        //perhaps not loaded (new or recursive)
        return (I) (runnable != null ? runnable : loadRunnable(getFileName(name, type), type));
    }

    protected String getFileName(String name, Class<? extends IPRunnable> type) {
    	name = FileUtil.getValidFileName(name);
        return name.matches(".*[.][a-z]{3}") ? name : getDirectory(type) + name + ENV.getFileExtension();
    }

    /**
     * delegates to {@link #add(String, IPRunnable)} using {@link IPRunnable#getName()}
     */
    public void add(IPRunnable runnable) {
        add(getPrefixedName(runnable), runnable);
    }

    /**
     * adds the given runnable to the pool
     * 
     * @param name runnable name
     * @param runnable runnable to add
     */
    protected void add(String name, IPRunnable runnable) {
        runnables().put(name, runnable);
        String fileName = getFileName(runnable.getName(), runnable.getClass());
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
