/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Oct 10, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.service.util.batch;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import de.tsl2.nano.exception.FormattedException;
import de.tsl2.nano.service.util.IGenericService;
import de.tsl2.nano.service.util.finder.AbstractFinder;
import de.tsl2.nano.serviceaccess.ServiceFactory;

/**
 * Batch loader that is optionally able to cache loaded data. Useable to optimize performance on loading several data
 * using a single communication line.
 * <p/>
 * 
 * <pre>
 * Features:
 * - load data through several queries on a single request
 * - cache base data for application access
 * </pre>
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class CachingBatchloader {
    private static CachingBatchloader self;

    /** genService */
    IGenericService genService;

    /** cache, holding all {@link Part}s where {@link Part#cache} is true */
    Map<String, Part<?>> cache;
    /** the next batch {@link Part}s to load through {@link #execute()} */
    Collection<Part<?>> nextBatchParts;

    /** mode on getting cached data through {@link #get(Class, String)} */
    int mode = MODE_AUTO;

    /** if data wasn't loaded, it will be done automatic */
    private static final int MODE_AUTO = 0;
    /** if data wasn't loaded, an exception will be thrown */
    private static final int MODE_ERROR = 1;

    /**
     * singelton constructor
     */
    private CachingBatchloader() {
        genService = ServiceFactory.getGenService();
        nextBatchParts = new LinkedList<Part<?>>();
        cache = new HashMap<String, Part<?>>();
    }

    /**
     * instance
     * 
     * @return singelton
     */
    public static final CachingBatchloader instance() {
        if (self == null)
            self = new CachingBatchloader();
        return self;
    }

    /**
     * adds the given part to the batch loading list - to be executed later
     * 
     * @param id part id
     * @param cache whether to cache the result in memory
     * @param finders for getting the data
     * @return this instance itself
     */
    public <T> CachingBatchloader add(String id, boolean cache, AbstractFinder<T>... finders) {
        Class<T> resultType = (Class<T>) finders[0].getResultType();
        Part<T> part = new Part<T>(id, resultType, cache);
        part.setFinders(finders);
        return add(part);
    }

    /**
     * adds the given part to the batch loading list - to be executed later
     * 
     * @param batchPart batch loading part
     * @return this instance itself
     */
    public CachingBatchloader add(Part<?> batchPart) {
        nextBatchParts.add(batchPart);
        return this;
    }

    /**
     * executes all batch parts. nothing will be returned to optimize performance. to access the data, use
     * {@link #get(Class, String)} and {@link #getSingle(Class, String)}.
     */
    public <T> void execute() {
        try {
            Part<T>[] loadedParts = genService.findBatch(nextBatchParts.toArray(new Part[0]));
            for (int i = 0; i < loadedParts.length; i++) {
                if (loadedParts[i].isCache())
                    cache.put(loadedParts[i].getId(), loadedParts[i]);
            }
        } finally {
            nextBatchParts.clear();
        }
    }

    /**
     * get cached result list
     * 
     * @param <T> result type
     * @param type result type
     * @param partId part id, identifying the data
     * @return data or null
     */
    public <T> Collection<T> get(Class<T> type, String partId) {
        Part<?> part = cache.get(partId);
        if (part == null && nextBatchParts.contains(new Part(partId))) {
            if (mode == MODE_AUTO)
                execute();
            else
                throw FormattedException.implementationError("the batch-loading wasn't started yet! you have to call 'execute' before getting the data",
                    partId);
        }
        return (Collection<T>) (part != null ? part.getResult() : null);
    }

    /**
     * get cached result as single object. use {@link #get(Class, String)} to obtain a list of elements.
     * 
     * @param <T> result type
     * @param type result type
     * @param partId part id, identifying the data
     * @return data or null
     */
    public <T> T getSingle(Class<T> type, String partId) {
        Collection<T> result = get(type, partId);
        if (result != null) {
            if (result.size() == 1)
                return result.iterator().next();
            else
                throw FormattedException.implementationError("the cache for " + type
                    + "|"
                    + partId
                    + " is not a single result. the cache contains"
                    + result.size()
                    + " elements for that!", "getSingle()", "get()");
        }
        return null;
    }

    /**
     * reloads all cache data
     */
    public void reloadCache() {
        genService = ServiceFactory.getGenService();
        nextBatchParts.addAll(cache.values());
        execute();
    }

    /**
     * resets the cached data
     * 
     * @param partIds parts to reset - if no parts are given, all parts will be reset.
     */
    public void reset(String... partIds) {
        nextBatchParts.clear();

        if (partIds.length == 0) {
            partIds = (String[]) cache.keySet().toArray();
        }
        for (int i = 0; i < partIds.length; i++) {
            if (cache.remove(partIds[i]) == null)
                throw FormattedException.implementationError("cache item couldn't be removed!",
                    partIds[i],
                    cache.keySet());
        }
    }
}
