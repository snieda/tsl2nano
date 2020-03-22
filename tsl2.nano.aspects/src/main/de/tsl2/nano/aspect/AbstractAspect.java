package de.tsl2.nano.aspect;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * This aspect is abstract to define the concrete pointcut in aop.xml
 */
@Aspect
public abstract class AbstractAspect {
    Map<String, ProfileInfo> profiling = new HashMap<>();
    static final String PREFIX = AbstractAspect.class.getSimpleName().toLowerCase() + ".";
    static final SimpleDateFormat SDF = new SimpleDateFormat(get("log.timeformat", "HH:mm:ss.SSS"));

    @Pointcut //("execution(public boolean mymethod(..))")
    abstract void trace();

    @Around("trace()")
    public Object aroundTrace(ProceedingJoinPoint pjp) throws Throwable {
        log(pjp.toShortString());
        return pjp.proceed();
    }

    @Pointcut
    abstract void profile();

    @Around("profile()")
    public Object aroundProfile(ProceedingJoinPoint pjp) throws Throwable {
        long mem = Runtime.getRuntime().freeMemory();
        long start = System.currentTimeMillis();
        log("==> " + Thread.currentThread().getName() + ": " + pjp.toShortString() + "(" + Arrays.toString(pjp.getArgs()) + ")");
        Object result = pjp.proceed();
        log("<== " + pjp.toShortString() + "{" + (mem = (Runtime.getRuntime().freeMemory() - mem)) + "b}: " + result);
        collect(pjp, System.currentTimeMillis() - start, mem);
        return result;
    }

    static String get(String key, String defaultValue) {
        return System.getProperty(PREFIX + key, defaultValue);
    }

    static int getInt(String key, Integer defaultValue) {
        return Integer.valueOf(System.getProperty(PREFIX + key, defaultValue.toString()));
    }

    void collect(ProceedingJoinPoint pjp, long duration, long mem) {
        try {
            String key = pjp.toShortString() + pjp.getKind();
            ProfileInfo pi = profiling.get(key);
            if (pi == null) {
                profiling.put(key, new ProfileInfo(key, pjp.getTarget().hashCode(), duration, mem));
            } else {
                pi.count++;
                pi.duration += duration;
                pi.memuse += mem;
    
                if (pi.count % getInt("profile.count", 1000) == 0 || pi.duration % getInt("profile.duration", 1000) == 0) {
                    log(pi);
                }
            }
        } catch (Exception e) {
                e.printStackTrace();
        }
    }

    @Pointcut
    abstract void mock();

    @Around("mock()")
    public Object aroundMock(ProceedingJoinPoint pjp) throws Throwable {
        log("mocking " + pjp.toShortString());
        return null;
    }

    public static void log(Object obj) {
        String time = SDF.format(Calendar.getInstance().getTime());
        System.out.println(time + " " + obj);
    }
}

class ProfileInfo {
    String key;
    long targetHash;//count diffs?
    long count;
    long duration;
    long memuse;

    ProfileInfo(String key, long targetHash, long duration, long memuse) {
        this.key = key;
        this.targetHash = targetHash;
        this.duration = duration;
        this.memuse = memuse;
    }
    @Override
    public String toString() {
        return "<<< " + getClass().getSimpleName() + " >>> " + key + ":" + count + " times " + duration + "ms " + memuse + "b";
    }
}
