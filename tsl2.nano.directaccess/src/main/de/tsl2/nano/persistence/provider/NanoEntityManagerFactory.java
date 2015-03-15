/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 08.09.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.persistence.provider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.Metamodel;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;

/**
 * simplified base implementation of an {@link javax.persistence.EntityManagerFactory} and an abstract
 * {@link EntityManager}.
 * <p/>
 * Please extend the classes {@link NanoEntityManagerFactory.AbstractEntityManager},
 * {@link NanoEntityManagerFactory.AbstractQuery} and implement the abstract methods to have a reduced jpa persistence
 * provider. This reduced provider wont support Criterias etc.<br/>
 * <p/>
 * Call {@link #setEntityManagerImpl(String)} to provide your implementation to this factory class <br/>
 * Call {@link NanoEntityManagerFactory#createEntityManager(Map)} to get a new specialized {@link EntityManager}. The
 * property map has to contain the following specific entries:<br/>
 * 1. {@link #EM_IMPLEMENTATION}: pointing to your special EntityManager implementation<br/>
 * 2. jdbc.url: database connection url<br/>
 * 3. jdbc.user: database connection user<br/>
 * 4. jdbc.passwd: database connection password<br/>
 * 
 * @author ts
 * @version $Revision$
 */
public class NanoEntityManagerFactory implements javax.persistence.EntityManagerFactory {
    private static final Log LOG = LogFactory.getLog(NanoEntityManagerFactory.class);
    
    Collection<EntityManager> ems;
    Map<String, Object> props;
    EntityTransaction dummyTransaction;
    public static final String EM_IMPLEMENTATION = "entity.manager.implementation.class";
    static NanoEntityManagerFactory self;

    /**
     * constructor
     */

    NanoEntityManagerFactory() {
        LOG.debug("creating spimplified entitymanagerfactory: " + this);
        ems = new LinkedList<EntityManager>();
        props = new LinkedHashMap<String, Object>();
        dummyTransaction = new NTransaction();
    }

    public static final NanoEntityManagerFactory instance() {
        if (self == null)
            self = new NanoEntityManagerFactory();
        return self;
    }

    @Override
    public void close() {
        LOG.debug("closing entitymanagerfactory: " + this);
        for (EntityManager em : ems) {
            em.close();
        }
        ems = null;
    }

    public void setEntityManagerImpl(String clsEntityManagerImpl) {
        props.put(EM_IMPLEMENTATION, clsEntityManagerImpl);
    }

    @Override
    public EntityManager createEntityManager() {
        return createEntityManager(getProperties());
    }

    /**
     * createEntityManager
     * 
     * @param clsEntityManagerImpl class to be loaded and used to create an {@link EntityManager}.
     * @return special {@link EntityManager}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public EntityManager createEntityManager(String clsEntityManagerImpl, Map props) {
        this.props.putAll(props);
        setEntityManagerImpl(clsEntityManagerImpl);
        return createEntityManager(this.props);
    }

    @Override
    @SuppressWarnings({ "rawtypes" })
    public EntityManager createEntityManager(Map arg0) {
        String clsEM = (String) arg0.get(EM_IMPLEMENTATION);
        LOG.info("creating entity manager: " + clsEM);
        EntityManager em = (EntityManager) BeanClass.createInstance(clsEM, arg0);
        ems.add(em);
        return em;
    }

    @Override
    public Cache getCache() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Metamodel getMetamodel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PersistenceUnitUtil getPersistenceUnitUtil() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> getProperties() {
        return props;
    }

    @Override
    public boolean isOpen() {
        return ems != null;
    }

    @SuppressWarnings("rawtypes")
    public abstract class AbstractEntityManager implements javax.persistence.EntityManager {
        Map props;
        FlushModeType flushModeType = FlushModeType.AUTO;
        LockModeType lockModeType = LockModeType.OPTIMISTIC;

        private final Log LOG = LogFactory.getLog(AbstractEntityManager.class);

        /**
         * constructor
         * 
         * @param props
         */
        public AbstractEntityManager(Map props) {
            super();
            this.props = props;
        }

