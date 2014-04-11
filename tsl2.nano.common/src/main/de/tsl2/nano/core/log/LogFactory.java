package de.tsl2.nano.core.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.classloader.ThreadUtil;
import de.tsl2.nano.core.util.BitUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.XmlUtil;

/**
 * simple logfactory implementation to enable use of common logging interfaces. useful if apache logging is not
 * supported (example: android platform) - to be extended or for testing purpose. provides no configurations. not
 * performance optimized!
 * <p/>
 * please overwrite {@link #log(Class, State, Object, Throwable)} to change the log output.
 * <p/>
 * <p/>
 * to en-disable debugging or tracing on application start (not reading from configuration), set the system property
 * 'tsl2.nano.log.level' to 'trace', 'debug' or 'warn'.
 * 
 * <pre>
 * Example:
 * java -Dtsl2.nano.log.level=debug ...
 * </pre>
 * 
 * TODO: make singelton, add simple configuration (Map<package, logstate>) and serialization, read filename from
 * system-properties
 * 
 * @author ts
 * @version $Revision$
 */
//using SimpleXml it is not possible to use the abstract class with anonymous implementation!
@Default(value = DefaultType.FIELD, required = false)
public/*abstract*/class LogFactory implements Runnable, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -1548678560499335157L;

    static LogFactory self;
    transient List<String> loggingQueue = Collections.synchronizedList(new LinkedList<String>());

    @ElementMap(attribute = true, inline = true, entry = "loglevel", key = "package", keyType = String.class, value = "level", valueType = Integer.class, required = false)
    Map<String, Integer> loglevels;

    transient PrintStream out = System.out;
    transient PrintStream err = System.err;

    /** a string formatting time, 'logClass', 'state', 'message' with {@link MessageFormat} */

    @Element(data = true)
    String outputformat = "%1$td.%1$tm.%1$tY %1$tT %2$s [%3$16s]: %4$s";
    @Element(required=false)
    String outputFile = logOutputFile;
    transient MsgFormat msgFormat;

    public static final int FATAL = 1;
    public static final int ERROR = 2;
    public static final int WARN = 4;
    public static final int INFO = 8;
    public static final int DEBUG = 16;
    public static final int TRACE = 32;

    public static final int LOG_WARN = WARN | ERROR | FATAL;
    public static final int LOG_STANDARD = INFO | WARN | ERROR | FATAL;
    public static final int LOG_DEBUG = INFO | WARN | ERROR | FATAL | DEBUG;
    public static final int LOG_ALL = INFO | WARN | ERROR | FATAL | DEBUG | TRACE;

    static final String[] STATEDESCRIPTION = new String[] { "fatal", "error", "warn", "info", "debug", "trace" };
    static final String[] STATETXT = new String[] { "!", "§", "#", " ", "-", "=" };

    /** bit set of states to log. will be used in inner log class */
    @Attribute
    String standard;
    transient int statesToLog = LOG_STANDARD;
    transient int defaultPckLogLevel;

    /** whether to filter the output. means, it doesn't repeat last line elements */
    @Attribute
    private boolean useFilter = true;

    /**
     * used for formatted logging using outputformat as pattern. see {@link #log(Class, State, Object, Throwable)}. (-->
     * performance)
     */
    static final String apacheLogFactory = "org.apache.commons.logging.LogFactory";

    static String logFactoryXml = "logfactory.xml";
    static String logOutputFile = "logfactory.log";

    /**
     * singelton constructor
     */
    private LogFactory() {
    }

    protected static LogFactory instance() {
        if (self == null) {
            /*
             * while it not possible to use convenience method from other base classes,
             * because the all use this logfactory, we have to create some not-nice code.
             */
            if (new File(logFactoryXml).canRead())
                try {
                    self = XmlUtil.loadSimpleXml_(logFactoryXml, LogFactory.class);
                    if (self.loglevels == null)
                        self.loglevels = new HashMap<String, Integer>();
                } catch (Throwable e) {//NoClassDefFound would be an error --> Throwable
                    //ok, we create the instance directly!
                    System.err.println(e);
//                    e.printStackTrace();
                }
            if (self == null) {
                self = new LogFactory() /* on abstract: {} */;
                self.loglevels = new HashMap<String, Integer>();
                self.loglevels.put("de.tsl2.nano.core", LOG_ALL);
                self.msgFormat = new MsgFormat(self.outputformat);
                self.defaultPckLogLevel = BitUtil.highestOneBit(self.statesToLog);
                if (self.outputFile != null) {
                    self.initPrintStream(self.outputFile);
                }
                try {
                    XmlUtil.saveSimpleXml_(logFactoryXml, self);
                } catch(Throwable ex) {//NoClassDefFound would be an error --> Throwable
                    //ok, perhaps the simple-xml is loaded later
                    log("error: LogFactory couldn't save xml properties");
                }
            }
            ThreadUtil.startDaemon("logger", self);
        }
        return self;
    }

    public static void stop() {
        //this will stop the thread, too.
        self = null;
    }
 
    /**
     * only for internal use! will reset the current singelton instance.
     * 
     * @param logConfiguration
     */
    public static void setLogFactoryXml(String logConfiguration) {
        self = null;
        logFactoryXml = logConfiguration;
    }

    /**
     * only for internal use
     * @return logfile name
     */
    public static String getLogFileName() {
        return self.outputFile;
    }
    
    /**
     * only for internal use!
     * 
     * @param logConfiguration
     */
    public static void setLogFile(String logFilename) {
        logOutputFile = logFilename;
    }

    public static final void initializeFileLogger(String fileName, int bitsetStatesToLog) {
        try {
            PrintStream fileOut = new PrintStream(fileName);
            initializeLogger(instance().outputformat, -1, fileOut, fileOut);
            self.outputFile = fileName;
        } catch (Exception e) {
            ManagedException.forward(e);
        }
    }

    public static final void initializeLogger(int bitsetStatesToLog) {
        initializeLogger(instance().outputformat, -1, instance().out, instance().err);
    }

    public static final void initializeLogger(String outputFormat,
            int bitsetStatesToLog,
            PrintStream output,
            PrintStream error) {
        instance().outputformat = outputFormat;
        instance().msgFormat.applyPattern(outputFormat);
        instance().statesToLog = bitsetStatesToLog;
        instance().out = output;
        instance().err = error;
        if (bitsetStatesToLog == -1) {
            String logLevel = System.getProperty("tsl2.nano.log.level");
            if ("trace".equals(logLevel))
                instance().statesToLog = LOG_ALL;
            else if ("debug".equals(logLevel))
                instance().statesToLog = LOG_DEBUG;
            else if ("warn".equals(logLevel))
                instance().statesToLog = LOG_WARN;
        } else {
            instance().statesToLog = bitsetStatesToLog;
        }
    }

    /**
     * @return Returns the out.
     */
    public static PrintStream getOut() {
        return self.out;
    }

    /**
     * @return Returns the err.
     */
    public static PrintStream getErr() {
        return self.err;
    }

    @Persist
    private void initSerialization() {
        standard = description(statesToLog);
    }

    @Commit
    private void initDeserializing() {
        statesToLog = BitUtil.bits(standard, Arrays.asList(STATEDESCRIPTION));
        if (outputFile != null) {
            initPrintStream(outputFile);
        }
    }

    /**
     * initPrintStream
     * @param outputFile 
     */
    private void initPrintStream(String outputFile) {
        try {
            PrintStream outFile = new PrintStream(outputFile);
            out = outFile;
            err = outFile;
        } catch (FileNotFoundException e) {
            //the logger shouldn't stop the application...
            err.println(e);
        }
    }

    /**
     * setLogLevel
     * 
     * @param loglevel bit field: {@link #FATAL}, {@link #ERROR}, {@link #WARN}, {@link #INFO}, {@link #DEBUG},
     *            {@link #TRACE}.
     */
    public static final void setLogLevel(int loglevel) {
        instance().statesToLog = loglevel;
    }

    /**
     * setLogLevel
     * 
     * @param packagePath package path to set the log level for.
     * @param loglevel bit field: {@link #FATAL}, {@link #ERROR}, {@link #WARN}, {@link #INFO}, {@link #DEBUG},
     *            {@link #TRACE}.
     */
    public final void setLogLevel(String packagePath, int loglevel) {
        loglevels.put(packagePath, loglevel);
    }

    private final boolean hasLogLevel(Class<?> logClass, int level) {
        String path = logClass.getPackage().getName();
        return hasLogLevel(path, level);
    }

    private final boolean hasLogLevel(String path, int level) {
        if (!loglevels.containsKey(path))
            if (path.indexOf('.') == -1) {
                //TODO: create default path levels to enhance performance
                return true;//level <= defaultPckLogLevel;//minimum default level
            } else {
                return hasLogLevel(StringUtil.substring(path, null, ".", true), level);
            }
        return loglevels.get(path) >= level;
    }

    private static final String state(int loglevel) {
        return STATETXT[BitUtil.highestBitPosition(loglevel)];
    }

    private static final String description(int loglevel) {
        return BitUtil.description(loglevel, Arrays.asList(STATEDESCRIPTION));
    }

    @SuppressWarnings({ "rawtypes" })
    public static final org.apache.commons.logging.Log getLog(final Class logClass) {
        //if an apache logger is available, we use it.
//        if (Environment.isAvailable() && Environment.get(CompatibilityLayer.class).isAvailable(apacheLogFactory))
//            return (org.apache.commons.logging.Log) Environment.get(CompatibilityLayer.class)
//                .runOptional(apacheLogFactory, "getLog", new Class[] { Class.class }, logClass);

        return new org.apache.commons.logging.Log() {

            /*
             * not using override annotations to be usable on jdk 1.5 platforms!
             */
            public void warn(Object arg0, Throwable arg1) {
                log(logClass, WARN, arg0, arg1);
            }

            public void warn(Object arg0) {
                log(logClass, WARN, arg0, null);
            }

            public void trace(Object arg0, Throwable arg1) {
                log(logClass, TRACE, arg0, arg1);
            }

            public void trace(Object arg0) {
                log(logClass, TRACE, arg0, null);
            }

            public boolean isWarnEnabled() {
                return instance().isEnabled(WARN);
            }

            public boolean isTraceEnabled() {
                return instance().isEnabled(TRACE);
            }

            public boolean isInfoEnabled() {
                return instance().isEnabled(INFO);
            }

            public boolean isFatalEnabled() {
                return instance().isEnabled(FATAL);
            }

            public boolean isErrorEnabled() {
                return instance().isEnabled(ERROR);
            }

            public boolean isDebugEnabled() {
                return instance().isEnabled(DEBUG);
            }

            public void info(Object arg0, Throwable arg1) {
                log(logClass, INFO, arg0, arg1);
            }

            public void info(Object arg0) {
                log(logClass, INFO, arg0, null);
            }

            public void fatal(Object arg0, Throwable arg1) {
                log(logClass, FATAL, arg0, arg1);
            }

            public void fatal(Object arg0) {
                log(logClass, FATAL, arg0, null);
            }

            public void error(Object arg0, Throwable arg1) {
                log(logClass, ERROR, arg0, arg1);
            }

            public void error(Object arg0) {
                log(logClass, ERROR, arg0, null);
            }

            public void debug(Object arg0, Throwable arg1) {
                log(logClass, DEBUG, arg0, arg1);
            }

            public void debug(Object arg0) {
                log(logClass, DEBUG, arg0, null);
            }
        };
    }

    /**
     * isEnabled
     * 
     * @param state
     * @return
     */
    protected boolean isEnabled(int state) {
        return BitUtil.hasBit(statesToLog, state);
    }

    private static final Exception STACKTRACER = new Exception();

    protected static String getCaller() {
        StackTraceElement[] st = STACKTRACER.getStackTrace();
        return st.length > 2 ? st[2].toString() : "<unknown>";
    }

    /**
     * simple delegate to {@link #log(Class, State, Object, Throwable)}
     */
    public static void log(Object message) {
        log(LogFactory.class, INFO, message, null);
    }

    /**
     * simple delegate to {@link #log(Class, State, Object, Throwable)}
     */
    protected static void log(Class<?> logClass, Object message) {
        log(logClass, INFO, message, null);
    }

    @Override
    public void run() {
        String txt, last = " ", text;
        int filter, lastFilter = 0;
        while (LogFactory.self != null || loggingQueue.size() > 0) {
            if (loggingQueue.size() > 0) {
                text = loggingQueue.remove(0);
                if (useFilter) {
                    filter = filterIndex(text, last);
                    if (filter < lastFilter) {
                        filter = 0;
                    }
                    txt = filter > 0 ? "\t" + text.substring(filter) : text;
                    lastFilter = filter;
                    last = text;
                } else {
                    txt = text;
                }
                out.println(txt);
                if (out != System.out)
                    System.out.println(txt);
            } else {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    ManagedException.forward(e);
                }
            }
        }

    }

    private int filterIndex(String newText, String lastText) {
        int i;
        int len = Math.min(newText.length(), lastText.length());
        for (i = 0; i < len; i++) {
            if (lastText.charAt(i) == newText.charAt(i))
                continue;
            break;
        }
        return i;
    }

    /**
     * main log method. simply logs to {@link System#out} and {@link System#err}. please override this method to change
     * your output.
     * 
     * @param logClass logging class
     * @param state see {@link #STATETXT} and {@link #INFO}, {@link #WARN}, etc.
     * @param message text to log
     * @param ex (optional) exception to log
     */
    protected static void log(Class<?> logClass, int state, Object message, Throwable ex) {
        final LogFactory factory = instance();
        if (factory.isEnabled(state) && factory.hasLogLevel(logClass, state)) {
            if (message != null) {
                //TODO: evaluate performance of predefined pattern in MessageFormat (MsgFormat)
                String f = String.format(factory.outputformat,
                    Calendar.getInstance().getTime(),
                    state(state),
                    logClass.getSimpleName(),
                    message);
                factory.loggingQueue.add(f);
            }
            //on errors, we don't use the queuing thread
            if (ex != null) {
                ex.printStackTrace(factory.err);
                if (factory.err != System.err) {
                    ex.printStackTrace(System.err);
                }
            }
        }
    }

}
