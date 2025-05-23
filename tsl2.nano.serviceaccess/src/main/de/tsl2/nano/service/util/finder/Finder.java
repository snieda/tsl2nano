/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Sep 21, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.service.util.finder;

import static de.tsl2.nano.service.util.ServiceUtil.getLogInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;

/**
 * Finder provider - usable for IQueryService.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class Finder {
    private static Log LOG = LogFactory.getLog(Finder.class);

    public static <FINDER extends AbstractFinder<T>, T> T findOne(FINDER... finder) {
    	Collection<T> find = find(finder);
    	if (find.size() != 1)
    		throw new IllegalStateException("exptected result of count=1, but was: " + find.size());
    	return find.iterator().next();
    }
    /** convenience, delegating to {@link #createQuery(Collection, Collection, AbstractFinder...)} */
    public static <FINDER extends AbstractFinder<T>, T> Collection<T> find(FINDER... finder) {
    	List<Object> parameter = new LinkedList<>();
    	return BeanContainer.instance().getBeansByQuery(createQuery(parameter, new LinkedList<>(), finder), false, parameter.toArray());
    }

    /**
     * creates the query for the given finders
     * 
     * @param <FINDER> finder-type
     * @param <T> result bean type
     * @param parameter parameter collection to be filled by this method
     * @param lazyRelations relation types to be filled by this method
     * @param finder at least one finder
     * @return query string
     */
    public static <FINDER extends AbstractFinder<T>, T> String createQuery(Collection<Object> parameter,
            Collection<Class<Object>> lazyRelations,
            FINDER... finder) {
        if (finder.length == 0) {
            throw ManagedException.illegalState("at least one finder must be defined!", parameter);
        } else if (finder[0].getResultType() == null) {
            throw ManagedException.illegalArgument(finder[0], "the first finder must define a resultType!");
        }
        StringBuffer qStr = new StringBuffer();//createStatement(finder[0].getResultType());
        for (int i = 0; i < finder.length; i++) {
            finder[i].addToQuery(i, qStr, parameter, lazyRelations);
            finder[i].getMaxResult();
        }
        arrangeParameterIndexes(qStr, 0, 0);
        LOG.debug(getLogInfo(qStr, parameter));
        return qStr.toString();
    }

    static void arrangeParameterIndexes(StringBuffer qStr, int fromIndex, int parameterIndex) {
		int pi = qStr.indexOf("?", fromIndex);
		if (pi == -1 || qStr.indexOf("?X") == -1)
			return;
		++parameterIndex;
		if (qStr.substring(pi, pi+2).equals("?X"))
			qStr.replace(pi+1, pi+2, String.valueOf(parameterIndex));
		arrangeParameterIndexes(qStr, pi+1, parameterIndex);
	}

	/**
     * creates an or-condition for the given finder
     * @param <FINDER> finder type
     * @param <T> result type
     * @param finder finder
     * @return given finder with influenced or-condition
     */
    public static <FINDER extends AbstractFinder<T>, T> FINDER or(FINDER finder) {
        return finder.setOrConnection();
    }
    /**
     * creates an and-condition (default) for the given finder
     * @param <FINDER> finder type
     * @param <T> result type
     * @param finder finder
     * @return given finder with influenced and-condition
     */
    public static <FINDER extends AbstractFinder<T>, T> FINDER and(FINDER finder) {
        return finder.setAndConnection();
    }
    /**
     * creates an and-not-condition for the given finder
     * @param <FINDER> finder type
     * @param <T> result type
     * @param finder finder
     * @return given finder with influenced and-not-condition
     */
    public static <FINDER extends AbstractFinder<T>, T> FINDER not(FINDER finder) {
        return finder.setNotConnection();
    }
    /**
     * id
     * 
     * @param <T> result bean type
     * @param id id to search for
     * @param relationsToLoad lazy collections to be preloaded. makes only sense on remote access.
     * @return Id finder expression
     */
    public static <T> Id<T> id(Class<T> type, Object id, Class... relationsToLoad) {
        return new Id<T>(type, id, relationsToLoad);
    }

    /**
     * all
     * 
     * @param <T> result bean type
     * @param id id to search for
     * @param relationsToLoad lazy collections to be preloaded. makes only sense on remote access.
     * @return Id finder expression
     */
    public static <T> All<T> all(Class<T> type, Class... relationsToLoad) {
        return new All<T>(type, relationsToLoad);
    }
    /**
     * between
     * 
     * @param <T> result bean type
     * @param minObject min object
     * @param maxObject max object
     * @param relationsToLoad lazy collections to be preloaded. makes only sense on remote access.
     * @return Between finder expression
     */
    public static <T> Between<T> between(T minObject, T maxObject, Class... relationsToLoad) {
        return new Between<T>(minObject, maxObject, relationsToLoad);
    }

    /**
     * example
     * 
     * @param <T> result bean type
     * @param example example object
     * @param relationsToLoad lazy collections to be preloaded. makes only sense on remote access.
     * @return Example finder expression
     */
    public static <T> Example<T> example(T example, Class... relationsToLoad) {
        return new Example<T>(example, relationsToLoad);
    }

    public static <T, H> Member<T, H> member(H holder,
            Class<T> beanType,
            Class... relationsToLoad) {
    	return member(holder, beanType, StringUtil.toFirstLower(holder.getClass().getSimpleName()), relationsToLoad);
    }
    /**
     * member
     * 
     * @param <T,H> result bean type and holder type
     * @param holder holder object
     * @param beanType member bean type
     * @param attributeName attribute name of bean type
     * @param relationsToLoad lazy collections to be preloaded. makes only sense on remote access.
     * @return Member finder expression
     */
    public static <T, H> Member<T, H> member(H holder,
            Class<T> beanType,
            String attributeName,
            Class... relationsToLoad) {
        return new Member<T, H>(holder, beanType, attributeName, relationsToLoad);
    }

    public static <T, H> Holder<T, H> holder(T member,
            Class<H> holderType,
            Class... relationsToLoad) {
        return holder(member, holderType, StringUtil.toFirstLower(member.getClass().getSimpleName()), relationsToLoad);
    }

    /**
     * holder
     * 
     * @param <T,H> result bean type and holder type
     * @param member member object
     * @param beanType member bean type
     * @param attributeName attribute name of bean type
     * @param relationsToLoad lazy collections to be preloaded. makes only sense on remote access.
     * @return Member finder expression
     */
    public static <T, H> Holder<T, H> holder(T member,
            Class<H> holderType,
            String attributeName,
            Class... relationsToLoad) {
        return new Holder<T, H>(member, holderType, attributeName, relationsToLoad);
    }

    /**
     * in selection
     * 
     * @param <T> result bean type
     * @param relationsToLoad lazy collections to be preloaded. makes only sense on remote access.
     * @return InSelection expression
     */
    public static <T> InSelection<T> inSelection(Class<T> resultType, String attribute, Collection<?> selection, Class... relationsToLoad) {
        return new InSelection<T>(resultType, attribute, selection, relationsToLoad);
    }

    /**
     * expression
     * 
     * @param <T> result bean type
     * @param expression query expression
     * @param relationsToLoad lazy collections to be preloaded. makes only sense on remote access.
     * @return query finder expression
     */
    public static <T> Expression<T> expression(Class<T> resultType,
            String queryString,
            boolean asSubSelect,
            Object[] args,
            Class... relationsToLoad) {
        return new Expression<T>(resultType, queryString, asSubSelect, args, relationsToLoad);
    }

    /**
     * order by
     * 
     * @param <T> result bean type
     * @param attributeNames order by names
     * @param relationsToLoad lazy collections to be preloaded. makes only sense on remote access.
     * @return order by expression
     */
    public static <T> OrderBy<T> orderBy(Class<T> resultType, String... attributeNames) {
        return new OrderBy<T>(resultType, attributeNames);
    }

    /**
     * group by
     * 
     * @param <T> result bean type
     * @param attributeNames group by names
     * @param relationsToLoad lazy collections to be preloaded. makes only sense on remote access.
     * @return order by expression
     */
    public static <T> GroupBy<T> groupBy(Class<T> resultType, String... attributeNames) {
        return new GroupBy<T>(attributeNames);
    }

    /**
     * union
     * 
     * @param <T> result bean type
     * @return union expression
     */
    public static <T> Union<T> union(Class<T> resultType) {
        return new Union<T>(resultType);
    }

}
