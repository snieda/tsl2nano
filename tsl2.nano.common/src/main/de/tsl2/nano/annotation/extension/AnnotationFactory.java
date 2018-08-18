package de.tsl2.nano.annotation.extension;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;

/**
 * This is a kind of workaround through the fact, that annotations cannot be
 * extended. If you want to create annotations, not known directly by the
 * underlying framework, the framework can provide a base annotation with only
 * one field: the factory class that will build something through the given
 * annotation.
 * <p/>
 * This base annotation has to be followed by the specific (unknown to the
 * underlying framework) annotation, holding the desired values.
 * <p/>
 * 
 * Example: @With(MyAnnotationFactory.class) @MySpecialAnnotation(myValue1="",
 * myValue2="")
 * 
 * @author Tom
 */
public interface AnnotationFactory<I, A extends Annotation> {
    
	void build(I instance, A annotation);

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void with(Object instance, AnnotatedElement annotationElment) {
		if (annotationElment.isAnnotationPresent(With.class)) {
			Annotation[] annotations = annotationElment.getAnnotations();
			for (int i = 0; i < annotations.length; i++) {
			    if (!(annotations[i] instanceof With))
			        continue;
				With with = (With) annotations[i];
				AnnotationFactory fact = BeanClass.createInstance(with.value());
				check(with.value(), annotations[i + 1]);
				fact.build(instance, annotations[i + 1]);
			}
		}
	}

    static void check(Class<? extends AnnotationFactory<?,?>> factory, Annotation annotation) {
        Class<? extends Annotation> aType = annotation.annotationType();
        ManagedException.assertion(aType.isAnnotationPresent(With.class) && factory.equals(aType.getAnnotation(With.class).value()), 
            "The \"@With\" annotation-type " + annotation.annotationType() + " must be annotated with @With(" + factory.getName() + ".class)");
    }
}
