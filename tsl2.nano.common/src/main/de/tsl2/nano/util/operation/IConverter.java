/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jul 2, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.util.operation;

/**
 * to convert from one to another object. we use the names from and to. synonyms would be:<br/>
 * from = source, min, begin<br/>
 * to = destination, max, end, until<br/>
 * 
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public interface IConverter<FROM, TO> {
    /** converts TO to FROM */
    FROM from(TO toValue);

    /** converts FROM to TO */
    TO to(FROM fromValue);
}
