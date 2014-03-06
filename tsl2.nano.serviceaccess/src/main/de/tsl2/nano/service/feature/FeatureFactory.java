/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Sep 6, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.service.feature;

import java.lang.reflect.Proxy;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;

import de.tsl2.nano.exception.ManagedException;
import de.tsl2.nano.log.LogFactory;
import de.tsl2.nano.serviceaccess.ServiceFactory;

/**
 * provides a mechanism to load optional feature-implementations.<br>
 * on calling a feature request, an error may be thrown only, inside a full implemented and permitted (permitted to
 * user/mandator) feature. if no feature is assigned, the feature-mechanism will be ignored, means, all features are
 * available!
 * <p>
 * use:<br>
 * <code>FeatureFactory.instance().getImpl(myFeatureInterface).myFeatureMethod(...)</code><br>
 * if the feature is implemented and the role is assigned, the implementation will be excecuted. you don't need any if
 * clause etc.! it is possible to use {@link #isEnabled(Class)} instead to implement the feature in your current class,
 * but it is not recommended - it is not reusable and uses need if-clauses.
 * <p>
 * if the feature is assigned, but no implementation is available, an exception will be only thrown, if configurable
 * {@link #mustImplement()} returns true. If {@link #mustImplement()} returns false, the implementation will be done
 * through an empty {@link FeatureProxy}.<br>
 * see {@link #getImpl(Class)} to know how the implementation will be found.
 * <p>
 * to initialize the factory for the first time, a call of
 * {@link #createInstance(FeatureFactory, ClassLoader, String, String, String, Boolean)} is obligate - but this will be
 * done by the ServiceFactory.
 * <p>
 * all available feature interfaces are found in a specific package. please see {@link #properties} with keys
 * {@link #KEY_INTERF_PACKAGE_PREFIX}.
 * <p>
 * Tip:<br>
 * If you only want to ask, if a feature was enabled or activated, call {@link #isEnabled(Class)}.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class FeatureFactory {
    protected static final Log LOG = LogFactory.getLog(FeatureFactory.class);

    private static FeatureFactory self;

    private final ClassLoader classloader;
    private final Map<Class<?>, Object> implementations = new Hashtable<Class<?>, Object>();
    private final Properties properties;
    public static final String KEY_INTERF_PACKAGE_PREFIX = "interface_package_prefix";
    public static final String KEY_IMPL_PACKAGE_POSTFIX = "impl_package_postfix";
    public static final String KEY_IMPL_CLASS_POSTFIX = "impl_class_postfix";
    public static final String KEY_MUST_IMPLEMENT = "must_implement";

    /**
     * constructor
     * 
     * @param classloader classloader of your plugin
     * @param featureInterfacePrefix (optional) package postfix (default: null)
     * @param implPackagePrefix (optional) package postfix (default: .impl)
     * @param implClassPostfix (optional) class postifx (default: Impl)
     * @param mustImplement (optional) if true, the feature interfaces must be implemented (default: true)
     */
    private FeatureFactory(ClassLoader classloader,
            String featureInterfacePrefix,
            String implPackagePostfix,
            String implClassPostfix,
            Boolean mustImplement) {
        super();
        this.classloader = classloader;
        //IMPROVE: perhaps we load the properties from file
        properties = new Properties();

        properties.put(KEY_INTERF_PACKAGE_PREFIX, featureInterfacePrefix != null ? featureInterfacePrefix : "");
        properties.put(KEY_IMPL_PACKAGE_POSTFIX, implPackagePostfix != null ? implPackagePostfix : ".impl");
        properties.put(KEY_IMPL_CLASS_POSTFIX, implClassPostfix != null ? implClassPostfix : "Impl");
        properties.put(KEY_MUST_IMPLEMENT, mustImplement != null ? mustImplement.toString() : "false");
    }

    /**
     * must be called before using the singelton.
     * 
     * @param factoryImpl optional own instance of module factory
     * @param featureInterfacePrefix (optional) package postfix (default: null)
     * @param implPackagePrefix (optional) package postfix (default: .impl)
     * @param implClassPostfix (optional) class postifx (default: Impl)
     * @param mustImplement (optional) if true, the feature interfaces must be implemented (default: true)
     */
    public static final void createInstance(FeatureFactory factoryImpl,
            ClassLoader classloader,
            String featureInterfacePrefix,
            String implPackagePostfix,
            String implClassPostfix,
            Boolean mustImplement) {
        if (factoryImpl == null) {
            self = new FeatureFactory(classloader,
                featureInterfacePrefix,
                implPackagePostfix,
                implClassPostfix,
                mustImplement);
        } else {
            self = factoryImpl;
        }
        LOG.info("FeatureFactory singelton instance assigned: " + self);
    }

    /**
     * instance
     * 
     * @return singelton
     */
    public static final FeatureFactory instance() {
        assert self != null : "please call createInstance(..) before!";
        ServiceFactory.checkConnection();
        return self;
    }

    /**
     * returns the feature implementation (packed into a {@link FeatureProxy})<br>
     * the (optional) implementation must be found in<br>
     * interfaze-package + impl_package_postfix + interfaze-class + impl_class_postfix
     * <p>
     * example:
     * 
     * <pre>
     * feature-name: ErweiterteAktenSuche
     * ==> interfaze: mypackage.feature.IErweiterteAktenSucheFeature
     * ==> impl : mypackage.feature.impl.ErweiterteAktenSucheFeatureImpl
     * </pre>
     * 
     * if the feature-factory was initialized with {@link #mustImplement()} == true, an exception will be thrown, if no
     * implementation was found - otherwise an empty FeatureProxy will be used.
     * 
     * @param <T> interface type
     * @param interfaze source interface
     * @return implementation object
     */
    public <T> T getImpl(Class<T> interfaze) {
        return getImpl(interfaze, mustImplement());
    }

    /**
     * see description of {@link #getImpl(Class)}. if mustImplement is true, a {@link ClassNotFoundException} will be
     * thrown, if no implementation was found.
     * 
     * @param <T> interface type
     * @param interfaze source interface
     * @param mustImplement if true, an interfaze implementation must be found
     * @return implementation object
     */
    protected <T> T getImpl(Class<T> interfaze, boolean mustImplement) {
        T impl = (T) implementations.get(interfaze);
        if (impl == null) {
            checkInterfacePath(interfaze);
            final String implClassName = interfaze.getPackage().getName() + properties.getProperty(KEY_IMPL_PACKAGE_POSTFIX)
                + "."
                + interfaze.getSimpleName().substring(1)
                + properties.getProperty(KEY_IMPL_CLASS_POSTFIX);
            LOG.info("Loading Feature request for: " + implClassName);
            Class<T> clazz;
            try {
                clazz = (Class<T>) classloader.loadClass(implClassName);
            } catch (final ClassNotFoundException e) {
                if (mustImplement) {
                    LOG.error("FeatureFactory couldn't load implementation class " + implClassName);
                    ManagedException.forward(e);
                    return null;
                } else {
                    clazz = null;
                    LOG.warn("No Feature implementation '" + implClassName + "' found ==> using empty FeatureProxy!");
                }
            }
            T instance = null;
            if (clazz != null) {
                try {
                    instance = clazz.newInstance();
                } catch (final Exception e) {
                    ManagedException.forward(e);
                }
            }
            final T proxy = FeatureProxy.createBeanImplementation(interfaze, instance, classloader);
            implementations.put(interfaze, proxy);
            impl = proxy;
        }
        return impl;
    }

    /**
     * defines, whether a feature implementation must be present
     * 
     * @return true, if must_implement property is true
     */
    private boolean mustImplement() {
        return Boolean.valueOf(properties.getProperty(KEY_MUST_IMPLEMENT));
    }

    /**
     * delegates to {@link FeatureProxy#isEnabled()}
     * 
     * @param <T> interface type
     * @return true, if mandator feature was enabled
     */
    public <T> boolean isEnabled(Class<T> interfaze) {
        final Proxy proxyObject = (Proxy) getImpl(interfaze, false);
        final FeatureProxy<T> impl = (FeatureProxy<T>) (Proxy.getInvocationHandler(proxyObject));
        return impl.isEnabled();
    }

    /**
     * checks the given interface package path to start with defined feature interface path.
     * 
     * @param <T>
     * @param interfaze interface package to check
     */
    protected <T> void checkInterfacePath(Class<T> interfaze) {

        String featurePathConstraint = properties.getProperty(KEY_INTERF_PACKAGE_PREFIX);
        if (featurePathConstraint != null) {
            if (!interfaze.getPackage().getName().startsWith(featurePathConstraint)) {
                throw new ManagedException("tsl2nano.implementationerror",
                    new Object[] { "Feature-Interface: " + interfaze.getName(),
                        "Please use only feature interfaces in packages starting with '" + featurePathConstraint + "'!" });
            }
        } else { //not defined? ==> default mechansim
            featurePathConstraint = "feature";
            if (!interfaze.getPackage().getName().contains(featurePathConstraint)) {
                throw new ManagedException("tsl2nano.implementationerror",
                    new Object[] { "Feature-Interface: " + interfaze.getName(),
                        "Please use only feature interfaces in packages containing the substring '" + featurePathConstraint
                            + "'!" });
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return super.toString() + ": " + properties;
    }

}
