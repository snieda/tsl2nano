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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.tools.ant.BuildEvent;
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

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

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
    public static final String TASK_SQL = "Sql";

    private static final Log LOG = LogFactory.getLog(AntRunner.class);

    /**
     * delegates to {@link #runTask(String, Map, FileSet...)} using {@link #createFileSets(String)} to create the
     * {@link FileSet}s.
     */
    public static void runTask(String name, Properties taskProperties, String fileSetExpression) {
        runTask(name, taskProperties, fileSetExpression != null ? createFileSets(fileSetExpression) : (FileSet[]) null);
    }

    /**
     * starts the task by name using its properties and perhaps some filesets.
     * <p/>
     * See <a href="https://ant.apache.org/manual/tasksoverview.html"> here for an overview of ants standard tasks</a>.
     * 
     * <pre>
     * Example:
     *   FileSet[] fileSets = AntRunner.createFileSets("./:{**\*.*ml}**\*.xml;" + basedir.getPath() + ":{*.txt}");
     *   Properties props = new Properties();
     *   props.put("destFile", new File(destFile));
     *   AntRunner.runTask("Jar", props, fileSets);
     * </pre>
     * 
     * @param name task name
     * @param taskProperties task properties
     * @param fileSets optional filesets (depends on the task!)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void runTask(String name, Map taskProperties, FileSet... fileSets) {
        Class<?> taskType = null;
        Task task = null;
        try {
            taskType = BeanClass.load(TASK_PATH + "." + name);
            task = (Task) taskType.newInstance();
        } catch (final Exception e) {
            ManagedException.forward(e);
        }
        task.setProject(new Project());
        task.getProject().setName(name);
        task.getProject().init();
        task.setTaskType("AntRunner." + taskType);
        task.setTaskName("AntRunner." + taskType);
        task.setOwningTarget(new Target());
        task.setLocation(new Location(System.getProperty("user.dir")));
        task.getProject().addBuildListener(createLogfileBuildListener());
        task.getProject().addBuildListener(createPipedAntBuildListener(new PipedOutputStream()));
        task.getProject().addBuildListener(createMessageListener());
        /*
         * now we use the properties to fill bean attributes of ant task
         */
        final Set<Object> keySet = taskProperties.keySet();
        for (final Object key : keySet) {
            final String n = (String) key;
            final Object v = taskProperties.get(n);
            BeanAttribute.getBeanAttributeWriter(task.getClass(), n, v.getClass()).setValue(task, v);
        }

        /*
         * optional filesets
         */
        if (!Util.isEmpty(fileSets)) {
            try { //try it directly through 'addFileset'
                final Method addFilesetMethod = taskType.getMethod("addFileset", new Class[] { FileSet.class });
                for (final FileSet fs : fileSets) {
                    addFilesetMethod.invoke(task, new Object[] { fs });
                    fs.setProject(task.getProject());
                }
            } catch (final Exception e) {
                if (task instanceof MatchingTask) {
                    final MatchingTask mtask = (MatchingTask) task;
                    for (final FileSet fs : fileSets) {
                        final Enumeration<FileSelector> fsEnum = fs.selectorElements();
                        fs.setProject(task.getProject());
                        while (fsEnum.hasMoreElements()) {
                            final FileSelector sel = fsEnum.nextElement();
                            mtask.add(sel);
                        }
                    }
                } else {
                    LOG.warn("The task '" + task.getClass().getName()
                        + "' is not a MatchingTask ==> given FileSets are ignored", e);
                }
            }
        }

        /*
         * start it
         */
        LOG.info("starting task " + taskType
            + " with properties:\n"
            + StringUtil.toFormattedString(taskProperties, 100, true));
        //TODO: FileSet throws NullPointers inside its toString() method!
