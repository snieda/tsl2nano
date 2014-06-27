/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Apr 26, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.service.util;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.junit.After;

import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.execution.ScriptUtil;
import de.tsl2.nano.serviceaccess.ServiceFactory;

/**
 * Base class for service tests.
 * <p>
 * overwrite the {@link #getService(Class)} method, if you want to login to a special user, to get the roles, the user
 * entity and the mandator entity. another way would be to create the service inside your setUp method.
 * <p>
 * to do a real login, use the method {@linkplain ServiceFactory#login(String, ClassLoader, String, String)}.
 * <p>
 * the creation of your test data will be done by the {@link #createTestData(String)} method, if
 * {@link #CREATE_SPECIFIC_TESTDATA} is true.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public abstract class BaseServiceTest extends RemoteServiceRunner<Serializable> {
    protected static final Log LOG = LogFactory.getLog(BaseServiceTest.class);
    /** should be used to ask before creating the test data in setUp() */
    protected static final boolean CREATE_SPECIFIC_TESTDATA = false;

    /** collection of entities, created through this test and to be removed after test */
    static Set<Object> entitiesToDestroy;

    /**
     * starts the ant build of this project and the target given by name. the ant script will recreate the local test
     * database!
     * 
     * @param name ant target
     */
    protected static void createTestData(String name) {
        if (CREATE_SPECIFIC_TESTDATA) {
            final Properties p = new Properties();
            p.put("kion.junit.name", name);
            ScriptUtil.ant("build-server.xml", "create.db.junit", p);
        } else {
            LOG.info("CREATE_SPECIFIC_TESTDATA=FALSE ==> createTestData '" + name + "' ignored");
        }
    }

    /**
     * call this to collect your new persisted entities. they will be removed after test finished. This entities may
     * have a state before persisting - without id - then it must be unique, to be found by example. If it is not
     * unique, an exception will be thrown on refreshing the beans from database.
     * 
     * @param entities persisted entities
     */
    protected void persisted(Object... entities) {
        if (entitiesToDestroy == null)
            entitiesToDestroy = new HashSet<Object>();
        for (Object e : entities) {
            entitiesToDestroy.add(e);
        }
    }

    /**
     * destroys the entity beans given by {@link #persisted(Object...)} and stored in {@link #entitiesToDestroy}.
     */
    @After
    public void destroyData() {
        if (entitiesToDestroy != null) {
            IGenericService service = getService(IGenericService.class);
            /*
             * get the refreshed beans to be removed from database.
             * if given beans are transient, they will be found by example
             */
            Set<Object> persistedEntities = new LinkedHashSet<Object>();
            for (Object e : entitiesToDestroy) {
                persistedEntities.add(service.findByExample(e));
            }
            service.removeCollection(persistedEntities);
        }
    }
}
