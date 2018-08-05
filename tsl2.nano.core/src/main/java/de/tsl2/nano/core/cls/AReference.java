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
    @Attribute(required=false) //to be usable in a range
    String description;
    
    protected transient O instance;

    protected static final String PREFIX_REFERENCE = "@";
    protected static final String PREFIX_ID = ":";
    protected String postfixID = ""; //only to be extendable...

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
        return PREFIX_REFERENCE + toString(instance.getClass()) + PREFIX_ID + getId(instance) + postfixID;
    }

    protected static <B> String createDescription(Class<B> type, Object id) {
        return PREFIX_REFERENCE + toString(type) + PREFIX_ID + id;
    }

    /**
     * toString
     * @param type
     * @return
     */
    static <B> String toString(Class<B> type) {
        return BeanClass.getName(type, true);
    }

    /**
     * splits into the type and id
     * 
     * @param description
     * @return type, id
     */
    protected Pointer getTypeAndId(String description) {
    	assert description != null : "description must not be null!";
        String[] o = description.substring(1).split("[" + PREFIX_ID + postfixID + "]");
        if (o.length < 2)
        	throw new IllegalArgumentException("parsing exception on '" + description);
        T type = type(o[0]);
        return new Pointer(type, id(type, o[1]));
    }

    /**
     * getType
     * @return
     */
    public T getType() {
        return description != null ? type(description) : null;
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
     * creates an id from string
     * @param strID id information
     * @return id
     */
    protected abstract Object id(T type, String strId);
    
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

    public String getDescription() {
		return description;
	}

    
	protected void setDescription(String description) {
		checkDescription(description);
		this.description = description;
	}

	abstract protected void checkDescription(String description);

	@SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
        return description != null ? obj instanceof AReference && description.equals(((AReference)obj).description) : super.equals(obj);
    }
    
    @Override
    public int hashCode() {
        return description != null ? description.hashCode() : super.hashCode();
    }
    
    @Override
    public String toString() {
        return description != null ? description : super.toString();
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
