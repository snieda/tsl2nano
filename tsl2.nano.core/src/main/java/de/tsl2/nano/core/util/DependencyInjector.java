package de.tsl2.nano.core.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.ClassFinder;
import de.tsl2.nano.core.log.LogFactory;

/**
 * Tries to find all fields with given Annotations to inject values or a BeanProxy as
 * instance.
 */

@SuppressWarnings({ "rawtypes", "unchecked" })
public class DependencyInjector {
    public static final String KEY_INJECTORANNOTATIONS = "tsl2.nano.dependencyinjector.injectorannotations";

    private static final Log LOG = LogFactory.getLog(DependencyInjector.class);

    private Map<Class, Object> injectables;
    private List<Class> injectorAnnotations;
    private List<Class> producerAnnotations;
    private Function<Class<?>, ?> instanceCreator;

    private List producers;

    public DependencyInjector() {
        this(Arrays.asList(Inject.class), Arrays.asList(Producer.class), null);
    }

    /**
     * 
     * @param annotationsToInspect only fields without a value (=null) having one the given annotations will become an injected value
     * @param producerAnnotation if no value for a field is found in cache and if methods in classpath are found with this annotation, this will be used to create a new instance for that field
     * @param instanceCreator if no value for a field and no producer annotation is found, this instancecreator will create a new instance to be injected for that field.
     * @param providedInstances provided available instance cache to be used by injection
     */
    public DependencyInjector(List<Class> annotationsToInspect, List<Class> producerAnnotations,
            Function<Class<?>, ?> instanceCreator, Object... providedInstances) {
        this.injectorAnnotations = annotationsToInspect;
        this.producerAnnotations = producerAnnotations;
        if (instanceCreator == null) {
            this.instanceCreator = cls -> InstanceCreator.createInstance(cls);
        }
        injectables = new HashMap<>();
        for (int i = 0; i < providedInstances.length; i++) {
            injectables.put(providedInstances[i].getClass(), providedInstances[i]);
        }

        addInjectorAnnotationsFromSystemProperties();
        LOG.info(this);
    }

    private void addInjectorAnnotationsFromSystemProperties() {
        String injectors = System.getProperty(KEY_INJECTORANNOTATIONS);
        if (injectors != null) {
            LOG.info("trying to load injector annotation classes from:" + injectors);
            String ins[] = injectors.split(",");
            Arrays.stream(ins).forEach(i -> Util
                    .trY(() -> injectorAnnotations.add(Thread.currentThread().getContextClassLoader().loadClass(i)),
                            false));
        }
    }

    public void addInjectorAnnotations(List<Class> injectorAnnotations) {
        this.injectorAnnotations.addAll(injectorAnnotations);
    }

    public void addProducerAnnotations(List<Class> producerAnnotations) {
        this.producerAnnotations.addAll(producerAnnotations);
        this.producers = null;
    }

    public void addInstances(Object... instances) {
        Arrays.stream(instances).forEach(i -> injectables.put(i.getClass(), i));
    }

    public <T> T getInstance(Class<T> toBeProvided) {
        T instance = findInjectable(injectables, toBeProvided);
        return instance != null ? instance : provideInstance(toBeProvided);
    }

    protected <T> T provideInstance(Class<T> toBeProvided) {
        Class<T> implClass = ObjectUtil.getDefaultImplementation(toBeProvided);
        T instance;
        Method producer = getProducers().stream().filter(p -> p.getReturnType().equals(implClass)).findFirst()
                .orElse(null);
        if (producer != null) {
            instance = (T) Util.trY(() -> producer.invoke(null));
        } else {
            instance = (T) instanceCreator.apply(implClass);
        }
        LOG.debug("providing new instance: " + instance);
        injectables.put(toBeProvided, instance);
        inject(instance, injectables, injectorAnnotations.toArray(new Class[0]));
        return instance;
    }

    private List<Method> getProducers() {
        if (producers == null) {
            producers = new ArrayList<>();
            producerAnnotations.forEach(p -> producers.addAll(ClassFinder.self().findMethods(".*", -1, p)));
            LOG.info("found " + producers.size() + " in classpath");
        }
        return producers;
    }

