package de.tsl2.nano.aspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.function.Function;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * The aspect works on methods annotated with @Cover. It is possible to
 * coverup/mock fields - but only if they are declared as interface.
 *
 * Note: This aspect is not abstract to define a pointcut with arguments
 * 
 * for more informations see {@link Cover}
 */
@Aspect
public class AspectCover {

    private Object thisField; //workaround to the class, holding the field - on method invoking

    // @Pointcut("call(@Cover * *(..)) || within(@Cover *)")
    @Pointcut("@annotation(cover)")
    public void callAt(Cover cover) {
    }

    @Around("callAt(cover)")
    public Object around(ProceedingJoinPoint pjp, Cover cover) throws Throwable {
        try {
            log("------->");
            Object result = null;
            if (cover == null || cover.trace())
                logInfo(pjp);
            if (cover != null) {
                invokeOrCover(CoverBefore.class, cover.before(), pjp);

                if (isFieldAccess(pjp)) {
                    thisField = pjp.getThis();
                }
                if (cover.up() && !pjp.getKind().equals(JoinPoint.FIELD_GET)
                        && (cover.upRegEx() == "" || Arrays.toString(pjp.getArgs()).matches(cover.upRegEx()))) {
                    log("COVERUP: "+ pjp.getSourceLocation() + ": " + pjp.toShortString() + " (" + Arrays.toString(pjp.getArgs()) + ")");
                    if (pjp.getKind().equals(JoinPoint.FIELD_SET)) {
                        Field field = getField(pjp);
                        if (field.getType().isInterface()) {
                            log("\tsetting proxy on field " + field);
                            log("TODO-ERROR: setting field with proxy is not working on aspectj!!!");
                            result = createProxy(cover, pjp);
                        } else { // we can't coverup here, otherwise we set only null fields!
                            result = invokeOrCover(CoverBody.class, cover.body(), pjp);
                        }
                    } else {
                        result = invokeOrCover(CoverBody.class, cover.body(), pjp);
                    }
                } else {
                    result = invokeOrCover(CoverBody.class, cover.body(), pjp);
                }
                invokeOrCover(CoverAfter.class, cover.after(), pjp);
            }
            log("<-------");
            return result;
        } catch (Throwable ex) {
            ex.printStackTrace(); // otherwise no error will be logged
            throw ex;
        }
    }

    private boolean isFieldAccess(ProceedingJoinPoint pjp) {
        return pjp.getKind().equals(pjp.FIELD_GET) || pjp.getKind().equals(pjp.FIELD_SET);
    }

    private Field getField(ProceedingJoinPoint pjp) throws NoSuchFieldException {
        return pjp.getSignature().getDeclaringType().getDeclaredField(pjp.getSignature().getName());
    }

    private Object invokeOrCover(
            Class<? extends Annotation> inspectionPoint,
            Class<?> staticClass, ProceedingJoinPoint pjp) throws Throwable {
                return invokeOrCover_(false, inspectionPoint, staticClass, pjp);
    }

    // there is a bug on aspectj - standard function overloading ends up in an internal error!! so we use the '_'
    private Object invokeOrCover_(
            boolean coverUp,
            Class<? extends Annotation> inspectionPoint,
            Class<?> staticClass, ProceedingJoinPoint pjp) throws Throwable {
        Method coverMethod;
        if ((coverMethod = findMethod(staticClass, pjp, inspectionPoint)) != null)
            return coverMethod(staticClass, coverMethod, pjp, inspectionPoint);
        else if (!coverUp && inspectionPoint.equals(CoverBody.class))
            return pjp.proceed();
        else 
            return null;
    }

    private Method findMethod(Class<?> staticClass, ProceedingJoinPoint pjp, Class<? extends Annotation> inspectionPoint) {
        try {
            Class<?> cls = !staticClass.equals(Class.class) ? staticClass : caller(pjp).getClass();
            String postfix = inspectionPoint.getSimpleName().toLowerCase().contains("body") ? "" : inspectionPoint.getSimpleName();
            String name = pjp.getSignature().getName() + postfix;
            System.out.print("\tcalling @" + inspectionPoint.getSimpleName() + " " + cls.getSimpleName() + "." + name + "(CoverArgs)...");
            Method m = cls.getDeclaredMethod(name, new Class[]{CoverArgs.class});
            return m.isAnnotationPresent(inspectionPoint) ? m : null;
        } catch (NoSuchMethodException | SecurityException e) {
            log("not available!");
            return null; // ok, no problem
        }
    }

    private Object caller(ProceedingJoinPoint pjp) {
        return thisField == null || isFieldAccess(pjp) ? pjp.getThis() : thisField;
    }

    private Object coverMethod(Class<?> staticClass, Method coverMethod,
            ProceedingJoinPoint pjp,
            Class<? extends Annotation> inspectionPoint)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Object result = coverMethod.invoke(staticClass.equals(Class.class) 
                            ? caller(pjp) : null, createCoverArgs(pjp, inspectionPoint));
        log("done! result=" + result);
        return result;
    }

    private CoverArgs createCoverArgs(ProceedingJoinPoint pjp, Class<? extends Annotation> inspectionPoint) {
        return new CoverArgs(pjp.toLongString(), caller(pjp), pjp.getTarget(), inspectionPoint, pjp.getArgs());
    }

    private Object createProxy(final Cover cover, final ProceedingJoinPoint pjp)
            throws IllegalArgumentException, NoSuchFieldException {
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class[]{getField(pjp).getType()}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        log("COVERPROXY: " + method + ": " + Arrays.toString(args));
                        if (method.toString().matches(cover.upRegEx())) {
                            return invokeOrCover_(true, CoverBody.class, cover.body(), pjp);
                        } else { //call on original instance
                            return method.invoke(pjp.getTarget(), args);
                        }
                    }
                });
    }

    private void logInfo(ProceedingJoinPoint pjp) {
        AbstractAspect.log(pjp.toShortString() + "(" + Arrays.toString(pjp.getArgs()) + ")");
    }

    private static final void log(Object o) {
        System.out.println(o);
    }

}
