/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 08.09.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.ormliteprovider;

import java.sql.SQLException;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.Metamodel;

import org.apache.commons.logging.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;

import de.tsl2.nano.exception.ManagedException;
import de.tsl2.nano.log.LogFactory;

/**
 * 
 * @author ts
 * @version $Revision$
 */
public class EntityManager implements javax.persistence.EntityManager {
    Map props;
    ConnectionSource connectionSource;
    private static final Log LOG = LogFactory.getLog(EntityManager.class);

    /**
     * constructor
     * 
     * @param props
     */
    public EntityManager(Map props) {
        super();
        this.props = props;
        try {
            connectionSource = new JdbcConnectionSource((String) props.get("jdbc.url"));
            LOG.info("New Entitymanager for ORMLite created");
        } catch (SQLException e) {
            ManagedException.forward(e);
        }
    }

    private final <T> Dao<Class<T>, Object> dao(Class<T> type) {
        try {
            return DaoManager.createDao(connectionSource, type);
        } catch (SQLException e) {
            ManagedException.forward(e);
            return null;
        }
    }
    
    @Override
    public void clear() {
        // TODO Auto-generated method stub

    }

    @Override
    public void close() {
        try {
            connectionSource.close();
        } catch (SQLException e) {
            ManagedException.forward(e);
        }
    }

    @Override
    public boolean contains(Object arg0) {
        return false;
    }

    @Override
    public Query createNamedQuery(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> TypedQuery<T> createNamedQuery(String arg0, Class<T> arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Query createNativeQuery(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Query createNativeQuery(String arg0, Class arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Query createNativeQuery(String arg0, String arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Query createQuery(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> TypedQuery<T> createQuery(String arg0, Class<T> arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void detach(Object arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public <T> T find(Class<T> arg0, Object arg1) {
        try {
            dao(arg0).queryForId(arg1);
        } catch (SQLException e) {
            ManagedException.forward(e);
        }
        return null;
    }

    @Override
    public <T> T find(Class<T> arg0, Object arg1, Map<String, Object> arg2) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2, Map<String, Object> arg3) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void flush() {
        // TODO Auto-generated method stub

    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getDelegate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FlushModeType getFlushMode() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LockModeType getLockMode(Object arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Metamodel getMetamodel() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Object> getProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T getReference(Class<T> arg0, Object arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EntityTransaction getTransaction() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isOpen() {
        return connectionSource.isOpen();
    }

    @Override
    public void joinTransaction() {
        // TODO Auto-generated method stub

    }

    @Override
    public void lock(Object arg0, LockModeType arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void lock(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public <T> T merge(T arg0) {
        try {
            dao(arg0.getClass()).commit(connectionSource.getReadWriteConnection());
        } catch (SQLException e) {
            ManagedException.forward(e);
        }        return null;
    }

    @Override
    public void persist(Object arg0) {
        merge(arg0);
    }

    @Override
    public void refresh(Object arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void refresh(Object arg0, Map<String, Object> arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void refresh(Object arg0, LockModeType arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void refresh(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void remove(Object arg0) {
        try {
            dao(arg0.getClass()).deleteById(arg0);
        } catch (SQLException e) {
            ManagedException.forward(e);
        }
    }

    @Override
    public void setFlushMode(FlushModeType arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setProperty(String arg0, Object arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public <T> T unwrap(Class<T> arg0) {
        // TODO Auto-generated method stub
        return null;
    }

}
