/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 13.05.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.ebeanprovider;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.persistence.EntityGraph;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaUpdate;

import org.apache.commons.logging.Log;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.Query;
import com.avaje.ebean.SqlUpdate;
import com.avaje.ebean.cache.ServerCache;
import com.avaje.ebean.config.ServerConfig;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.persistence.provider.NanoEntityManagerFactory;
import de.tsl2.nano.persistence.provider.NanoEntityManagerFactory.AbstractEntityManager;

/**
 * reduced OrmLite jpa persistence-provider extending {@link NanoEntityManagerFactory}.
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class EntityManager extends AbstractEntityManager {
    private static final Log LOG = LogFactory.getLog(EntityManager.class);

    EbeanServer server;
    
    public EntityManager() {
        this(new LinkedHashMap());
    }

    /**
     * constructor
     * 
     * @param props
     */
    public EntityManager(Map props) {
        NanoEntityManagerFactory.instance().super(props);
        final String ebeanTemplate = "ebean.properties.tml";
        ENV.extractResource(ebeanTemplate);
        Properties ep = new Properties();
        try {
            ep.load(FileUtil.getFile(ENV.getConfigPath() + ebeanTemplate));
            if (props.containsKey("jpa.beansjar"))
            	ep.setProperty("ebean.search.jars", (String) props.get("jpa.beansjar"));
            ep.setProperty("datasource.custom.username", (String) props.get("jdbc.username"));
            ep.setProperty("datasource.custom.password", (String) props.get("jdbc.password"));
            ep.setProperty("datasource.custom.databaseUrl", (String) props.get("jdbc.url"));
            ep.setProperty("datasource.custom.databaseDriver", (String) props.get("jdbc.driver"));
            ep.store(new FileOutputStream(ENV.getConfigPath() + "ebean.properties"), "generated by tsl2.nano.ebeanprovider - do not edit!");
        } catch (IOException e) {
            ManagedException.forward(e);
        }
    }

    EbeanServer getServer() {
        if (server == null) {
            /*
             * as the default ebean-mechanism seems not to be able to load a 
             * configured bean-jar-file through the ebean.properties, we have
             * configure it programatically. 
             */
            ServerConfig config = new ServerConfig();
            config.setName("custom");
            config.setDefaultServer(true);
            config.loadFromProperties();
            Collection<Class> beanTypes = ENV.get("service.loadedBeanTypes", null);
            for (Class cls : beanTypes) {
                config.addClass(cls);
            }
            server = EbeanServerFactory.create(config);
            LOG.info("New Entitymanager for EBean created");
        }
        return server;
    }
    
//    @Override
    public <X> TypedQuery<X> createQuery(final String qstr, final Class<X> type) {
        return NanoEntityManagerFactory.instance().new AbstractQuery() {
//            @Override
            public List getResultList() {
                    Class t = type;
                    if (type == null || Object.class.isAssignableFrom(type)) {
                        t = evaluateResultType(qstr);
                    }
                    Query query = getServer().createQuery(t, qstr);
                    Set<String> keySet = parameter.keySet();
                    for (String k : keySet) {
                        query.setParameter(k, getNParameter(k));
                    }
                    return query.findList();
            }

//            @Override
            public int executeUpdate() {
                SqlUpdate query = getServer().createSqlUpdate(qstr);
                Set<String> keySet = parameter.keySet();
                for (String k : keySet) {
                    query.setParameter(k, getNParameter(k));
                }
                return Ebean.execute(query);
            }
            
        };
    }

//    @Override
    public <T> T find(Class<T> arg0, Object arg1) {
        return Ebean.find(arg0, arg1);
    }

//    @Override
    public <T> T merge(T arg0) {
        Ebean.save(arg0);
        return arg0;
    }

//    @Override
    public void remove(Object arg0) {
        Ebean.delete(arg0);
    }

//    @Override
    public void detach(Object arg0) {
        Object beanId = getServer().getBeanId(arg0);
        Ebean.getServerCacheManager().getBeanCache(arg0.getClass()).remove(beanId);
    }

    @Override
    public void refresh(Object arg0) {
        Ebean.refresh(arg0);
    }

//    @Override
    public boolean isOpen() {
        //how can I check that?
        return getServer() != null;
    }

//    @Override
    public void close() {
        //this would be normally to much...
        getServer().shutdown(true, true);
    }

//    @Override
    public boolean contains(Object arg0) {
        ServerCache cache = Ebean.getServerCacheManager().getBeanCache(arg0.getClass());
        Object beanId = getServer().getBeanId(arg0);
        return cache.get(beanId) != null;
    }

//    @Override
    public void clear() {
        Ebean.getServerCacheManager().clearAll();
    }

//    @Override
    public <T> EntityGraph<T> createEntityGraph(Class<T> arg0) {
        // TODO Auto-generated method stub
        return null;
    }

//    @Override
    public EntityGraph<?> createEntityGraph(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

//    @Override
    public StoredProcedureQuery createNamedStoredProcedureQuery(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

//    @Override
    public javax.persistence.Query createQuery(CriteriaUpdate arg0) {
        // TODO Auto-generated method stub
        return null;
    }

//    @Override
    public javax.persistence.Query createQuery(CriteriaDelete arg0) {
        // TODO Auto-generated method stub
        return null;
    }

//    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

//    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String arg0, Class... arg1) {
        // TODO Auto-generated method stub
        return null;
    }

//    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String arg0, String... arg1) {
        // TODO Auto-generated method stub
        return null;
    }

//    @Override
    public EntityGraph<?> getEntityGraph(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

//    @Override
    public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> arg0) {
        // TODO Auto-generated method stub
        return null;
    }

//    @Override
    public boolean isJoinedToTransaction() {
        // TODO Auto-generated method stub
        return false;
    }
}