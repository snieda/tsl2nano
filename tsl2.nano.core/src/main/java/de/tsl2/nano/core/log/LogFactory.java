package de.tsl2.nano.core.log;

import java.io.File;
import java.io.PrintStream;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.serialize.XmlUtil;
import de.tsl2.nano.core.util.BitUtil;
import de.tsl2.nano.core.util.ConcurrentUtil;
import static  de.tsl2.nano.core.util.CLI.*;
import static  de.tsl2.nano.core.util.CLI.Color.*;
import de.tsl2.nano.core.util.StringUtil;

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
 * @author ts
 * @version $Revision$
 */
//using SimpleXml it is not possible to use the abstract class with anonymous implementation!
@Default(value = DefaultType.FIELD, required = false)
public/*abstract*/class LogFactory implements Runnable, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -1548678560499335157L;

    static LogFactory self;
    transient Queue<String> loggingQueue;
    Integer queueCapacity = Integer.MAX_VALUE;
    
    @ElementMap(attribute = true, inline = true, entry = "loglevel", key = "package", keyType = String.class, value = "level", valueType = Integer.class, required = false)
    Map<String, Integer> loglevels;

    @Attribute(required = false)
    static boolean printToConsole = true;
    
    transient PrintStream out = System.out;
    transient PrintStream err = System.err;

    /** a string formatting time, 'logClass', 'state', 'message' with {@link MessageFormat} */

    @Element(data = true)
    String outputformat = "%1$td.%1$tm.%1$tY %1$tT %2$s [%3$16s]: %4$s";
    @Element(required = false)
    String outputFile = logOutputFile;
    transient MsgFormat msgFormat;

    public static final int FATAL = 1;
    public static final int ERROR = 2;
    public static final int WARN = 4;
    public static final int INFO = 8;
    public static final int DEBUG = 16;
    public static final int TRACE = 32;

    public static final int LOG_ERROR = ERROR | FATAL;
    public static final int LOG_WARN = WARN | ERROR | FATAL;
    public static final int LOG_STANDARD = INFO | WARN | ERROR | FATAL;
    public static final int LOG_DEBUG = INFO | WARN | ERROR | FATAL | DEBUG;
    public static final int LOG_ALL = INFO | WARN | ERROR | FATAL | DEBUG | TRACE;

    static final String[] STATEDESCRIPTION = new String[] { "fatal", "error", "warn", "info", "debug", "trace" };
    static final String[] STATETXT = new String[] { tag("!", LIGHT_RED), tag("§", LIGHT_RED), tag("#", YELLOW), " ", "-", "=" };

    /** bit set of states to log. will be used in inner log class */
    @Attribute
    String standard;
    transient int statesToLog = LOG_STANDARD;
    transient int defaultPckLogLevel;

    /** whether to filter the output. means, it doesn't repeat last line elements */
    @Attribute
    private boolean useFilter = true;

    private static transient AtomicBoolean isPreparing = new AtomicBoolean(false);
    
    /**
     * used for formatted logging using outputformat as pattern. see {@link #log(Class, State, Object, Throwable)}. (-->
     * performance)
     */
    static final String apacheLogFactory = "org.apache.commons.logging.LogFactory";

    static String logFactoryXml = "logfactory" + (ENV.isAvailable() ? ENV.getFileExtension() : ".xml");
    static String logOutputFile = "logfactory.log";

    /**
     * singelton constructor
     */
    private LogFactory() {
    }

    protected static LogFactory instance() {
        if (self == null) {
        	isPreparing.set(true);
        	try {
            /*
             * while it not possible to use convenience method from other base classes,
             * because the all use this logfactory, we have to create some not-nice code.
             */
            if (new File(logFactoryXml).getAbsoluteFile().canRead()) {
                try {
                    self = XmlUtil.loadSimpleXml_(logFactoryXml, LogFactory.class, true);
                    if (self.loglevels == null) {
                        self.loglevels = new HashMap<String, Integer>();
                    }
                } catch (Throwable e) {//NoClassDefFound would be an error --> Throwable
                    //ok, we create the instance directly!
                    System.err.println(e);
//                    e.printStackTrace();
                }
            }
            if (self == null) {
                self = new LogFactory() /* on abstract: {} */;
                self.loggingQueue = new LinkedBlockingQueue<String>(self.queueCapacity);
                self.loglevels = new HashMap<String, Integer>();
                self.loglevels.put("de.tsl2.nano.core", LOG_ALL);
                self.msgFormat = new MsgFormat(self.outputformat);
                self.defaultPckLogLevel = BitUtil.highestOneBit(self.statesToLog);
                if (Boolean.getBoolean("tsl2.nano.logfactory.off")) {
                	System.out.println("LOGFACTORY IS OFF! <- system-property 'tsl2.nano.logfactory.off' is true");
                	self.queueCapacity = 1;
                	return self;
                }
                if (self.outputFile != null) {
                    self.initPrintStream(self.outputFile);
                }
                try {
                    XmlUtil.saveSimpleXml_(logFactoryXml, self);
                } catch (Throwable ex) {//NoClassDefFound would be an error --> Throwable
                    //ok, perhaps the simple-xml is loaded later
                    log("error: LogFactory couldn't save xml properties");
                }
            }
            ConcurrentUtil.startDaemon("tsl2-logger-" + self.outputFile, self);
            Runtime.getRuntime().addShutdownHook(Executors.defaultThreadFactory().newThread(new Runnable() {
                @Override
                public void run() {
                        self.logMessages();
                }
            }));
        	} finally {
        		isPreparing.set(false);
        		System.out.println("<== Logfactory is READY!");
        	}
        }
        return self;
    }

    public static boolean isInitialized() {
    	return self != null && self.loglevels != null;
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
    	if (!isPreparing.get())
    		stop();
    	else
    		log("WARN: shouldn't reset LogFactory in preparing mode!");
        logFactoryXml = logConfiguration;
    }

    /**
     * only for internal use
     * 
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
            //create the print-stream with autoflush = false
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
            if ("trace".equals(logLevel)) {
                enableTrace();
            } else if ("debug".equals(logLevel)) {
                instance().statesToLog = LOG_DEBUG;
            } else if ("warn".equals(logLevel)) {
                instance().statesToLog = LOG_WARN;
            }
        } else {
            instance().statesToLog = bitsetStatesToLog;
        }
    }

    private static void enableTrace() {
        instance().statesToLog = LOG_ALL;
        Runtime.getRuntime().traceInstructions(true);
        Runtime.getRuntime().traceMethodCalls(true);
    }

    /**
     * @return Returns the out.
     */
    public static PrintStream getOut() {
        return self.out;
    }
    
    public static synchronized boolean isPrintToConsole() {
        return printToConsole;
    }
    
    public static synchronized void setPrintToConsole(boolean toStandard) {
        printToConsole = toStandard;
    }
    
    /** asks a system log level "tsl2.nano.log.level". not only for the logger classes but for some system.out , too. see #class {@link LogFactory} */
    public static final boolean isDebugLevel() {
    	return "debug".equals(System.getProperty("tsl2.nano.log.level"));
    }

    /** asks a system log level "tsl2.nano.log.level". not only for the logger classes but for some system.out , too. see #class {@link LogFactory} */
    public static final boolean isWarnLevel() {
    	return "warn".equals(System.getProperty("tsl2.nano.log.level"));
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
        loggingQueue = new LinkedBlockingQueue<String>(queueCapacity);
        if (outputFile != null) {
            initPrintStream(outputFile);
        }
    }

    /**
     * initPrintStream
     * 
     * @param outputFile
     */
    private void initPrintStream(String outputFile) {
        try {
            File absoluteFile = new File(outputFile).getAbsoluteFile();
			absoluteFile.createNewFile();
            PrintStream outFile = new PrintStream(absoluteFile);
            out = outFile;
            err = outFile;
        } catch (Exception e) {
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
        if (BitUtil.hasBit(loglevel, TRACE))
            enableTrace();
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
        String path = logClass.getPackage() != null ? logClass.getPackage().getName() : "";
        return hasLogLevel(path, level);
    }

    private final boolean hasLogLevel(String path, int level) {
        if (!loglevels.containsKey(path)) {
            if (path.indexOf('.') == -1) {
                //TODO: create default path levels to enhance performance
                return true;//level <= defaultPckLogLevel;//minimum default level
            } else {
                return hasLogLevel(StringUtil.substring(path, null, ".", true), level);
            }
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
            @Override
            public void warn(Object arg0, Throwable arg1) {
                log(logClass, WARN, tag(arg0, ORANGE), arg1);
            }

            @Override
            public void warn(Object arg0) {
                log(logClass, WARN, tag(arg0, ORANGE), null);
            }

            @Override
            public void trace(Object arg0, Throwable arg1) {
                log(logClass, TRACE, arg0, arg1);
            }

            @Override
            public void trace(Object arg0) {
                log(logClass, TRACE, arg0, null);
            }

            @Override
            public boolean isWarnEnabled() {
                return LogFactory.isEnabled(WARN);
            }

            @Override
            public boolean isTraceEnabled() {
                return LogFactory.isEnabled(TRACE);
            }

            @Override
            public boolean isInfoEnabled() {
                return LogFactory.isEnabled(INFO);
            }

            @Override
            public boolean isFatalEnabled() {
                return LogFactory.isEnabled(FATAL);
            }

            @Override
            public boolean isErrorEnabled() {
                return LogFactory.isEnabled(ERROR);
            }

            @Override
            public boolean isDebugEnabled() {
                return LogFactory.isEnabled(DEBUG);
            }

            @Override
            public void info(Object arg0, Throwable arg1) {
                log(logClass, INFO, arg0, arg1);
            }

            @Override
            public void info(Object arg0) {
                log(logClass, INFO, arg0, null);
            }

            @Override
            public void fatal(Object arg0, Throwable arg1) {
                log(logClass, FATAL, tag(arg0, LIGHT_RED), arg1);
            }

            @Override
            public void fatal(Object arg0) {
                log(logClass, FATAL, arg0, null);
            }

            @Override
            public void error(Object arg0, Throwable arg1) {
                log(logClass, ERROR, tag(arg0, LIGHT_RED), arg1);
            }

            @Override
            public void error(Object arg0) {
                log(logClass, ERROR, arg0, null);
            }

            @Override
            public void debug(Object arg0, Throwable arg1) {
                log(logClass, DEBUG, arg0, arg1);
            }

            @Override
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
    public static boolean isEnabled(int state) {
        return BitUtil.hasBit(instance().statesToLog, state);
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
        while (LogFactory.self != null) {
            try {
                logMessages();
                Thread.sleep(200);
            } catch (InterruptedException e) {
                out.println(e.toString());
            }
        }
        logMessages();
        if (out != null && !out.equals(System.out)) {
            out.flush();
            out.close();
        }
    }
    void logMessages() {
        String txt, last = " ", text;
        int filter, lastFilter = 0;
        while ((text = loggingQueue.poll()) != null) {
            if (useFilter) {
                filter = filterIndex(text, last);
                if (filter < lastFilter) {
                    filter = 0;
                } else if (filter >= text.length() - 1) {
                    filter = 0;
                    continue;
                }
                txt = filter > 0 ? "\t" + text.substring(filter) : text;
                lastFilter = filter;
                last = text;
            } else {
                txt = text;
            }
            out.println(txt);
            if (isPrintToConsole() && out != System.out) {
                System.out.println(txt);
            }
        }
    }

    private int filterIndex(String newText, String lastText) {
        int i;
        int len = Math.min(newText.length(), lastText.length());
        for (i = 0; i < len; i++) {
            if (lastText.charAt(i) == newText.charAt(i)) {
                continue;
            }
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
        if (Boolean.getBoolean("tsl2.nano.logfactory.off") && ex == null) {
        	return;
        } else if (isPreparing.get()) {
    		System.out.println("[LOGPREPARE] " + logClass.getSimpleName() + ": " + message + (ex != null ? " " + ex : ""));
    		return;
    	} else if (message != null && message.toString() != null && message.toString().length() > 16000) {
    		message = StringUtil.toStringCut(message, 120) + "...";
    	}
    		
        final LogFactory factory = instance();
        if (LogFactory.isEnabled(state) && factory.hasLogLevel(logClass, state)) {
            if (message != null) {
                //TODO: evaluate performance of predefined pattern in MessageFormat (MsgFormat)
                // is there a fast way doing the formatting inside the other thread?
                String f = String.format(factory.outputformat,
                    Calendar.getInstance().getTime(),
                    state(state),
                    logClass.getSimpleName(),
                    message);
                factory.loggingQueue.add(f);
            }
            //on errors, we don't use the queuing thread
            if (ex != null) {
                try {
                    if (printToConsole)
                        ex.printStackTrace(factory.err);
                } catch (Exception e) {
                    //don't stop the flow on logging problems!
                    ex.printStackTrace();
                    e.printStackTrace();
                }
                if (printToConsole && factory.err != System.err) {
                    ex.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * convenience to avoid creating log members on each class.
     * logger
     * @param obj any instance to log
     * @return logger for given instance
     */
    public static final Log logger(Object obj) {
        return logger(obj.getClass());
    }
    /**
     * convenience to avoid creating log members on each class.
     * logger
     * @param cls any class to log
     * @return logger for given instance
     */
    public static final Log logger(Class<?> cls) {
        return getLog(cls);
    }
}
