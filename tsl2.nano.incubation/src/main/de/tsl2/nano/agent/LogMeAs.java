package de.tsl2.nano.agent;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**
 * Logs any method call using the standard java Logger configuration (as FINEST) - or if system-property
 * 'LogMeAs.system.out' was set to true, logs any method call satisfying the pointcut in aop.xml to the standard
 * console.
 * 
 * <pre>
 * Details:<br/>
 * - the standard pointcut name is 'traceMethods'
 * - the advices 'after' and afterReturning' use pointcut name 'traceMethodEx'
 * </pre>
 * 
 * @author Thomas Schneider
 */
@Aspect
public abstract class LogMeAs {

    public static final String SYSTEMOUT = "LogMeAs.system.out";
    private static final boolean systemOut = Boolean.getBoolean(SYSTEMOUT);

    private static final Format sdf = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
    private static final Date date = new Date();

    static {//dynamic agent loading
        AttachAgent.main(null);
    }
    
    //private static final Logger log = Logger.getLogger(LoggingAspect.class.getSimpleName());
    /*
     * protected pointcut traceMethods() : (
     * !cflow(execution(String *.toString()))
     * && !cflow(within(LogMeAs))
     * && (execution(* *.* (..)))
     * || execution(*.new (..)));
     */
    //@Pointcut("cflow(execution(String *.toString())) && !cflow(within(LogMeAs)) && (execution(* *.* (..))) || execution(*.new (..))")
    //@Pointcut("execution(* com..*.*(..))")

    /**
     * a generic scope (pointcut) to be concrete on an extending aop.xml (found in classpath)
     */
    @Pointcut
    public void traceMethodAround() {
    }

    @Pointcut
    public void traceMethod() {
    }

    @Around("traceMethodAround()")
    public Object logAround(ProceedingJoinPoint jp) {
        long start = System.currentTimeMillis();
        long free = Runtime.getRuntime().freeMemory();
        try {
            Object result;
            log(getLog(jp.getSignature()), "--> " + jp.toString() + ", args: " + Arrays.deepToString(jp.getArgs())
                + "\n\tresult: " + (result = jp.proceed()) + "\n\t[time: " + (System.currentTimeMillis() - start)
                + " msecs, mem: "
                + (free - Runtime.getRuntime().freeMemory()) + " bytes]");
            return result;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    @Before("traceMethod()")
    public void before(JoinPoint jp) {
        Signature sig = jp.getSignature();
        Logger logger = getLog(sig);
        if (isLoggable(logger)) {
            log(logger, "--> entering " + fullName(sig) + Arrays.deepToString(jp.getArgs()));
        }
    }

    @After("traceMethod()")
    public void after(JoinPoint jp) {
        Signature sig = jp.getSignature();
        Logger logger = getLog(sig);
        if (isLoggable(logger)) {
            log(logger, "\tleaving " + fullName(sig));
        }
    }

    @AfterReturning(pointcut = "traceMethod()", returning = "result")
    public void afterReturning(JoinPoint jp, Object result) {
        Signature sig = jp.getSignature();
        Logger logger = getLog(sig);
        if (isLoggable(logger)) {
            log(logger, "\treturning from " + fullName(sig) + ": " + result);
        }
    }

    @AfterThrowing(pointcut = "traceMethod()", throwing = "ex")
    public void afterThrowing(JoinPoint jp, Throwable ex) {
        Signature sig = jp.getSignature();
        Logger logger = getLog(sig);
        if (isLoggable(logger)) {
            log(logger, "\ttrown: " + fullName(sig) + ex);
        }
    }

    boolean isLoggable(Logger log) {
        return systemOut || log.isLoggable(Level.FINEST);
    }

    /**
     * Find log for current class
     */
    private static final Logger getLog(Signature sig) {
        return systemOut ? null : Logger.getLogger(sig.getDeclaringType().toString());
    }

    void log(Logger log, Object msg) {
        if (systemOut)
            System.out.println(getTime() + msg);
        else
            log.finest((String) msg);
    }

    private String getTime() {
        date.setTime(System.currentTimeMillis());
        return sdf.format(date) + ": ";
    }

//UNDER CONSTRUCTION
    private void logMe(ProceedingJoinPoint jp, Runnable logRun) {
        Logger log = getLog(jp.getSignature());
        if (isLoggable(log)) {
            logRun.run();
        }
    }

    /**
     * @return String with class name + method name
     */
    private String fullName(Signature sig) {
        return "[" + sig.getDeclaringType().getName() + "." + sig.getName() + "]";
    }
}