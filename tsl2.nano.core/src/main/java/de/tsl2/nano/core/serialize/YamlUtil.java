/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 01.06.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.core.serialize;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.CollectionNode;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.ClassFinder;
import de.tsl2.nano.core.cls.PrivateAccessor;
import de.tsl2.nano.core.util.DelegationHandler;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.Util;

/**
 * serializes (dumps) and de-serializes (loads) yaml documents and beans. default properties are BeanAccess.FIELD and
 * skipEmpties. call {@link #dump(Object, String)} to serialize to a file. reload that calling
 * {@link #load(File, Class)}.
 * 
 * @author Tom
 * @version $Revision$
 */
public class YamlUtil {
    private static Representer representer;
    private static Constructor constructor;

    /**
     * dumps the given object (document / bean) to the given file.
     * 
     * @param obj object to dump
     * @param fileName file name of dump
     */
    public static void dump(Object obj, String fileName) {
        FileUtil.writeBytes(dump(obj).getBytes(), fileName, false);
    }

    /**
     * @delegates to {@link #dump(Object, boolean, boolean, String)} with skipEmpties=true, fields=true, and
     *            {@link Util#FRAMEWORK_PACKAGE}
     */
    public static String dump(Object obj) {
        return dump(obj, true, true, Util.FRAMEWORK_PACKAGE);
    }

