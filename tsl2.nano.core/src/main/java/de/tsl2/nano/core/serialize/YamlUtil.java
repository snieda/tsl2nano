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

import org.apache.commons.logging.Log;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
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

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.ClassFinder;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.cls.PrivateAccessor;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.DelegationHandler;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.ObjectUtil;
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
	private static final Log LOG = LogFactory.getLog(YamlUtil.class);
	
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
    	DumperOptions doptions = new DumperOptions();
    	if (ENV.get("app.configuration.persist.yaml.flowstyle", false)) {
			doptions.setDefaultFlowStyle(FlowStyle.FLOW);
    	} else {
    		doptions.setDefaultFlowStyle(FlowStyle.AUTO);
    	}
        Yaml yaml = new Yaml(getRepresenter(skipEmpties, shortCutPackage), doptions);
        if (fields)
        	yaml.setBeanAccess(BeanAccess.FIELD);
        return yaml.dump(obj);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static Representer getRepresenter(boolean skipEmpties, String shortCutPackage) {
    	Representer representer = skipEmpties ? new SkipEmptyRepresenter() : new PreRepresenter();
        Map<Double, Class> classes = ClassFinder.self().fuzzyFind(shortCutPackage);
        int c = 0;
        for (Class cls : classes.values()) {
            if (isContructable(cls)) {
                representer.addTypeDescription(new TypeDescription(cls, cls.getSimpleName()));
                c++;
            }
        }
        representer.addClassTag(Class.class, new Tag(Class.class.getSimpleName()));
        representer.addClassTag(Long.class, new Tag(Long.class));
        LOG.info(representer + " created! added " + c + " classtags for '" + shortCutPackage + "'");
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
        	LOG.warn(e.toString());
            ManagedException.forward(e, false);
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
        Constructor constructor = new PostConstructor();
        Map<Double, Class> classes = ClassFinder.self().fuzzyFind(shortCutPackage);
        TypeDescription typeDef;
        int c = 0;
        for (Class cls : classes.values()) {
            if (isContructable(cls)) {
                typeDef = new TypeDescription(cls, new Tag(cls.getSimpleName()), cls);
                constructor.addTypeDescription(typeDef);
                c++;
            }
        }
        LOG.info(constructor + " created! added " + c + " typedefinitions for '" + shortCutPackage + "'");
        return constructor;
    }

	private static boolean isContructable(Class cls) {
		return cls.getSimpleName().length() > 0  && !cls.getSimpleName().endsWith("Util") 
				&& BeanClass.hasDefaultConstructor(cls) && !cls.isAnnotation();
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
		this.yamlConstructors.put(null, new PostConstruct());
		ClassConstructor classConstructor = new ClassConstructor();
		classConstructor.setPropertyUtils(getPropertyUtils());
		this.yamlConstructors.put(new Tag(Class.class.getSimpleName()), classConstructor.new ConstructClass());
		ProxyConstructor proxyConstructor = new ProxyConstructor();
		proxyConstructor.setPropertyUtils(getPropertyUtils());
		this.yamlConstructors.put(new Tag(DelegationHandler.class), proxyConstructor.new ConstructProxy());
	}
    @Override
    public TypeDescription addTypeDescription(TypeDescription definition) {
        TypeDescription typeDescription = super.addTypeDescription(definition);
        Tag tag = new Tag(definition.getType().getSimpleName());
        this.yamlConstructors.put(tag, new PostConstruct());
        this.yamlConstructors.put(new Tag(definition.getType()), new PostConstruct());
        return typeDescription;
    }
    
    @Override
    protected Object constructObject(Node node) {
        if (Class.class.isAssignableFrom(node.getType())) {
            if (node instanceof MappingNode) {
                List<NodeTuple> values = ((MappingNode) node).getValue();
                for (NodeTuple tuple : values) {
                    if (tuple.getKeyNode() instanceof ScalarNode) {
                        if (((ScalarNode) tuple.getKeyNode()).getValue().equals("name")) {
                        	//loading the class through the ClassLoder.loadClass(name) may fail on object arrays, so we load it through Class.forName(name)                            	
                        	String clsName = ((ScalarNode) tuple.getValueNode()).getValue();
							return ObjectUtil.loadClass(clsName);
                        }
                    }
                }
            }
        }
        return super.constructObject(node);
    }
    class PostConstruct extends ConstructYamlObject {
        @Override
        public Object construct(Node node) {
            Object result = super.construct(node);
            if (node instanceof MappingNode && !PrimitiveUtil.isPrimitiveOrWrapper(node.getType()))
                node.setTwoStepsConstruction(true);
            return result;
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
	public boolean hasDeserializationMethod(Node node) {
		return Arrays.stream(node.getType().getDeclaredMethods()).anyMatch(m -> m.getName().equals("initDeserialization"));
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
    protected MappingNode representJavaBean(Set<Property> props, Object data) {
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
                    return arg0 instanceof Class ? ((Class) arg0).getName() : arg0.getClass().getName();
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
			return ObjectUtil.loadClass(val);
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

