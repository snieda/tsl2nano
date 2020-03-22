package de.tsl2.nano.aspect;

import java.util.function.Function;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * The aspect works on methods annotated with @Cover.
 *
 * Note: This aspect is not abstract to define a pointcut with arguments
 */
@Aspect
public class AspectCover {
    // @Pointcut("call(@Cover * *(..)) || within(@Cover *)")
    @Pointcut("@annotation(cover)")
    public void callAt(Cover cover) {
    }

    @Around("callAt(cover)")
    public Object around(ProceedingJoinPoint pjp, Cover cover) throws Throwable {
        Object result = null;
        if (cover == null || cover.trace())
            AbstractAspect.log(pjp.toShortString());
        if (cover != null) {
            if (!cover.before().equals(Function.class))
                invoke(cover.before(), pjp.getThis(), pjp.getArgs());

            if (cover.up() && !pjp.getKind().equals(JoinPoint.FIELD_SET) && !pjp.getKind().equals(JoinPoint.FIELD_GET))
                System.out.println("mocked: " + pjp.toShortString());
            else
                result = pjp.proceed();

            if (!cover.after().equals(Function.class))
                invoke(cover.after(), pjp.getThis(), pjp.getArgs());
        }
        return result;
    }

    Object invoke(Class<Function> call, Object instance, Object[] args) throws Throwable {
        return null;
    }
}
