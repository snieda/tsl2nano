package de.tsl2.nano.core.util.parser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.Format;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
/**
 * De-/Serialization informations for a bean attribute, used by BeanClass and StructParser
 */
public @interface Serial {
    /** alternative name on de-/serialization */
    String name() default "";

    /** alternative type in hierarchy on deserialization */
    Class<?> type() default Object.class;

    /** formatter for de-/serialization */
    Class<? extends Format> formatter() default Format.class;

    /** whether to ignore the field on de-/serialization */
    boolean ignore() default false;

    /** whether to embed (without enclosing in extra tag) the items (of a list or array) directly into the parent */
    boolean embedItems() default false;

}
