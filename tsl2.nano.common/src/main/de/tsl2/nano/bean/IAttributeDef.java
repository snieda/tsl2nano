/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Feb 22, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.bean;

import java.util.Date;

/**
 * Extending to the java types, additional metadata informations through a database or annotations.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public interface IAttributeDef {
    /** maximum length - useful on strings */
    int length();

    /** scale - useful for numbers of type BigDecimal */
    int scale();

    /** precision - useful for numbers of type BigDecimal */
    int precision();

    /** should return true, if attribute-value may be null */
    boolean nullable();

    /** should be true, if this attribute defines the id of the owning bean */
    boolean id();

    /** returns {@link Date}, {@link Time} or {@link Timestamp} depending on temporal type annotation - or null. */
    Class<? extends Date> temporalType();

    /**
     * like an uml composition, this attribute as one-to-many child can't exist without it's parent
     * 
     * @return true, if attribute cannot exist without it's parent bean.
     */
    boolean composition();

    /**
     * if attribute type is one-to-many (collection), jpa will persist this relation only, if cascading is activated
     * (ALL,MERGE, orphanremoval).
     * 
     * @return true , if cascading is activated
     */
    boolean cascading();

    /** whether this field should be unique */
    boolean unique();
}
