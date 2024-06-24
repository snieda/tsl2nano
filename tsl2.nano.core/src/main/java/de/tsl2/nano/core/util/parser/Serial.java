package de.tsl2.nano.core.util.parser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.Format;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
/**
 * De-/Serialization informations for a bean attribute, used by BeanClass and StructParser
 */
public @interface Serial {
    /** alternative name on serialization */
    String name();

    /** alternative type in hierarchy on deserialization */
    Class<?> type();

    /** formatter for de-/serialization */
    Class<Format> formatter();

    /** whether to ingore the field on de-/serialization */
    boolean ignore();
}
