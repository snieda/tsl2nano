/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 04.03.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.incubation.specification;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Text;

/**
 * Parameter types to simplify rule parameter definitions - especially on xml files.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class ParType implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -2568880599680159654L;
    
    @Text(required = false)
    T type;
    @Attribute(required = false)
    Class<?> javaType;

    /**
     * constructor
     */
    public ParType() {
    }

    /**
     * constructor
     * 
     * @param type
     */
    public ParType(T type) {
        super();
        this.type = type;
    }

    /**
     * constructor
     * 
     * @param javaType
     */
    public ParType(Class<?> javaType) {
        super();
        this.javaType = javaType;
    }

    public Class<?> getType() {
        return javaType != null ? javaType : type.transform();
    }

    public static final ParType TEXT = new ParType(T.TEXT);
    public static final ParType BOOLEAN = new ParType(T.BOOLEAN);
    public static final ParType NUMBER = new ParType(T.NUMBER);
    public static final ParType DATE = new ParType(T.DATE);
    
    public enum T {
        TEXT, BOOLEAN, NUMBER, DATE;

        protected Class<?> transform() {
            switch (this) {
            case TEXT:
                return String.class;
            case BOOLEAN:
                return Boolean.class;
            case NUMBER:
                return BigDecimal.class;
            case DATE:
                return Date.class;
            }
            return null;
        }

    }
}
