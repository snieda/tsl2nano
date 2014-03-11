/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: May 7, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.execution;

import java.io.File;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;

/**
 * Util to start system executables
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class ScriptUtil extends SystemUtil {
    /**
     * starts an ant script
     * 
     * @param filePath build file
     * @param target target
     * @param properties properties
     * @return true, if build was successful
     */
    public static final boolean ant(String filePath, String target, Properties properties) {
        try {
            antbuild(filePath, target, properties, null);
            return true;
        } catch (final Exception e) {
            //TODO: refactore: return boolean --> void
            ManagedException.forward(e);
            return false;
        }
    }

    public static final void antbuild(String filePath, String target, Properties properties, BuildListener buildListener) {
//        for (Object key : properties.keySet()) {
//            System.setProperty(key.toString(), properties.getProperty(key.toString()));
//        }
//        Launcher.main(new String[] { "-f", filePath, target });

        final File buildFile = new File(filePath);
        final Project p = new Project();
//        boolean t = true;
//        Path path = null;
//        AntClassLoader ACL = new AntClassLoader(p, path, t);

        p.setUserProperty("ant.file", buildFile.getAbsolutePath());
        //logging
        if (buildListener == null) {
            buildListener = AntRunner.createLogfileBuildListener();
        }
        p.addBuildListener(buildListener);
        try {
            p.fireBuildStarted();
            p.init();
            for (final Object key : properties.keySet()) {
                p.setProperty(key.toString(), properties.getProperty(key.toString()));
            }
            /*
             * TODO: setting the classloader doesn't work yet. you have to set the classpath
             * through 'path' in your ant script!
             */
//            p.createClassLoader((ClassLoader)properties.get("classloader"), new Path(p));
            if (properties.get("classloader") != null) {
                p.setCoreLoader((ClassLoader) properties.get("classloader"));
            } else {
                p.setCoreLoader(Thread.currentThread().getContextClassLoader());
            }
            final ProjectHelper helper = ProjectHelper.getProjectHelper();
            helper.parse(p, buildFile);
            p.addReference("ant.projectHelper", helper);
            target = target != null ? target : p.getDefaultTarget();
            p.setSystemProperties();
            p.setName(buildFile.getName());

            LOG.info("starting ant script: " + buildFile.getAbsolutePath()
                + "\ntarget: "
                + target
                + "\nbasedir: "
                + p.getBaseDir()
                + "\nproperties:\n"
                + StringUtil.toFormattedString(properties, 100));
            if (LOG.isDebugEnabled()) {
                LOG.debug("\ncoreloader: " + p.getCoreLoader()
                    + "\nexecutor: "
                    + p.getExecutor()
                    + "\nproperties: "
                    + p.getProperties()
                    + "\nreferences: "
                    + p.getReferences()
                    + "\nuser-properties: "
                    + p.getUserProperties());
            }
            p.executeTarget(target);
            p.fireBuildFinished(null);
        } catch (final BuildException e) {
            if (p.getReference("ant.projectHelper") == null) {
                LOG.info("problem starting ant script: " + buildFile.getAbsolutePath()
                    + "\ntarget: "
                    + target
                    + "\nbasedir: "
                    + p.getBaseDir()
                    + "\nproperties:\n"
                    + StringUtil.toFormattedString(properties, 100));
            }
            p.fireBuildFinished(e);
            ManagedException.forward(e);
        }
    }

    public static final void main(String[] args) {
        if (args.length < 2) {
            System.out.println("scriptutil start: <build-target of shell.xml>");
        }
        Properties p = new File("shell.properties").canRead() ? FileUtil.loadProperties("shell.properties",
            Thread.currentThread().getContextClassLoader()) : new Properties();
        ant("shell.xml", args[1], p);
    }
}
