/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 13.05.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.core.cls;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.AppLoader;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.classloader.RuntimeClassloader;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;

/**
 * WORKS ONLY IN ORACLES JAVA IMPLEMENTATION
 * <p/>
 * is able to find all types of java elements like classes, annotations,
 * methods, fields - per reflection.
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ClassFinder {
	private static final Log LOG = LogFactory.getLog(ClassFinder.class);

	private static ClassFinder self = null;

	private Set<String> packageNames; // only for performance aspects 
	private Set<Class<?>> classes;

	public static ClassFinder self() {
		if (self == null)
			self = new ClassFinder();
		return self;
	}
	
	ClassFinder() {
		this(Thread.currentThread().getContextClassLoader());
	}

	/**
	 * constructor
	 */
	ClassFinder(ClassLoader classLoader) {
		init(classLoader);
	}

	private void init(ClassLoader classLoader) {
		ClassLoader baseClassLoader = classLoader;
		packageNames = new HashSet<>();
		classes = new HashSet<>();

		//TODO: let our own classloader implementations provide the loaded classes!
		try {
			addClasses(ClassLoader.getSystemClassLoader(), classes);
			while ((classLoader = addClasses(classLoader, classes)) != null)
				;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		collectPackageClasses(baseClassLoader, classes);
		LOG.info("---------------------------------------------------------------------");
		LOG.info("ClassFinder created for " + classes.size() + " classes");
		LOG.info("---------------------------------------------------------------------");
	}

	/**
	 * SPECIFIC IMPLEMENTATION FOR ORACLES JAVA IMPLEMENTATION!
	 * 
	 * @param classLoader
	 * @return parent ClassLoader
	 */
	private static ClassLoader addClasses(ClassLoader classLoader, Set<Class<?>> classes) {
		if (AppLoader.isJdkOracle()) {
		classes.addAll(
				(Collection<? extends Class<?>>) new PrivateAccessor(classLoader).member("classes", Vector.class));
//		return (ClassLoader) new PrivateAccessor(classLoader).call("getParent", ClassLoader.class);
		} else {
			LOG.warn("cannot access oracle specific member of classloader. this may result in problems on finding classes.");
		}
		return classLoader.getParent();
	}

//	private void collectPackageClasses(ClassLoader classLoader) {
//		collectPackageClasses(ClassLoader.getSystemClassLoader(), classes);
//		while ((classLoader = collectPackageClasses(classLoader, classes)) != null)
//			;
//	}

	ClassLoader collectPackageClasses(ClassLoader cl, Set<Class<?>> classes) {
		Package[] packages = cl instanceof RuntimeClassloader ? ((RuntimeClassloader)cl).getPackages() : Package.getPackages();
		LOG.debug("---------------------------------------------------------------------");
		LOG.debug(packages.length + " Packages will be loaded on classloader " + cl);
		LOG.debug("---------------------------------------------------------------------");
		for (int i = 0; i < packages.length; i++) {
			if (!packageNames.contains(packages[i].getName()))
				classes.addAll(collectPackageClasses(cl, packages[i].getName(), classes));
		}
		LOG.info(classes.size() + " classes scanned by ClassFinder");
		return cl.getParent();
	}

	private Collection<Class<?>> collectPackageClasses(ClassLoader cl, String pack, Set<Class<?>> classes) {
		if (LOG.isDebugEnabled())
			System.out.print("loading classes on " + pack + ": ");
		int i = 0;
		try {
//			Enumeration<URL> entries = cl.getResources(pack.replace('.', '/'));
//			// recurse into sub-packages
//			while (entries.hasMoreElements()) {
//				collectPackageClasses(cl, entries.nextElement().getPath(), classes);
//			}

			URL upackage = cl.getResource(pack.replace('.', '/'));
			if (upackage == null) {
				return classes;
			}
			BufferedReader dis = new BufferedReader(new InputStreamReader(((InputStream) upackage.getContent())));
			String line = null;
			while ((line = dis.readLine()) != null) {
				if (line.endsWith(".class")) {
					try {
						i++;
						System.out.print(".");
						classes.add(cl.loadClass(pack + "." + line.substring(0, line.lastIndexOf('.'))));
					} catch (ClassNotFoundException e) {
						ManagedException.forward(e);
					}
				} else {
					classes.addAll(collectPackageClasses(cl, pack + "." + line, classes));
				}
			}
		} catch (IOException e1) {
			ManagedException.forward(e1);
		} finally {
			if (LOG.isDebugEnabled())
				System.out.println(i);
		}
		packageNames.add(pack);
		return classes;
	}

	private void addFromPackageAnnotation(Class<? extends PackageDescriptor> packageDescriptor) {
		classes.addAll(getImplementations(packageDescriptor));
	}
	
	public static Set<Class<?>> getImplementations(Class<? extends PackageDescriptor> packageDescriptor) {
    	Package[] packages = Package.getPackages();
    	Set<Class<?>> classes = new HashSet<>();
    	for (int i = 0; i < packages.length; i++) {
    		PackageDescriptor a = packages[i].getAnnotation(packageDescriptor);
			if (a != null)
				classes.addAll(Arrays.asList(a.implementations()));
		}
    	return classes;
	}

	/**
	 * since java 6 the {@link ServiceLoader} provides extensions of a given base
	 * type or interface. these extensions must be registered through an entry in
	 * META-INF/services.xml.
	 * 
	 * @param baseType base type or interface to search extensions for
	 */
	private <T> void addFromServiceLoader(Class<T> baseType) {
		ServiceLoader<T> loader = ServiceLoader.load(baseType, Thread.currentThread().getContextClassLoader());
		for (T impl : loader) {
			classes.add(impl.getClass());
		}
	}

	public <T> Collection<Class<T>> findClass(Class<T> base) {
		return (Collection<Class<T>>) fuzzyFind(null, base, -1, null).values();
	}

	public Class findClass(String filter) {
		Map<Double, Class> result = fuzzyFind(filter);
		return result.size() > 0 && result.containsKey(1d) ? result.get(1d) : null;
	}

	public <M extends Map<Double, Class>> M fuzzyFind(String filter) {
		return fuzzyFind(filter, Class.class, -1, null);
	}

	/**
	 * finds all classes/methods/fields of the given classloader fuzzy matching the
	 * given filter, having the given modifiers (or modifiers is -1) and having the
	 * given annotation (or annotation is null)
	 * 
	 * @param filter
	 *            fuzzy filter
	 * @param resultType
	 *            (optional) restricts to search for classes/extensions , methods or
	 *            fields. If it is {@link Method}, only method matches will be
	 *            returned. if it is an interface, all matching implementations will
	 *            be returned. The class itself will not be returned
	 * @param modifier
	 *            (optional, -1: all) see {@link Modifier}.
	 * @param annotation
	 *            (optional) class/method/field annotation as constraint.
	 * @return all found java elements sorted by matching quote down. best quote is
	 *         1.
	 */
	public <T, M extends Map<Double, T>> M fuzzyFind(String filter, Class<T> resultType, int modifier,
			Class<? extends Annotation> annotation) {
		Map<Double, T> result = new TreeMap<Double, T>() {
			@Override
			public T put(Double key, T value) {
				while (containsKey(key))
					key += 0000000001;
				return super.put(key, value);
			}
		};
		if (filter != null)
			collectPackageClasses(Thread.currentThread().getContextClassLoader(), filter, classes);
		else
			collectPackageClasses(Thread.currentThread().getContextClassLoader(), classes);
		if (resultType != null)
			addFromServiceLoader(resultType);
		if (annotation != null && PackageDescriptor.class.isAssignableFrom(annotation))
			addFromPackageAnnotation((Class<? extends PackageDescriptor>) annotation);

		Class cls;
		double match;
		boolean addMethods = resultType == null || Method.class.isAssignableFrom(resultType);
		boolean addFields = resultType == null || Field.class.isAssignableFrom(resultType);
		boolean addClasses = resultType == null || Class.class.isAssignableFrom(resultType)
				|| (!addMethods && !addFields);
		// clone the classes vector to avoid concurrent modification - when the
		// classloader is working
		for (Iterator<Class<?>> it = /* ((Vector<Class<?>>) */classes/* .clone()) */.iterator(); it.hasNext();) {
			cls = it.next();
			if (addClasses) {
				if ((modifier < 0 || cls.getModifiers() == modifier)
						&& (annotation == null || PackageDescriptor.class.isAssignableFrom(annotation)
							|| cls.getAnnotation(annotation) != null)) {
					match = filter != null ? StringUtil.fuzzyMatch(cls.getName(), filter) : 1;
					if (match > 0) {
						if (resultType == null || Class.class.isAssignableFrom(resultType)
								|| resultType.isAssignableFrom(cls))
							if (!cls.equals(resultType)) // don't return the base class itself
								result.put(match, (T) cls);
					}
				}
			}
			if (addMethods) {
				result.putAll((Map<Double, T>) fuzzyFindMethods(cls, filter, modifier, annotation));
			}
			if (addFields) {
				result.putAll((Map<Double, T>) fuzzyFindFields(cls, filter, modifier, annotation));
			}
		}
		return (M) result;
	}

	public Map<Double, Method> fuzzyFindMethods(Class cls, String filter, int modifier,
			Class<? extends Annotation> annotation) {
		HashMap<Double, Method> map = new HashMap<Double, Method>() {
			@Override
			public Method put(Double key, Method value) {
				while (containsKey(key))
					key += 0000000001;
				return super.put(key, value);
			}
		};
		Method[] methods = Modifier.isPublic(modifier) ? cls.getMethods() : cls.getDeclaredMethods();
		double match;
		for (int i = 0; i < methods.length; i++) {
			if ((modifier < 0 || methods[i].getModifiers() == modifier)
					&& (annotation == null || methods[i].getAnnotation(annotation) != null)) {
				match = StringUtil.fuzzyMatch(methods[i].toGenericString(), filter);
				if (match > 0)
					map.put(match, methods[i]);
			}
		}
		return map;
	}

	public Map<Double, Field> fuzzyFindFields(Class cls, String filter, int modifier,
			Class<? extends Annotation> annotation) {
		HashMap<Double, Field> map = new HashMap<Double, Field>() {
			@Override
			public Field put(Double key, Field value) {
				while (containsKey(key))
					key += 0000000001;
				return super.put(key, value);
			}
		};
		Field[] fields = Modifier.isPublic(modifier) ? cls.getFields() : cls.getDeclaredFields();
		double match;
		for (int i = 0; i < fields.length; i++) {
			if ((modifier < 0 || fields[i].getModifiers() == modifier)
					&& (annotation == null || fields[i].getAnnotation(annotation) != null)) {
				match = StringUtil.fuzzyMatch(fields[i].toGenericString(), filter);
				if (match > 0)
					map.put(match, fields[i]);
			}
		}
		return map;
	}

	public void reset() {
		init(Thread.currentThread().getContextClassLoader());
	}
}
