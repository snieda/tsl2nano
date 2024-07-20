package de.tsl2.nano.core.util.parser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * used by StructParser/Serializer and their implementations to define beans attribute/field behaviour
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.PARAMETER })
public @interface SerialClass {
    /** whether to use fields instead of bean attributes */
    boolean useFields() default false;

    // /** whether to sort all attributes/fields (and of all child items) on their names (on serializing) */
    // boolean orderByAttributeNamesInStructure() default false;

    /** serialize only types that implement Serializable */
    boolean implementingSerializable() default false;

    /** constraint the serialization to fields/methods having the given modifiers. default: -1, means, all modifiers. */
    int havingModifiers() default -1;

    /** define the bean attribute (or field) order on serializing */
    String[] attributeOrder() default {};
}
