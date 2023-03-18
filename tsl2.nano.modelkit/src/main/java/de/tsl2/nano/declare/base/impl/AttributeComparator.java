package de.tsl2.nano.modelkit.impl;

/**
 * provides a field-comparing comparator
 */
public class AttributeComparator<T> extends NamedComparator<T> {

    String fieldName;

    public AttributeComparator(String name, String fieldName, String selectorFact, String... onEqualsThen) {
        super(name, selectorFact, (facts, c1, c2) -> get(c1, fieldName).compareTo(get(c2, fieldName)), onEqualsThen);
        this.fieldName = fieldName;
    }

    static final Comparable get(Object obj, String fieldName) {
        try {
            // TODO use bean property instead
            return (Comparable<?>) obj.getClass().getField(fieldName).get(obj);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            throw new IllegalArgumentException(fieldName, e);
        }
    }
}
