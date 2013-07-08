/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Oct 7, 2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */
package de.tsl2.nano.execution;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.resources.Files;
import org.apache.tools.ant.types.selectors.FileSelector;

import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.util.StringUtil;
import de.tsl2.nano.util.bean.BeanClass;

/**
 * is able to run ant targets given by name and properties. If task is a {@link MatchingTask}, an array of
 * {@link FileSet} can be given.
 * <p>
 * please have a look at the setter methods of the task to evaluate the properties to set. the properties are case
 * sensitive.<br>
 * if use the task 'Expand', the class <code> org.apache.tools.ant.taskdefs.Expand</code> will be loaded and
 * instantiated. you must set the property 'destFile' with java.io.File. the AntRunner will call
 * Expand.setDestFile(File).
 * <p>
 * see @link{http://ant.apache.org/manual} for a list of standard ant tasks.
 * <p>
 * short list of tasks:<br>
 * Ant AntCall ANTLR AntStructure AntVersion Apply/ExecOn Apt Attrib Augment Available Basename Bindtargets BuildNumber
 * BUnzip2 BZip2 Cab Continuus/Synergy Tasks CvsChangeLog Checksum Chgrp Chmod Chown Clearcase Tasks Componentdef Concat
 * Condition Supported conditions Copy Copydir Copyfile Cvs CVSPass CvsTagDiff CvsVersion Defaultexcludes Delete Deltree
 * Depend Dependset Diagnostics Dirname Ear Echo Echoproperties EchoXML EJB Tasks Exec Fail Filter FixCRLF FTP GenKey
 * Get GUnzip GZip Hostinfo Image Import Include Input Jar Jarlib-available Jarlib-display Jarlib-manifest
 * Jarlib-resolve Java Javac JavaCC Javadoc/Javadoc2 Javah JDepend JJDoc JJTree Jlink JspC JUnit JUnitReport Length
 * LoadFile LoadProperties LoadResource Local MacroDef Mail MakeURL Manifest ManifestClassPath MimeMail Mkdir Move
 * Native2Ascii NetRexxC Nice Parallel Patch PathConvert Perforce Tasks PreSetDef ProjectHelper Property PropertyFile
 * PropertyHelper Pvcs Record Rename RenameExtensions Replace ReplaceRegExp ResourceCount Retry RExec Rmic Rpm
 * SchemaValidate Scp Script Scriptdef Sequential ServerDeploy Setproxy SignJar Sleep SourceOffSite Sound Splash Sql
 * Sshexec Sshsession Subant Symlink Sync Tar Taskdef Telnet Tempfile Touch Translate Truncate TStamp Typedef Unjar
 * Untar Unwar Unzip Uptodate Microsoft Visual SourceSafe Tasks Waitfor War WhichResource Weblogic JSP Compiler
 * XmlProperty XmlValidate XSLT/Style Zip
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class AntRunner {
    public static final String TASK_PATH = Jar.class.getPackage().getName();
    public static final String TASK_JAR = "Jar";
    public static final String TASK_ZIP = "Zip";
    /** usefull to unzip files. see {@link Expand} */
    public static final String TASK_UNJAR = "Expand";
    public static final String TASK_COPY = "Copy";
    public static final String TASK_MOVE = "Move";
    public static final String TASK_DELETE = "Delete";
    /** useful to list and search files. see {@link Files} */
    public static final String TASK_FILES = "Files";
    public static final String TASK_REPLACE_REGEXP = "optional.ReplaceRegExp";
    public static final String TASK_XSLT = "XSLTProcess";

    private static final Log LOG = LogFactory.getLog(AntRunner.class);

    /**
     * starts the task by name using its properties and perhaps some filesets.
     * 
     * @param name task name
     * @param taskProperties task properties
     * @param fileSets optional filesets (depends on the task!)
     */
    public static void runTask(String name, Properties taskProperties, FileSet... fileSets) {
        Class<?> taskType = null;
        Task task = null;
        try {
            taskType = Class.forName(TASK_PATH + "." + name);
            task = (Task) taskType.newInstance();
        } catch (final Exception e) {
            ForwardedException.forward(e);
        }
        task.setProject(new Project());
        task.getProject().init();
        task.setTaskType("AntRunner." + taskType);
        task.setTaskName("AntRunner." + taskType);
        task.setOwningTarget(new Target());
        task.setLocation(new Location(System.getProperty("user.home")));
        task.getProject().addBuildListener(createSystemOutBuildListener());
        task.getProject().addBuildListener(createPipedAntBuildListener(new PipedOutputStream()));

        /*
         * now we use the properties to fill bean attributes of ant task
         */
        final Set<Object> keySet = taskProperties.keySet();
        for (final Object key : keySet) {
            final String n = (String) key;
            final BeanClass bc = new BeanClass(taskType);
            bc.setValue(task, n, taskProperties.get(n));
        }

        /*
         * optional filesets
         * NOT YET WORKING!
         */
        if (task instanceof MatchingTask) {
            final MatchingTask mtask = (MatchingTask) task;
            for (final FileSet fs : fileSets) {
                final Enumeration<FileSelector> fsEnum = fs.selectorElements();
                while (fsEnum.hasMoreElements()) {
                    final FileSelector sel = fsEnum.nextElement();
                    mtask.add(sel);
                }
            }
        } else if (fileSets.length > 0) {
            try { //try it directly through 'addFileset'
                final Method addFilesetMethod = taskType.getMethod("addFileset", new Class[] { FileSet.class });
                for (final FileSet fs : fileSets) {
                    addFilesetMethod.invoke(task, new Object[] { fs });
                }
            } catch (final Exception e) {
                LOG.warn("The task '" + task.getClass().getName()
                    + "' is not a MatchingTask ==> given FileSets are ignored", e);
            }
        }

        /*
         * start it
         */
        LOG.info("starting task " + taskType
            + " with properties:\n"
            + StringUtil.toFormattedString(taskProperties, 100, true)
            + (fileSets.length > 0 ? "\nfilesets:\n"/* + StringUtil.toFormattedString(fileSets, 100, true)*/: ""));
        task.execute();
        LOG.info("build " + taskType + " successful");
    }

    /**
     * create System.out build listener
     * 
     * @return ant build listener
     */
    public static BuildListener createSystemOutBuildListener() {
        final DefaultLogger consoleLogger = new DefaultLogger();
        consoleLogger.setErrorPrintStream(System.err);
        consoleLogger.setOutputPrintStream(System.out);
        consoleLogger.setMessageOutputLevel(true/*LOG.isDebugEnabled()*/? Project.MSG_DEBUG : Project.MSG_INFO);
        return consoleLogger;
    }

    /**
     * createPipedBuildListener
     * 
     * @param pipedOutputStream stream to be connected to a {@link PipedInputStream}.
     * @return ant build listener
     */
    public static BuildListener createPipedAntBuildListener(PipedOutputStream pipedOutputStream) {
        final DefaultLogger consoleLogger = new DefaultLogger();
        final PrintStream printStream = new PrintStream(pipedOutputStream);
        consoleLogger.setErrorPrintStream(new PrintStream(printStream));
        consoleLogger.setOutputPrintStream(printStream);
        consoleLogger.setMessageOutputLevel(true/*LOG.isDebugEnabled()*/? Project.MSG_DEBUG : Project.MSG_INFO);
        return consoleLogger;
    }
}
