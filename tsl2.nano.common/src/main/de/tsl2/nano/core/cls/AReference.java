/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 02.03.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.core.cls;

import org.simpleframework.xml.Attribute;

/**
 * provides a detach/attach or de-/materialize or pointer/content algorithm for any object. usable for de-/serializing
 * non-serializable objects.
 * <p/>
 * To be more abstract, we could generalize the description type ;-)
 * 
 * @param <T> object type (normally Class<O>
 * @param <O> object instance itself
 * @author Tom
 * @version $Revision$
 */
public abstract class AReference<T, O> {
    /** the description is a concatenation of the full class-name and a unique object-id */
    @Attribute
    String description;
    
    transient O instance;

    static final String PREFIX_REFERENCE = "@";
    static final String PREFIX_ID = ":";

    /**
     * constructor for deserialization
     */
    protected AReference() {
    }
    
    /**
     * constructor
     * 
     * @param instance
     */
    public AReference(O instance) {
        this.instance = instance;
        this.description = createDescription(instance);
    }

    /**
     * constructor
     * 
     * @param description
     */
    public AReference(Class<O> type, Object id) {
        this(createDescription(type, id));
    }

    /**
     * constructor
     * 
     * @param description
     */
    public AReference(String description) {
        this.description = description;
    }

    protected String createDescription(Object instance) {
        return PREFIX_REFERENCE + BeanClass.getName(instance.getClass()) + PREFIX_ID + getId(instance);
    }

    protected static <B> String createDescription(Class<B> type, Object id) {
        return PREFIX_REFERENCE + BeanClass.getName(type) + PREFIX_ID + id;
    }

    /**
     * splits into the type and id
     * 
     * @param description
     * @return type, id
     */
    protected Pointer getTypeAndId(String description) {
        String[] o = description.substring(1).split(PREFIX_ID);
        return new Pointer(type(o[0]), o[1]);
    }

    /**
     * getType
     * @return
     */
    public T getType() {
        return type(description);
    }
    
    /**
     * provides the unique id of an instance
     * @param instance instance to get the id for
     * @return instance id
     */
    protected abstract Object getId(Object instance);
    
    /**
     * creates a type from string
     * @param strType type information
     * @return type
     */
    protected abstract T type(String strType);
    
    /**
     * materializes an instance through given description
     * @param description holding the type and id information
     * @return new instance
     */
    protected abstract O materialize(String description);

    /**
     * get the real instance
     * 
     * @return
     */
    public O resolve() {
        if (instance == null) {
            instance = materialize(description);
        }
        return instance;
    }

    /**
     * structure for type and id
     * 
     * @param <T>
     * @author Tom
     * @version $Revision$
     */
    public class Pointer {
        public T type;
        public Object id;

        public Pointer(T type, Object id) {
            super();
            this.type = type;
            this.id = id;
        }
    }
}
