/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts, Thomas Schneider
 * created on: 19.05.2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */
package de.tsl2.nano.service.util;

import java.util.Collection;

import javax.ejb.Remote;

/**
 * provides services to work on beans with named queries. used by {@link IGenericService}.
 * 
 * @author ts, Thomas Schneider
 * @version $Revision$
 */
@Remote
public interface INamedQueryService {
    static final String NAMEDQUERY_ALL = "findAll";
    static final String NAMEDQUERY_ID = "findById";
    static final String NAMEDQUERY_BETWEEN = "findBetween";
    static final String NAMEDQUERY_INSERT = "insert";
    static final String NAMEDQUERY_UPDATE = "update";
    static final String NAMEDQUERY_DELETE = "delete";

    /**
     * used to work with named queries - on entities without table or view binding.
     * 
     * @param <T> virtual entity type
     * @param beanType entity without table or view binding
     * @param namedQuery named query (see annotation on given entity)
     * @param args query arguments
     * @return list of entities
     */
    <T> Collection<T> findByNamedQuery(Class<T> beanType, String namedQuery, int maxResult, Object... args);

}
