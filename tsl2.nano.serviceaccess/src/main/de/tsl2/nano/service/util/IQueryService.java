/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Sep 19, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.service.util;

import de.tsl2.nano.service.util.finder.AbstractFinder;

/**
 * Provides finder expression - instead of sql or ejb-ql. Additional ejb-ql-queries are possible. Finder expressions
 * will automatically create ejb-ql expressions through given transient bean instances.
 * <p/>
 * Combines multiple findBy-Expressions to create a complex query. Similar to criterias but working on intelligent
 * finders. At the moment, all concatenations are added with AND.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public interface IQueryService {
    /**
     * combines several find-expressions, like findByExample and findBetween etc., to create complex queries through
     * bean-attribute informations.
     * <p/>
     * example:
     * 
     * <pre>
     * result = ServiceFactory.getGenService().find(between(wvFrom, wvTo),
     *     expression(Wiedervorlage.class, qStbereich, false, argsPflName),
     *     expression(Wiedervorlage.class, qPflName, false, argsStbereich));
     * </pre>
     * 
     * @param <T> type to return
     * @param finder several finders to constrain the result
     * @return query result as list of beans
     */
    <FINDER extends AbstractFinder<T>, T> java.util.Collection<T> find(FINDER... finder);
}