        @Override
        public Query createNamedQuery(String arg0) {
            return createNamedQuery(arg0, Object.class);
        }

        @Override
        public <T> TypedQuery<T> createNamedQuery(String arg0, Class<T> arg1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Query createNativeQuery(String arg0) {
            return createNativeQuery(arg0, Object.class);
        }

        @Override
        public Query createNativeQuery(String arg0, Class arg1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Query createNativeQuery(String arg0, String arg1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> TypedQuery<T> createQuery(CriteriaQuery<T> arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Query createQuery(String arg0) {
            return createQuery(arg0, Object.class);
        }

        @Override
        public <T> T find(Class<T> arg0, Object arg1, Map<String, Object> arg2) {
            LOG.warn("ignoring properties on find()");
            return find(arg0, arg1);
        }

        @Override
        public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2) {
            LOG.warn("ignoring lockmodetype on find()");
            return find(arg0, arg1);
        }

        @Override
        public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2, Map<String, Object> arg3) {
            LOG.warn("ignoring lockmodetype and properties on find()");
            return find(arg0, arg1);
        }

        @Override
        public void flush() {
            LOG.warn("ignoring flush");
        }

        @Override
        public CriteriaBuilder getCriteriaBuilder() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getDelegate() {
            return null;
        }

        @Override
        public NanoEntityManagerFactory getEntityManagerFactory() {
            return NanoEntityManagerFactory.self;
        }

        @Override
        public FlushModeType getFlushMode() {
            return flushModeType;
        }

        @Override
        public LockModeType getLockMode(Object arg0) {
            return lockModeType;
        }

        @Override
        public Metamodel getMetamodel() {
            return NanoEntityManagerFactory.self.getMetamodel();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Map<String, Object> getProperties() {
            return props;
        }

        @Override
        public <T> T getReference(Class<T> arg0, Object arg1) {
            return find(arg0, arg1);
        }

        @Override
        public EntityTransaction getTransaction() {
            return dummyTransaction;
        }

        @Override
        public void joinTransaction() {
            LOG.warn("ignoring joinTransaction");
        }

        @Override
        public void lock(Object arg0, LockModeType arg1) {
            lock(arg0, arg1, null);
        }

        @Override
        public void lock(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void persist(Object arg0) {
            merge(arg0);
        }

        @Override
        public void refresh(Object arg0) {
            refresh(arg0, null, null);
        }

        @Override
        public void refresh(Object arg0, Map<String, Object> arg1) {
            refresh(arg0, null, arg1);
        }

        @Override
        public void refresh(Object arg0, LockModeType arg1) {
            refresh(arg0, arg1);
        }

        @Override
        public void refresh(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
            LOG.warn("ignoring refresh");
        }

        @Override
        public void setFlushMode(FlushModeType arg0) {
            LOG.warn("ignoring flushmode");

        }

        @SuppressWarnings("unchecked")
        @Override
        public void setProperty(String arg0, Object arg1) {
            props.put(arg0, arg1);
        }

        @Override
        public <T> T unwrap(Class<T> arg0) {
            return null;
        }

    }

    /**
     * 
     * @author Tom
     * @version $Revision$
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public abstract class AbstractQuery<X> implements TypedQuery<X> {
        protected EntityManager em;
        protected Map<String, Object> props = new HashMap<String, Object>();
        protected Map<String, Parameter> parameter = new HashMap<String, Parameter>();
        protected int first = 0;
        protected int max = -1;

        FlushModeType flushModeType = FlushModeType.AUTO;
        LockModeType lockModeType = LockModeType.OPTIMISTIC;

        @Override
        public X getSingleResult() {
            List<X> resultList = getResultList();
            if (resultList != null) {
                if (resultList.size() > 1)
                    throw new IllegalStateException();
                else
                    return resultList.size() == 1 ? resultList.iterator().next() : null;
            } else
                return null;

        }

        @Override
        public int getFirstResult() {
            return first;
        }

        @Override
        public FlushModeType getFlushMode() {
            return em.getFlushMode();
        }

        @Override
        public Map<String, Object> getHints() {
            return props;
        }

        @Override
        public LockModeType getLockMode() {
            return lockModeType;
        }

        @Override
        public int getMaxResults() {
            return max;
        }

        @Override
        public Parameter<?> getParameter(String arg0) {
            return getParameter(arg0, Object.class);
        }

        @Override
        public Parameter<?> getParameter(int arg0) {
            return getParameter(String.valueOf(arg0), Object.class);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> Parameter<T> getParameter(String arg0, Class<T> arg1) {
            return parameter.get(arg0);
        }

        @Override
        public <T> Parameter<T> getParameter(int arg0, Class<T> arg1) {
            return getParameter(String.valueOf(arg0), arg1);
        }

        @Override
        public <T> T getParameterValue(Parameter<T> arg0) {
            return ((NanoEntityManagerFactory.NParameter<T>) arg0).getValue();
        }

        @Override
        public Object getParameterValue(String arg0) {
            return getParameterValue(getParameter(arg0));
        }

        @Override
        public Object getParameterValue(int arg0) {
            return getParameterValue(getParameter(arg0));
        }

        @Override
        public Set<Parameter<?>> getParameters() {
            return new HashSet<Parameter<?>>((Collection<? extends Parameter<?>>) parameter.values());
        }

        @Override
        public boolean isBound(Parameter<?> arg0) {
            return parameter.containsKey(arg0.getName());
        }

        @Override
        public TypedQuery setFirstResult(int arg0) {
            first = arg0;
            return this;
        }

        @Override
        public TypedQuery setFlushMode(FlushModeType arg0) {
            flushModeType = arg0;
            return this;
        }

        @Override
        public TypedQuery setHint(String arg0, Object arg1) {
            props.put(arg0, arg1);
            return this;
        }

        @Override
        public TypedQuery setLockMode(LockModeType arg0) {
            lockModeType = arg0;
            return this;
        }

        @Override
        public TypedQuery setMaxResults(int arg0) {
            max = arg0;
            return this;
        }

//        @Override
        public <T> TypedQuery<X> setParameter(Parameter<T> arg0, T arg1) {
            parameter.put(arg0.getName(),
                arg0 instanceof NParameter ? arg0 : new NParameter(arg0.getName(), arg0.getParameterType(), arg1));
            return this;
        }

        @Override
        public TypedQuery setParameter(String arg0, Object arg1) {
            return setParameter(new NParameter(arg0, null, arg1), arg1);
        }

        @Override
        public TypedQuery setParameter(int arg0, Object arg1) {
            return setParameter(String.valueOf(arg0), arg1);
        }

        @Override
        public TypedQuery setParameter(Parameter<Calendar> arg0, Calendar arg1, TemporalType arg2) {
            return setParameter(new NParameter<Calendar>(arg0.getName(), Calendar.class, arg1).setTemporalType(arg2),
                arg1);
        }

        @Override
        public TypedQuery setParameter(Parameter<Date> arg0, Date arg1, TemporalType arg2) {
            return setParameter(new NParameter<Date>(arg0.getName(), Date.class, arg1).setTemporalType(arg2), arg1);
        }

        @Override
        public TypedQuery setParameter(String arg0, Calendar arg1, TemporalType arg2) {
            return setParameter(arg0, new NParameter<Calendar>(arg0, Calendar.class, arg1).setTemporalType(arg2));
        }

        @Override
        public TypedQuery setParameter(String arg0, Date arg1, TemporalType arg2) {
            return setParameter(arg0, new NParameter<Date>(arg0, Date.class, arg1).setTemporalType(arg2));
        }

        @Override
        public TypedQuery setParameter(int arg0, Calendar arg1, TemporalType arg2) {
            return setParameter(String.valueOf(arg0),
                new NParameter<Calendar>(arg0, Calendar.class, arg1).setTemporalType(arg2));
        }

        @Override
        public TypedQuery setParameter(int arg0, Date arg1, TemporalType arg2) {
            return setParameter(String.valueOf(arg0),
                new NParameter<Date>(arg0, Date.class, arg1).setTemporalType(arg2));
        }

        @Override
        public <T> T unwrap(Class<T> arg0) {
            throw new UnsupportedOperationException();
        }

        /**
         * utility to find the desired result type
         * 
         * @param qstr sql-selection-statement
         * @return select result type
         */
        protected Class<X> evaluateResultType(String qstr) {
            //TODO: not-complete evaluation
            String clsName = StringUtil.substring(qstr, "from ", " t");
            Collection<Class> beanTypes = ENV.get("loadedBeanTypes", null);
            for (Class t : beanTypes) {
                if (t.getSimpleName().equals(clsName))
                    return t;
            }
            throw new IllegalArgumentException("The result type is not evaluable through the given select: " +qstr);
        }

        protected String toNativeSQL(String jpqlStatement) {
            //not-complete: only transforming: t --> t.*
            return jpqlStatement.replaceAll("select t\\s", "select t.* ");
        }
        
        protected Object getNParameter(String key) {
            return ((NParameter)parameter.get(key)).getValue();
        }
        
        /**
         * getParameterValues
         * @return all parameter values
         */
        protected Collection getNParameterValues() {
            Set<String> keys = parameter.keySet();
            List pars = new ArrayList(parameter.size());
            for (String k : keys) {
                pars.add(getNParameter(k));
            }
            return pars;
        }
    }

    /**
     * implementation of {@link Persistence}
     * 
     * @param <T>
     * @author Tom
     * @version $Revision$
     */
    public class NParameter<T> implements Parameter<T> {
        protected String name;
        protected Class<T> type;
        protected TemporalType temporalType;
        protected Integer position;
        protected T value;

        /**
         * constructor
         * 
         * @param name
         * @param type
         * @param position
         */
        public NParameter(String name, Class<T> type, T value) {
            super();
            this.name = name;
            this.type = type;
            this.value = value;
        }

        /**
         * constructor
         * 
         * @param name
         * @param type
         * @param position
         */
        public NParameter(Integer position, Class<T> type, T value) {
            super();
            this.name = String.valueOf(position);
            this.type = type;
            this.position = position;
            this.value = value;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Class<T> getParameterType() {
            return type;
        }

        @Override
        public Integer getPosition() {
            return position;
        }

        /**
         * @return Returns the value.
         */
        protected T getValue() {
            return value;
        }

        /**
         * @param value The value to set.
         */
        protected void setValue(T value) {
            this.value = value;
        }

        /**
         * @return Returns the temporalType.
         */
        protected TemporalType getTemporalType() {
            return temporalType;
        }

        /**
         * @param temporalType The temporalType to set.
         */
        protected NParameter<T> setTemporalType(TemporalType temporalType) {
            this.temporalType = temporalType;
            return this;
        }
    }

    /**
     * 
     * @author Tom, Thomas Schneider
     * @version $Revision$
     */
    public class NTransaction implements EntityTransaction {
        private final Log LOG = LogFactory.getLog(NTransaction.class);

        protected boolean rollbackOnly;

        @Override
        public void commit() {
            LOG.warn("ignoring transaction request!");
        }

        @Override
        public void begin() {
            LOG.warn("ignoring transaction request!");
        }

        @Override
        public boolean getRollbackOnly() {
            return rollbackOnly;
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public void rollback() {
            LOG.warn("ignoring transaction request!");
        }

        @Override
        public void setRollbackOnly() {
            rollbackOnly = true;
        }
    }
}
