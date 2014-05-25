/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Apr 26, 2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */
package de.tsl2.nano.persistence;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.EntityType;

import org.apache.commons.logging.Log;

import de.tsl2.nano.collection.ITransformer;
import de.tsl2.nano.core.Environment;
import de.tsl2.nano.core.classloader.RuntimeClassloader;
import de.tsl2.nano.core.classloader.TransformingClassLoader;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.service.util.AbstractStatelessServiceBean;
import de.tsl2.nano.service.util.IGenericService;

/**
 * It is a {@link RuntimeClassloader}, manipulating the found persistence.xml
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class PersistenceClassLoader extends TransformingClassLoader {
    private static final Log LOG = LogFactory.getLog(PersistenceClassLoader.class);

    /**
     * constructor
     * 
     * @param urls
     */
    public PersistenceClassLoader(URL[] urls) {
        super(urls);
        setTransformer(getPersistenceTransform());
    }

    /**
     * constructor
     * 
     * @param urls
     * @param parent
     */
    public PersistenceClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        setTransformer(getPersistenceTransform());
    }

    /**
     * transforms the standard persistence file path {@link Persistence#FILE_PERSISTENCE_XML} to
     * {@link Persistence#FILE_MYPERSISTENCE}.
     * 
     * @return path to transformed persistence-file: {@link Persistence#FILE_MYPERSISTENCE}
     */
    ITransformer<String, String> getPersistenceTransform() {
        return new ITransformer<String, String>() {
            @Override
            public String transform(String name) {
                /*
                 * as eclipselink uses another classloader this transformation doesn't work there.
                 */
//                if (name != null && name.contains(Persistence.FILE_PERSISTENCE_XML)) {
//                    LOG.info("manipulating url of " + name
//                        + " ==> using file: "
//                        + Persistence.getPath(Persistence.FILE_MYPERSISTENCE));
//                    name = Persistence.FILE_MYPERSISTENCE;
//                }
                return name;
            }
        };
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public List<Class> loadBeanClasses(String beanjar, String regExp, StringBuilder messages) {
        if (Environment.get("use.applicationserver", false) || Environment.get(EntityManager.class) != null)
            return super.loadBeanClasses(beanjar, regExp, messages);
        else {//local through entity types should be faster
            Collection<EntityType<?>> types =
                ((AbstractStatelessServiceBean) Environment.get(IGenericService.class)).getEntityTypes();
            LOG.info("loading " + types.size() + " entity-types from entitymanagerfactory");
            List<Class> list = new ArrayList(types.size());
            for (EntityType t : types) {
                if (t.getName().matches(regExp)) {
                    LOG.debug("loading entity-type: " + t.getJavaType());
                    list.add(t.getJavaType());
                }
            }
            Collections.sort(list, new Comparator<Class>() {
                @Override
                public int compare(Class o1, Class o2) {
                    return o1.getSimpleName().compareTo(o2.getSimpleName());
                }
            });
            return list;
        }
    }
}