    /**
     * dumps the given object to a string
     * 
     * @param obj to be dumped
     * @param skipEmpties whether to skip nulls and empty (like empty collections, maps)
     * @param fields whether to access private fields - or public bean properties
     * @param shortCutPackage package to generate simple-class-name shortcuts
     * @return dumps as string
     */
    public static String dump(Object obj, boolean skipEmpties, boolean fields, String shortCutPackage) {
        Yaml yaml = new Yaml(getRepresenter(skipEmpties, shortCutPackage));
        if (fields)
        	yaml.setBeanAccess(BeanAccess.FIELD);
        return yaml.dump(obj);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static Representer getRepresenter(boolean skipEmpties, String shortCutPackage) {
    	if (representer == null) {
        	representer = skipEmpties ? new SkipEmptyRepresenter() : new Representer();
	        Map<Double, Class> classes = ClassFinder.self().fuzzyFind(shortCutPackage);
	        classes.put(-1d, Class.class);
	        for (Class cls : classes.values()) {
	            if (isContructable(cls))
	                representer.addClassTag(cls, new Tag(cls.getSimpleName()));
	        }
    	}
//        DumperOptions doptions = new DumperOptions();
//        doptions.setDefaultFlowStyle(FlowStyle.AUTO);
        return representer;
    }

    /**
     * loads a dump yaml document (bean) to an instance of the given type.
     * 
     * @param file file to load
     * @param type type of object to de-serialized
     * @return filled object
     */
    public static <T> T load(File file, Class<T> type) {
        try {
            return load(new FileInputStream(file), type);
        } catch (FileNotFoundException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    public static <T> T load(InputStream stream, Class<T> type) {
        return createLoaderYaml().loadAs(stream, type);
    }

    public static <T> T load(String txt, Class<T> type) {
        return createLoaderYaml().loadAs(txt, type);
    }

	private static Yaml createLoaderYaml() {
		Yaml yaml = new Yaml(getConstructor(true, Util.FRAMEWORK_PACKAGE), new Representer(), new DumperOptions(), getLoaderOptions());
        yaml.setBeanAccess(BeanAccess.FIELD);
		return yaml;
	}

	private static LoaderOptions getLoaderOptions() {
		LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setMaxAliasesForCollections(200);
		return loaderOptions;
	}

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static Constructor getConstructor(boolean fields, String shortCutPackage) {
    	if (constructor == null) {
	        constructor = new PostConstructor();
	        Map<Double, Class> classes = ClassFinder.self().fuzzyFind(shortCutPackage);
	        TypeDescription typeDef;
	        for (Class cls : classes.values()) {
	            if (isContructable(cls)) {
	                typeDef = new TypeDescription(cls, cls.getSimpleName());
	                constructor.addTypeDescription(typeDef);
	            }
	        }
    	}
        return constructor;
    }

	private static boolean isContructable(Class cls) {
		return cls.getSimpleName().length() > 0  && !cls.getSimpleName().endsWith("Util") 
				&& BeanClass.hasDefaultConstructor(cls) && !cls.isAnnotation();
	}

    public static void reset() {
    	representer = null;
    	constructor = null;
    }
}

/**
 * constructor to initialize values after construction.
 * 
 * @author Tom
 * @version $Revision$
 */
//TODO: use more generic algorithms through annotations instead of fixed method name 'initDeserialization'
class PostConstructor extends Constructor {
	public PostConstructor() {
		ClassConstructor classConstructor = new ClassConstructor();
		classConstructor.setPropertyUtils(getPropertyUtils());
		this.yamlConstructors.put(new Tag(Class.class), classConstructor.new ConstructClass());
		ProxyConstructor proxyConstructor = new ProxyConstructor();
		proxyConstructor.setPropertyUtils(getPropertyUtils());
		this.yamlConstructors.put(new Tag(DelegationHandler.class), proxyConstructor.new ConstructProxy());
	}
    @Override
    public TypeDescription addTypeDescription(TypeDescription definition) {
        TypeDescription typeDescription = super.addTypeDescription(definition);
        Tag tag = new Tag(definition.getType().getSimpleName());
        this.yamlConstructors.put(tag, new PostConstruct());
        return typeDescription;
    }
    
    @Override
    protected Object constructObject(Node node) {
        if (Class.class.isAssignableFrom(node.getType())) {
            try {
                if (node instanceof MappingNode) {
                    List<NodeTuple> values = ((MappingNode) node).getValue();
                    for (NodeTuple tuple : values) {
                        if (tuple.getKeyNode() instanceof ScalarNode) {
                            if (((ScalarNode) tuple.getKeyNode()).getValue().equals("name"))
                                return Thread.currentThread().getContextClassLoader()
                                    .loadClass(((ScalarNode) tuple.getValueNode()).getValue());
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                ManagedException.forward(e);
            }
        }
        return super.constructObject(node);
    }

    class PostConstruct extends ConstructYamlObject {
        @Override
        public Object construct(Node node) {
            if (!Class.class.isAssignableFrom(node.getType()))
                node.setTwoStepsConstruction(true);
            return super.construct(node);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public void construct2ndStep(Node node, Object data) {
            super.construct2ndStep(node, data);
            PrivateAccessor acc = new PrivateAccessor(data);
            if (acc.findMethod("initDeserialization", null).size() > 0)
                acc.call("initDeserialization", null);
        }
    }
}

/**
 * does some pre-initializations (through fixed call to method 'initSerialization()' to pre-arranage values before
 * dumping.
 * 
 * @author Tom
 * @version $Revision$
 */
//TODO: use more generic algorithms through annotations instead of fixed method name 'initSerialization'
class PreRepresenter extends Representer {
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected MappingNode representJavaBean(Set<Property> props, final Object data) {
        PrivateAccessor acc = new PrivateAccessor(data);
        if (data instanceof Class) {
            props.add(new Property("name", Class.class) {
                @Override
                public void set(Object arg0, Object arg1) throws Exception {
                }

                @Override
                public Class<?>[] getActualTypeArguments() {
                    return null;
                }

                @Override
                public Object get(Object arg0) {
                    return ((Class) data).getName();
                }

				@Override
				public List<Annotation> getAnnotations() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
					// TODO Auto-generated method stub
					return null;
				}
            });
        }
        if (acc.findMethod("initSerialization", null).size() > 0)
            acc.call("initSerialization", null);
        return super.representJavaBean(props, data);
    }
}

class SkipEmptyRepresenter extends ProxyRepresenter {
    @Override
    protected NodeTuple representJavaBeanProperty(Object javaBean,
            Property property,
            Object propertyValue,
            Tag customTag) {
        NodeTuple tuple = super.representJavaBeanProperty(javaBean, property, propertyValue,
            customTag);
        Node valueNode = tuple.getValueNode();
        if (Tag.NULL.equals(valueNode.getTag())) {
            return null;// skip 'null' values
        }
        if (valueNode instanceof CollectionNode) {
            if (Tag.SEQ.equals(valueNode.getTag())) {
                SequenceNode seq = (SequenceNode) valueNode;
                if (seq.getValue().isEmpty()) {
                    return null;// skip empty lists
                }
            }
            if (Tag.MAP.equals(valueNode.getTag())) {
                MappingNode seq = (MappingNode) valueNode;
                if (seq.getValue().isEmpty()) {
                    return null;// skip empty maps
                }
            }
        }
        return tuple;
    }
}

class ClassConstructor extends Constructor {
	class ConstructClass extends AbstractConstruct {
	    public Object construct(Node node) {
	        String val = constructScalar((ScalarNode) ((MappingNode) node).getValue().get(0).getValueNode());
	        try {
				return Class.forName(val, true, Thread.currentThread().getContextClassLoader());
			} catch (ClassNotFoundException e) {
				ManagedException.forward(e);
				return null;
			}
	    }
	}
}

class ProxyRepresenter extends PreRepresenter {
	@Override
	public Node represent(Object data) {
			return super.represent(Proxy.isProxyClass(data.getClass()) ? Proxy.getInvocationHandler(data) : data);
	}
}

class ProxyConstructor extends Constructor {
	public ProxyConstructor() {
		ClassConstructor classConstructor = new ClassConstructor();
		classConstructor.setPropertyUtils(getPropertyUtils());
		this.yamlConstructors.put(new Tag(Class.class.getSimpleName()), classConstructor.new ConstructClass());
	}
	class ConstructProxy extends ConstructYamlObject {
	    public Object construct(Node node) {
	    	ProxyConstructor.this.getPropertyUtils().setBeanAccess(BeanAccess.FIELD);
//	    	node.setUseClassConstructor(true);
//	    	node.setTwoStepsConstruction(true);
	        DelegationHandler val = (DelegationHandler) super.construct(node);
//	        construct2ndStep(node, val);
	        workaroundSnakeBug(val);
			return DelegationHandler.createProxy(val);
	    }

		private void workaroundSnakeBug(DelegationHandler dh) {
			//snakeyaml fills all the same interfaces (the first one found)  to the array
			Object[] uniqueInterfaces = new HashSet(Arrays.asList(dh.getInterfaces())).toArray(new Class[0]);
			new PrivateAccessor<DelegationHandler>(dh).set("interfaces", uniqueInterfaces);
		}
	}
}

