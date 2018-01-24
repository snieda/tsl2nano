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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
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
import de.tsl2.nano.core.cls.ClassFinder;
import de.tsl2.nano.core.cls.PrivateAccessor;
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
    private static Yaml yamlWriter;
    private static Yaml yamlLoader;

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
        if (yamlWriter == null)
            yamlWriter = createYamlWriter(true, true, Util.FRAMEWORK_PACKAGE);
        return yamlWriter.dump(obj);
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
        return createYamlWriter(skipEmpties, fields, shortCutPackage).dump(obj);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static Yaml createYamlWriter(boolean skipEmpties, boolean fields, String shortCutPackage) {
        Representer representer = skipEmpties ? new SkipEmptyRepresenter() : new Representer();
        Map<Double, Class> classes = new ClassFinder().fuzzyFind(shortCutPackage);
        for (Class cls : classes.values()) {
            if (cls.getSimpleName().length() > 0)
                representer.addClassTag(cls, new Tag(cls.getSimpleName()));
        }
//        DumperOptions doptions = new DumperOptions();
//        doptions.setDefaultFlowStyle(FlowStyle.AUTO);
        Yaml yaml = new Yaml(representer);
        if (fields)
            yaml.setBeanAccess(BeanAccess.FIELD);
        return yaml;
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
        if (yamlLoader == null)
            yamlLoader = createYamlLoader(true, Util.FRAMEWORK_PACKAGE);
        return yamlLoader.loadAs(stream, type);
    }

    public static <T> T load(String txt, Class<T> type) {
        if (yamlLoader == null)
            yamlLoader = createYamlLoader(true, Util.FRAMEWORK_PACKAGE);
        return yamlLoader.loadAs(txt, type);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static Yaml createYamlLoader(boolean fields, String shortCutPackage) {
        Constructor constructor = new PostConstructor();
        Map<Double, Class> classes = new ClassFinder().fuzzyFind(shortCutPackage);
        TypeDescription typeDef;
        for (Class cls : classes.values()) {
            if (cls.getSimpleName().length() > 0) {
                typeDef = new TypeDescription(cls, cls.getSimpleName());
                constructor.addTypeDescription(typeDef);
            }
        }
        Yaml yaml = new Yaml(constructor);
        if (fields)
            yaml.setBeanAccess(BeanAccess.FIELD);
        return yaml;
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
            });
        }
        if (acc.findMethod("initSerialization", null).size() > 0)
            acc.call("initSerialization", null);
        return super.representJavaBean(props, data);
    }
}

class SkipEmptyRepresenter extends PreRepresenter {
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