    /** <pre>
     * tries to inject values to fields (having no value and are annotated with given #annotationsToInspect) of the given instance.
     * values are provided in this order:
     *   - from existing cache of provided and created instances
     *   - from produder methods (#producerAnnotations) on classpath providing the right instance type
     *   - from a given (or internal) instance creator (putting the new instance to the cache)
     * 
     * The internal instance creator tries to find implementation classes for interfaces on classpath - if not available, 
     * a dynamic proxy instance will be used.
     * 
     * @param <V> instance type
     * @param instance instance to inject values to its fields.
     * @return same instance reference
     */
    public <V> V inject(V instance) {
        inject((Object) instance, injectables, injectorAnnotations.toArray(new Class[0]));
        return instance;
    }

    void inject(Object instance, Map<Class, Object> injectables, Class... injectOnAnyOfAnnotations) {
        Field[] fields = BeanClass.fieldsOf(instance.getClass());
        if (LOG.isDebugEnabled()) {
            LOG.debug("found " + fields.length + " fields on " + instance.getClass() + ": "
                    + Arrays.stream(fields).map(f -> f.getName()).toList());
        }
        Arrays.stream(fields)
                .filter(f -> hasAnnotationOrIsTypeOf(f, injectOnAnyOfAnnotations))
                .forEach(f -> inject(instance, injectables, f));
    }

    private void inject(Object instance, Map<Class, Object> injectables, Field f) {
        Object valueToInject = findInjectable(injectables, f.getType());
        if (valueToInject == null) {
            valueToInject = provideInstance(f.getType());
        }
        if (valueToInject != null && Util.trY(() -> f.get(instance) == null)) {
            if (LOG.isDebugEnabled())
                LOG.debug("injecting field: " + f.getName() + " with instance " + instance);
            f.setAccessible(true);
            final Object value = valueToInject;
            Util.trY(() -> f.set(instance, value));
        }
    }

    private <T> T findInjectable(Map<Class, Object> injectables, Class<T> type) {
        Object injectable = injectables.get(type);
        if (injectable == null) {
            injectable = injectables.get(BeanClass.getDefiningClass(type));
            if (injectable == null) {
                injectable = injectables.values().stream().filter(o -> type.isAssignableFrom(o.getClass())).findFirst()
                        .orElse(null);
            }
        }
        return (T) injectable;
    }

    private static boolean hasAnnotationOrIsTypeOf(Field f, Class[] injectOnAnyOfAnnotations) {
        return Arrays.stream(injectOnAnyOfAnnotations)
                .anyMatch(type -> (type.isAnnotation() && f.isAnnotationPresent(type))
                        || type.isAssignableFrom(f.getType()));
    }

    public void reset() {
        injectables.clear();
    }

    static class InstanceCreator {
        private static <T> T createInstance(Class<T> implClass) {
            T instance;
            if (implClass.isInterface()) {
                instance = createImplementingInstance(implClass);
            } else {
                instance = newInstance(implClass);
            }
            return instance;
        }

        private static <T> T createImplementingInstance(Class<T> implClass) {
            T instance;
            Collection<Class<T>> implClasses = ClassFinder.self().findClass(implClass);
            if (!implClasses.isEmpty()) {
                implClass = implClasses.iterator().next();
                instance = newInstance(implClass);
            } else {
                instance = createProxy(implClass);
            }
            return instance;
        }

        protected static <T> T createProxy(Class<T> implClass) {
            return AdapterProxy.create(implClass);
        }

        private static <T> T newInstance(Class<T> implClass) {
            return BeanClass.getBeanClass(implClass).createInstance();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    public @interface Inject {

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD })
    public @interface Producer {

    }

    public String toString() {
        return Util.toString(this.getClass(), "\n\tinjecting-annotations: " + this.injectorAnnotations,
                "\n\tproducer-annotation  : " + this.producerAnnotations + " (producers found: "
                        + (producers != null ? producers.size() : 0) + ")",
                "\n\tinstanceCreator      :" + this.instanceCreator,
                "\n\tinjectables          : " + injectables);
    }
}
