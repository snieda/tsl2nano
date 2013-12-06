package de.tsl2.nano.log;

import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import de.tsl2.nano.classloader.ThreadUtil;
//import de.tsl2.nano.Environment;
import de.tsl2.nano.exception.ForwardedException;
//import de.tsl2.nano.execution.CompatibilityLayer;
import de.tsl2.nano.util.NumberUtil;
import de.tsl2.nano.util.StringUtil;

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
public abstract class LogFactory implements Runnable {
    static LogFactory self;
    List<String> loggingQueue = Collections.synchronizedList(new LinkedList<String>());

    final Map<String, Integer> loglevels = new HashMap<String, Integer>();

    PrintStream out = System.out;
    PrintStream err = System.err;

    /** a string formatting time, 'logClass', 'state', 'message' with {@link MessageFormat} */
    String outputformat = "%1$td:%1$tm:%1$tY %1$tT %2$s [%3$16s]: %4$s";
    final MsgFormat msgFormat = new MsgFormat(outputformat);

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

//    static final String[] STATETXT = new String[] { "info", "warn", "error", "fatal", "debug", "trace" };
    static final String[] STATETXT = new String[] { "§", "§", "#", " ", "-", "-" };

    /** bit set of states to log. will be used in inner log class */
    int statesToLog = LOG_STANDARD;
    int defaultPckLogLevel = NumberUtil.highestOneBit(statesToLog);

    /**
     * used for formatted logging using outputformat as pattern. see {@link #log(Class, State, Object, Throwable)}. (-->
     * performance)
     */
    static final String apacheLogFactory = "org.apache.commons.logging.LogFactory";

    /**
     * singelton constructor
     */
    private LogFactory() {
    }
    
    protected static LogFactory instance() {
        if (self == null) {
            self = new LogFactory() {
            };
            ThreadUtil.startDaemon("logger", self);
        }
        return self;
    }

    public static final void initializeFileLogger(String fileName, int bitsetStatesToLog) {
        try {
            //TODO use PipeReader to write to console, too
            initializeLogger(instance().outputformat, -1, instance().out, instance().err);
        } catch (Exception e) {
            ForwardedException.forward(e);
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
        return STATETXT[NumberUtil.highestBitPosition(loglevel)];
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
        return NumberUtil.hasBit(statesToLog, state);
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
        log(null, INFO, message, null);
    }

    /**
     * simple delegate to {@link #log(Class, State, Object, Throwable)}
     */
    protected static void log(Class<?> logClass, Object message) {
        log(logClass, INFO, message, null);
    }

    @Override
    public void run() {
        String txt;
        while (true) {
            if (loggingQueue.size() > 0) {
                txt = loggingQueue.remove(0);
                out.println(txt);
                if (out != System.out)
                    System.out.println(txt);
            } else {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    ForwardedException.forward(e);
                }
            }
        }
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
                factory.err.println(ex);
                if (factory.err != System.err)
                    System.err.println(ex);
            }
        }
    }

}