//            + (fileSets != null && fileSets.length > 0 ? "\nfilesets:\n" + StringUtil.toFormattedString(fileSets, 100, true): ""));
        task.execute();
        LOG.info("build " + taskType + " successful");
    }

    /**
     * create System.out build listener
     * 
     * @return ant build listener
     */
    public static BuildListener createLogfileBuildListener() {
        return createBuildListener(LogFactory.getOut(), LogFactory.getErr());
    }

    /**
     * createPipedBuildListener
     * 
     * @param pipedOutputStream stream to be connected to a {@link PipedInputStream}.
     * @return ant build listener
     */
    public static BuildListener createPipedAntBuildListener(PipedOutputStream pipedOutputStream) {
        final PrintStream printStream = new PrintStream(pipedOutputStream);
        return createBuildListener(printStream, new PrintStream(printStream));
    }

    public static BuildListener createBuildListener(PrintStream out, PrintStream err) {
        final DefaultLogger consoleLogger = new DefaultLogger();
        consoleLogger.setOutputPrintStream(out);
        consoleLogger.setErrorPrintStream(err);
        consoleLogger.setMessageOutputLevel(LOG.isDebugEnabled() ? Project.MSG_DEBUG : Project.MSG_INFO);
        return consoleLogger;
    }

    public static BuildListener createMessageListener() {
        return new BuildListener() {

            @Override
            public void buildFinished(BuildEvent arg0) {
                Message.send("BUILD " + arg0.getProject().getName() + " FINISHED");
            }

            @Override
            public void buildStarted(BuildEvent arg0) {
                Message.send("BUILD " + arg0.getProject().getName() + " STARTED");
            }

            @Override
            public void messageLogged(BuildEvent arg0) {
                if (arg0.getException() != null)
                    Message.send(arg0.getException());
                else if (arg0.getPriority() <= 2)
                    Message.send(arg0.getMessage());
            }

            @Override
            public void targetFinished(BuildEvent arg0) {
                Message.send("BUILD TARGET " + arg0.getTarget().getName() + " FINISHED");
            }

            @Override
            public void targetStarted(BuildEvent arg0) {
                Message.send("BUILD TARGET " + arg0.getTarget().getName() + " STARTED");
            }

            @Override
            public void taskFinished(BuildEvent arg0) {
            }

            @Override
            public void taskStarted(BuildEvent arg0) {
            }
            
        };
    }
    
    /**
     * <directory-name>[:{[include][,<include>...]]}[[exclude][,<exclude>...]];...
     * 
     * @param expression
     * @return
     */
    public static FileSet[] createFileSets(String expression) {
        String[] fsets = expression.split(";");
        ArrayList<FileSet> fileSets = new ArrayList<FileSet>(fsets.length);
        String[] includes, excludes;
        String s;
        for (int i = 0; i < fsets.length; i++) {
            FileSet fileSet = new FileSet();
            fileSet.setDir(FileUtil.userDirFile(StringUtil.substring(fsets[i], null, ":{")).getAbsoluteFile());
            s = StringUtil.substring(fsets[i], "{", "}");
            includes = s.split(",");
            fileSet.appendIncludes(includes);
            s = StringUtil.substring(fsets[i], "}", null);
            if (s.length() > 0) {
                excludes = s.split(",");
                fileSet.appendExcludes(excludes);
            }
//            for (int e = 0; e < excludes.length; e++) {
//                FilenameSelector nameSelector = new FilenameSelector();
//                nameSelector.setName(excludes[e]);
//                fileSet.addFilename(nameSelector);
//            }
            fileSets.add(fileSet);
        }
        return (FileSet[]) fileSets.toArray(new FileSet[0]);
    }

    /**
     * convenience to run task {@link #TASK_REPLACE_REGEXP}
     * 
     * @param match
     * @param replace
     * @param dir
     * @param includes
     */
    public static void runRegexReplace(String match, String replace, String dir, String includes) {
        if (includes == null)
            includes = "**";
        Properties p = new Properties();
        p.put("match", match);
        p.put("replace", replace);
        AntRunner.runTask(AntRunner.TASK_REPLACE_REGEXP, p, dir + ":{" + includes + "}");
    }
}
