package de.tsl2.nano.core.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;

/**
 * Provides an {@link InvocationHandler} to {@link Proxy} {@link Annotation}s. Additionally some helpers on
 * {@link Annotation}s are available. These helpers evaluate declared annotations classloader-independent.
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings("unchecked")
public class AnnotationProxy<A extends Annotation> extends DelegationHandler<A> {
    /** serialVersionUID */
    private static final long serialVersionUID = 8875640804676220045L;

    private static final Log LOG = LogFactory.getLog(AnnotationProxy.class);
    
    public AnnotationProxy(A annotation, Object... properties) {
        super(annotation, properties);
    }

    public AnnotationProxy(Class<A> annotationType, Object... properties) {
        super(annotationType, properties);
    }

    @Override
    protected Object invokeDelegate(Method method, Object[] args) throws IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException {
        Method delegatingMethod = getDelegatingAnnotationMethod(method);
        if (delegatingMethod != null) {
            return delegatingMethod.invoke(delegate, args);
        } else {
            //otherwise use the annotations default value
            Object defaultValue = method.getDefaultValue();
            if (defaultValue != null) {
                return defaultValue;
            }
            throw new UnsupportedOperationException(
                "Method \"" + method.getName()
                    + "\" doesn't exist in the delegated annotation, and no methods default value exist!");
        }
    }

    private Method getDelegatingAnnotationMethod(Method method) {
        try {
            Method customMethod =
                delegate.getClass().getDeclaredMethod(method.getName(), method.getParameterTypes());
            if (customMethod.getReturnType().isAssignableFrom(method.getReturnType())) {
                return customMethod;
            }
            return null;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /****************************************************************************
     * Utility Methods on Annotations
     ***************************************************************************/

    /**
     * asks for the given annotation. this is a 'soft' implementation, means: will not use objects reference, but
     * class.getName(). so, different classloaders will not be respected!
     * 
     * @throws NullPointerException {@inheritDoc}
     * @since 1.5
     */
    public static final <A extends Annotation> A getAnnotation(Annotation[] annotations, Class<A> annotationClass) {
        if (annotationClass == null) {
            throw new NullPointerException();
        }

        for (int i = 0; i < annotations.length; i++) {
            if (annotations[i].annotationType().getName().equals(annotationClass.getName())) {
                return (A) annotations[i];
            }
        }
        return null;
    }

    /**
     * SEEMS NOT TO WORK YET!
     * <p/>
     * gets annotations for the given class. feel free to change the map to change the class annoations.
     * 
     * @param cls class to read/change the annotations from.
     * @return all class annotations
     */
    protected static Map<Class<? extends Annotation>, Annotation> getAnnotationMap(Class<?> cls) {
        try {
            Field field = Class.class.getDeclaredField("annotations");
            field.setAccessible(true);
            return (Map<Class<? extends Annotation>, Annotation>) field.get(cls);
        } catch (Exception ex) {
            ManagedException.forward(ex);
            return null;
        }
    }

    /**
     * gets annotations for the given class. feel free to change the map to change the class annoations.
     * 
     * @param cls class to read/change the annotations from.
     * @return all class annotations
     */
    public static Annotation[] getAnnotations(Class<?> cls) {
        return cls.getDeclaredAnnotations();
    }

    /**
     * evaluates the annotation for the given field. uses the soft-implementation (classloader-independent) of
     * {@link #getAnnotation(Annotation[], Class)}.
     * 
     * @param cls class holding the field
     * @param fieldName field name
     * @param annotationType type of annotation to evaluate
     * @return annotation of the given field
     */
    public static <A extends Annotation> A getAnnotation(Class<?> cls, String fieldName, Class<A> annotationType) {
        return getAnnotation(getAnnotations(cls, fieldName), annotationType);
    }

    /**
     * evaluates the given field and delegates to {@link #getAnnotations(Field)}.
     * 
     * @param cls class holding the field
     * @param fieldName field name
     * @return annotations of the given field
     */
    public static Annotation[] getAnnotations(Class<?> cls, String fieldName) {
        try {
            return getAnnotations(cls.getDeclaredField(fieldName));
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * evaluates the annotation for the given field. uses the soft-implementation (classloader-independent) of
     * {@link #getAnnotation(Annotation[], Class)}.
     * 
     * @param cls class holding the field
     * @param f field to read/change the annotations from.
     * @param annotationType type of annotation to evaluate
     * @return annotation of the given field
     */
    public static <A extends Annotation> A getAnnotation(Field field, Class<A> annotationType) {
        return getAnnotation(getAnnotations(field), annotationType);
    }

    /**
     * gets annotations for the given field. feel free to change the map to change the class annotations.
     * 
     * @param f field to read/change the annotations from.
     * @return all field annotations
     */
    public static Annotation[] getAnnotations(Field f) {
        return f.getDeclaredAnnotations();
    }

    /**
     * evaluates the annotation for the given method. uses the soft-implementation (classloader-independent) of
     * {@link #getAnnotation(Annotation[], Class)}.
     * 
     * @param cls class holding the method
     * @param methodName method name
     * @param annotationType type of annotation to evaluate
     * @param par optional method parameters
     * @return annotation of the given method
     */
    public static <A extends Annotation> A getAnnotationForMethod(Class<?> cls,
            String methodName,
            Class<A> annotationType,
            Class<?>... par) {
        return getAnnotation(getAnnotationsForMethod(cls, methodName, par), annotationType);
    }

    /**
     * evaluates the given method and delegates to {@link #getAnnotations(Method)}.
     * 
     * @param cls class holding the field
     * @param methodName method name
     * @param par optional method parameters
     * @return annotations of the given method
     */
    public static Annotation[] getAnnotationsForMethod(Class<?> cls, String methodName, Class<?>... par) {
        try {
            return getAnnotations(cls.getDeclaredMethod(methodName, par));
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * evaluates the annotation for the given method. uses the soft-implementation (classloader-independent) of
     * {@link #getAnnotation(Annotation[], Class)}.
     * 
     * @param cls class holding the method
     * @param method method to read/change the annotations from.
     * @param annotationType type of annotation to evaluate
     * @return annotation of the given method
     */
    public static <A extends Annotation> A getAnnotation(Method method, Class<A> annotationType) {
        return getAnnotation(getAnnotations(method), annotationType);
    }

    /**
     * gets annotations for the given method. feel free to change the map to change the method annotations.
     * 
     * @param m method to read/change the annotations from.
     * @return all method annotations
     */
    public static Annotation[] getAnnotations(Method m) {
        return m.getDeclaredAnnotations();
    }

    /**
     * convenience on {@link #setAnnotationValues(Annotation, Map)} to give values as array of key/value pairs.
     * @param annotation annotation to change
     * @param values key/values
     * @return count of changes on annotation
     */
    public static int setAnnotationValues(Annotation annotation, Object...values) {
        return setAnnotationValues(annotation, MapUtil.asMap(values));
    }
    
    /**
     * asserts, java implementation works on annotation-proxies with invocationhandler having a field 'memberValues'.
     * Works on suns jdk1.7 and jdk1.8
     * 
     * @param annotation annotation to change
     * @param values to be set on the given annoation. you have to ensure the names and types.
     * @return count of changes on annotation
     */
    public static int setAnnotationValues(Annotation annotation, Map<String, Object> values) {
        Object handler = Proxy.getInvocationHandler(annotation);
        try {
            Field f = handler.getClass().getDeclaredField("memberValues");
            f.setAccessible(true);
            Map<String, Object> memberValues = (Map<String, Object>) f.get(handler);
            Set<String> keys = values.keySet();
            int count = 0;
            Object oldValue, v;
            for (String key : keys) {
                v = values.get(key);
                oldValue = memberValues.put(key, v);
                LOG.debug("changed " + annotation + "." + key + ": " + oldValue + " --> " + v);
                count++;
            }
            return count;
        } catch (Exception e) {
            ManagedException.forward(e);
            return -1;
        }
    }
}