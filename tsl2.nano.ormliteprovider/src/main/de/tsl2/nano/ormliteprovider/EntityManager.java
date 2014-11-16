/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 13.05.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.ormliteprovider;

import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.TypedQuery;

import org.apache.commons.logging.Log;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.mapped.MappedPreparedStmt;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableInfo;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.persistence.provider.NanoEntityManagerFactory;

/**
 * reduced OrmLite jpa persistence-provider extending {@link NanoEntityManagerFactory}.
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class EntityManager extends NanoEntityManagerFactory.AbstractEntityManager {
    private static final Log LOG = LogFactory.getLog(EntityManager.class);

    ConnectionSource connectionSource;

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
        try {
            connectionSource =
                new JdbcConnectionSource((String) props.get("jdbc.url"), (String) props.get("jdbc.username"),
                    (String) props.get("jdbc.password"));
            LOG.info("New Entitymanager for ORMLite created");
        } catch (SQLException e) {
            ManagedException.forward(e);
        }
    }

    @Override
    public <X> TypedQuery<X> createQuery(final String qstr, final Class<X> type) {
        return NanoEntityManagerFactory.instance().new AbstractQuery() {
            @Override
            public List getResultList() {
                try {
                    Class t = type;
                    if (type == null || Object.class.isAssignableFrom(type))
                        t = evaluateResultType(qstr);
//                    TableInfo tableInfo = new TableInfo(connectionSource, (BaseDaoImpl) dao(t), t);
//                    PreparedQuery preparedQuery =
//                        new MappedPreparedStmt(tableInfo, toNativeSQL(qstr), argFieldTypes, resultFieldTypes,
//                            argHolders, 100, t);
//                    return dao(t).query(preparedQuery);
                    return dao(t).queryRaw(toNativeSQL(qstr), getValuesAsStrings()).getResults();
                } catch (SQLException e) {
                    ManagedException.forward(e);
                    return null;
                }
            }

            @Override
            public int executeUpdate() {
                try {
                    return dao(type).updateRaw(qstr, getValuesAsStrings());
                } catch (SQLException e) {
                    ManagedException.forward(e);
                    return -1;
                }
            }

            protected String[] getValuesAsStrings() {
                Collection values = super.getNParameterValues();
                String[] result = new String[values.size()];
                int i = 0;
                for (Object object : values) {
                    result[i++] = Util.asString(object);
                }
                return result;
            }
        };
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
    public <T> T merge(T arg0) {
        try {
            dao(arg0.getClass()).createOrUpdate(arg0);
        } catch (SQLException e) {
            ManagedException.forward(e);
        }
        return null;
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
    public void detach(Object arg0) {
        Dao dao = dao(arg0.getClass());
        try {
            dao.getObjectCache().remove(arg0.getClass(), dao.extractId(arg0));
        } catch (SQLException e) {
            ManagedException.forward(e);
        }
    }

    @Override
    public void refresh(Object arg0) {
        Dao dao = dao(arg0.getClass());
        try {
            dao.refresh(arg0);
        } catch (SQLException e) {
            ManagedException.forward(e);
        }
    }

    @Override
    public boolean isOpen() {
        return connectionSource.isOpen();
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
        Dao dao = dao(arg0.getClass());
        try {
            return dao.getObjectCache().get(arg0.getClass(), dao.extractId(arg0)) != null;
        } catch (SQLException e) {
            ManagedException.forward(e);
            return false;
        }
    }

    @Override
    public void clear() {
        DaoManager.clearCache();
    }

    private final <T> Dao dao(Class<T> type) {
        try {
            Dao dao = DaoManager.lookupDao(connectionSource, type);
            if (dao == null)
                dao = DaoManager.createDao(connectionSource, type);
            return dao;
        } catch (Exception e) {
            LOG.warn("Did you annotate properties instead of fields? ORMLite is not able to read property (get*) annotations!");
            ManagedException.forward(e);
            return null;
        }
    }

}
