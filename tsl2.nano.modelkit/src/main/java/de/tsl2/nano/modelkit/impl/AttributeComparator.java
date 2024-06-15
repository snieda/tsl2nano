/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 31.03.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.modelkit.impl;

import java.lang.reflect.Field;

import lombok.Getter;
import lombok.Setter;

/**
 * provides a field-comparing comparator
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class AttributeComparator<T> extends Comp<T> {

    static {
        ModelKitLoader.registereElement(AttributeComparator.class);
    }

    @Getter @Setter
    String fieldName;

    AttributeComparator() {
    }

    public AttributeComparator(String name, String fieldName, String selectorFact, String... onEqualsThen) {
        super(name, selectorFact, (facts, c1, c2) -> get(c1, fieldName).compareTo(get(c2, fieldName)), onEqualsThen);
        this.fieldName = fieldName;
    }

    static final Comparable get(Object obj, String fieldName) {
        try {
            // TODO use bean property instead
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (Comparable<?>) field.get(obj);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            throw new IllegalArgumentException(fieldName, e);
        }
    }
}
