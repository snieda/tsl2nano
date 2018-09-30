package de.tsl2.nano.h5.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import de.tsl2.nano.annotation.extension.With;

/**
 * defines a {@link de.tsl2.nano.h5.collector.Controller} for a bean.
 * <pre>
 * possible compositions:
 * - baseType + baseAttribute
 * - baseType + targetAttribute
 * 
 * increase parameter are optional for a special increase button. the iconAttribute is optional 
 * if you have an attribute of type byte[]/blob/string to present the item.
 * </pre>
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
@Retention(RUNTIME)
@Target(TYPE)
@With(ControllerAnnotationFactory.class)
public @interface Controller {
    Class<? extends Serializable> baseType();
    String baseAttribute();
    Class<? extends Serializable> targetType() default Serializable.class; //normally the type of the annotation owning class!
    String targetAttribute() default "";
    String iconAttribute() default "";
    String increaseAttribute() default "";
    int increaseCount() default 1;
    int increaseStep() default 1;
}
