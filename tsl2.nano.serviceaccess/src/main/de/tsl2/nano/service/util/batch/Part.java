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

import java.io.Serializable;
import java.util.Collection;

import de.tsl2.nano.service.util.finder.AbstractFinder;

/**
 * batch part structure. used by {@link CachingBatchloader} and IBatchService. each batch part is totally identified
 * by its id!. if caching is true, the {@link CachingBatchloader} will hold the results in its cache.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class Part<T> implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -6843134692781385005L;

    /** identifier for this part  */
    String id;
    /** beanType of data to load */
    Class<T> beanType;
    /** finders define the query to load the data */
    AbstractFinder<T>[] finders;
    /** if true, the {@link CachingBatchloader} will hold the {@link #result} in its cache */
    boolean cache;
    /** result of query, after execution */
    Collection<T> result;

    /**
     * constructor
     * 
     * @param id see #id
     */
    public Part(String id) {
        this(id, null, false);
    }

    /**
     * constructor
     * 
     * @param id see {@link #id}
     * @param beanType see {@link #beanType}
     * @param cache see {@link #cache}
     */
    public Part(String id, Class<T> beanType, boolean cache) {
        super();
        this.id = id;
        this.beanType = beanType;
        this.cache = cache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Part other = (Part) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    /**
     * @return Returns the {@link #id}.
     */
    public String getId() {
        return id;
    }

    /**
     * @param id The {@link #id} to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return Returns the {@link #beanType}.
     */
    public Class<T> getBeanType() {
        return beanType;
    }

    /**
     * @param beanType The {@link #beanType} to set.
     */
    public void setBeanType(Class<T> beanType) {
        this.beanType = beanType;
    }

    /**
     * @return Returns the {@link #finders}.
     */
    public AbstractFinder<T>[] getFinders() {
        return finders;
    }

    /**
     * @param finders The {@link #finders} to set.
     */
    public void setFinders(AbstractFinder<T>[] finders) {
        this.finders = finders;
    }

    /**
     * @return Returns the {@link #cache}.
     */
    public boolean isCache() {
        return cache;
    }

    /**
     * @param cache The {@link #cache} to set.
     */
    public void setCache(boolean cache) {
        this.cache = cache;
    }

    /**
     * @return Returns the {@link #result}.
     */
    public Collection<T> getResult() {
        return result;
    }

    /**
     * @param result The {@link #result} to set.
     */
    public void setResult(Collection<T> result) {
        this.result = result;
    }
}
