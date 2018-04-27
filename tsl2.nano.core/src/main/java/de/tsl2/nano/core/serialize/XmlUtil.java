/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jun 11, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.core.serialize;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.events.StartElement;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.convert.Registry;
import org.simpleframework.xml.convert.RegistryStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.strategy.Strategy;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;
import org.simpleframework.xml.transform.Matcher;
import org.simpleframework.xml.transform.Transform;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.cls.Reflection;
import de.tsl2.nano.core.execution.CompatibilityLayer;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.AnnotationProxy;
import de.tsl2.nano.core.util.DelegationHandler;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.StringUtil;

/**
 * provides convenience methods for:
 * 
 * <pre>
 * - de-/serializing xml through jaxb, simple-xml and xml-api
 * - xpath
 * - velocity transformations (to generate source code etc.)
 * - apache fop transformations (to create printable formats like pcl, pdf, rtf, jpg, etc.)
 * - xsl-tranformations
 * </pre>
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class XmlUtil {
    private static final Log LOG = LogFactory.getLog(XmlUtil.class);

    /**
     * does an xsl transformation
     * 
     * @param srcFile source to be transformed
     * @param xsl transformation definition
     * @param outputFile destination file
     * @throws TransformerConfigurationException
     * @throws TransformerException
     */
    public static void transformXsl(String srcFile, String xsl, String outputFile)
            throws TransformerConfigurationException,
            TransformerException {
        LOG.info("creating xsl transformation for " + srcFile + " with " + xsl);
        TransformerFactory factory = TransformerFactory.newInstance();
        StreamSource xslStream = new StreamSource(new File(xsl));
        Transformer transformer = factory.newTransformer(xslStream);
        StreamSource in = new StreamSource(new File(srcFile));
        StreamResult out = new StreamResult(new File(outputFile));
        transformer.transform(in, out);
        System.out.println("xsl transformation result: " + outputFile);
    }

    /**
     * delegates to {@link #xpath(String, InputStream, Class)}
     */
    public static String xpath(String expression,
            String fileName) {
        try {
            return xpath(expression, new FileInputStream(new File(fileName)), String.class);
        } catch (FileNotFoundException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * delegates to {@link #xpath(String, InputStream, Class)}
     */
    public static <RESULTTYPE> RESULTTYPE xpath(String expression,
            String fileName,
            Class<RESULTTYPE> resultType) {
        try {
            return xpath(expression, new FileInputStream(new File(fileName)), resultType);
        } catch (FileNotFoundException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * evaluates an xpath expression and returns its result
     * 
     * @param expression xpath expression
     * @param stream input stream (mostly a FileInputStream)
     * @param resultType result type to be returned. One of: {@link Map}, {@link Node}, {@link Number}, {@link Boolean},
     *            {@link String}.
     * @return result depending on the result type. if Map was specified, all result nodes and their child-nodes will be
     *         put to the result map.
     */
    @SuppressWarnings({ "unchecked" })
    private static <RESULTTYPE> RESULTTYPE xpath(String expression,
            InputStream stream,
            Class<RESULTTYPE> resultType) {
        DocumentBuilder builder;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document xmlDocument = builder.parse(stream);

            XPath xPath = XPathFactory.newInstance().newXPath();
            // read a nodelist using xpath
            if (resultType == null || Map.class.isAssignableFrom(resultType)) {
                NodeList nodes = (NodeList) xPath.compile(expression).evaluate(
                    xmlDocument, XPathConstants.NODESET);
                Map<String, Object> result = new LinkedHashMap<String, Object>();
                for (int i = 0; i < nodes.getLength(); i++) {
                    Node n = nodes.item(i);
                    add(n, result);
                }
                return (RESULTTYPE) result;
            } else if (Node.class.isAssignableFrom(resultType)) {
                return (RESULTTYPE) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODE);
            } else if (Boolean.class.isAssignableFrom(resultType)) {
                return (RESULTTYPE) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.BOOLEAN);
            } else if (Number.class.isAssignableFrom(resultType)) {
                return (RESULTTYPE) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NUMBER);
            } else if (String.class.isAssignableFrom(resultType)) {
                return (RESULTTYPE) xPath.compile(expression).evaluate(xmlDocument);
            } else {
                throw new IllegalArgumentException(
                    "resulttype must be one of: Map, org.w3c.dom.Node, Boolean, Number, String or null!");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * recursive nodes and child-nodes will be put to the given result map
     * 
     * @param n node to put its properties to the reuslt map
     * @param result
     */
    private static void add(Node n, Map<String, Object> result) {
        NodeList children = n.getChildNodes();
        if (children.getLength() > 0) {
            for (int i = 0; i < children.getLength(); i++) {
                add(children.item(i), result);
            }
        } else {
            Object v = n.getNodeValue();
            result.put(n.getParentNode().getNodeName(), v != null ? v : n.getTextContent());
        }
    }

    public static String replaceTagsAndNamespace(String xml, Class... classes) {
        //first: the namespace
        xml = xml.replaceAll("(</?)\\w+[:]", "$1");
        for (int i = 0; i < classes.length; i++) {
            //entferne tag der geg. Klasse
            xml = xml.replaceAll("</?" + StringUtil.toFirstLower(classes[i].getSimpleName()) + "[^>]*>", "");
        }
        return xml;
    }

    public static final <T> T loadXml(String xmlFile, Class<T> type) {
        return loadXml(xmlFile, type, ENV.get(CompatibilityLayer.class), true, true);
    }

    public static final <T> T loadXml(String xmlFile, Class<T> type, boolean renameOnError) {
        return loadXml(xmlFile, type, ENV.get(CompatibilityLayer.class), true, renameOnError);
    }

    @SuppressWarnings("unchecked")
    public static final <T> T loadXml(String xmlFile,
            Class<T> type,
            CompatibilityLayer compLayer,
            boolean assignClassloader,
            boolean renameOnError
            ) {
        //not available on android
        /*if (compLayer.isAvailable("javax.xml.bind.JAXB")) {
            return javax.xml.bind.JAXB.unmarshal(xmlFile, type);
        } else */if (compLayer.isAvailable("org.simpleframework.xml.core.Persister")) {
            if (assignClassloader) {
                ENV.assignClassloaderToCurrentThread();
            }
            LOG.debug("loading type '" + type.getName() + "' from '" + xmlFile + "'");
            return loadSimpleXml_(xmlFile, type, renameOnError);
        } else {
            return (T) FileUtil.loadXml(xmlFile);
        }
    }

    /**
     * only for internal use
     * 
     * @param xmlFile
     * @param type
     * @return
     */
    public static <T> T loadSimpleXml_(String xmlFile, Class<T> type, boolean renameOnError) {
        FileInputStream fileInputStream = null;
        try {
            return new org.simpleframework.xml.core.Persister(getSimpleXmlProxyStrategy(),
                new SimpleXmlArrayWorkaround()).read(type,
                    fileInputStream = new FileInputStream(new File(xmlFile)));
        } catch (Exception e) {
            //mark the loaded xml file as corrupt
            if (renameOnError && (ENV.isAvailable() && !ENV.get("app.mode.strict", false))) {
                File file = new File(xmlFile);
                if (file.canWrite()) {
                    fileInputStream = FileUtil.close(fileInputStream, false);
                    LOG.info("renaming corrupted file '" + xmlFile + "' to: " + xmlFile + ".failed");
                    if (!file.renameTo(new File(file.getPath() + ".failed")))
                        LOG.warn("couldn't rename corrupted file '" + xmlFile + "' to '" + xmlFile
                            + ".failed' !");
                }
            }
            //write the error to an equal-named file
            String stackTraceFile = xmlFile + ".stacktrace";
            if (!new File(stackTraceFile).exists()) {
                PrintStream printStream = null;
                try {
                    printStream = new PrintStream(stackTraceFile);
                    e.printStackTrace(printStream);
                } catch (FileNotFoundException e1) {
                    LOG.error("cant' write stacktrace to " + stackTraceFile);
                } finally {
                    if (printStream != null)
                        printStream.close();
                }
            }
            //don't use the ManagedException.forward(), because the LogFactory is using this, too!
            throw new RuntimeException(e);
        } finally {
            FileUtil.close(fileInputStream, false);
        }
    }

    public static final void saveXml(String xmlFile, Object obj) {
        CompatibilityLayer compLayer = ENV.get(CompatibilityLayer.class);
        //not available on android
        /*if (compLayer.isAvailable("javax.xml.bind.JAXB")) {
            javax.xml.bind.JAXB.marshal(obj, xmlFile);
        } else */if (compLayer.isAvailable("org.simpleframework.xml.core.Persister")) {
            LOG.debug("saving file '" + xmlFile + "' with object '" + obj + "'");
            saveSimpleXml_(xmlFile, obj);
        } else {
            FileUtil.saveXml((Serializable) obj, xmlFile);
        }
    }

    /**
     * only for internal use
     * 
     * @param xmlFile
     * @param obj
     */
    public static void saveSimpleXml_(String xmlFile, Object obj) {
        try {
            new org.simpleframework.xml.core.Persister(getSimpleXmlProxyStrategy(),
                new Format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")).write(
                    obj, new File(xmlFile).getAbsoluteFile());
            //workaround for empty files
            if (FileUtil.getFile(xmlFile).available() == 0) {
                new File(xmlFile).getAbsoluteFile().delete();
            }
        } catch (Exception e) {
            //as simple-xml doesn't delete corrupt created files, we move it to temp
            File file = new File(xmlFile).getAbsoluteFile();
            if (file.exists()) {
                File temp = new File(xmlFile + ".failed");
                if (!temp.exists() || temp.delete()) {
                    file.renameTo(temp);
                }
            }
            //don't use the ManagedException.forward(), because the LogFactory is using this, too!
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Strategy getSimpleXmlProxyStrategy() {
//        return new TreeStrategy() {
//            @Override
//            public Value read(Type type, NodeMap node, Map map) throws Exception {
//                return super.read(type, node, map);
//            }
//            @Override
//            public boolean write(Type type, Object value, NodeMap node, Map map) {
//                if (Proxy.isProxyClass(value.getClass()))
//                    value = Proxy.getInvocationHandler(value);
//                return super.write(type, value, node, map);
//            }
//        };

        final Persister persister = new Persister();
        //this converter delegates from a proxy to its invocationhandler
        final Converter proxyConverter = new Converter() {
            @Override
            public Object read(InputNode n) throws Exception {
                //WORKAROUND to evaluate the class name. where can we extract the class name from?
            	String clsName = ((StartElement)n.getSource()).getAttributeByName(new QName("class")).getValue();
                final Class cls = BeanClass.load( clsName != null ? clsName : "de.tsl2.nano.h5.RuleCover");
                Element element = AnnotationProxy.getAnnotation(SimpleXmlAnnotator.class, "attribute", Element.class);
                AnnotationProxy.setAnnotationValues(element, "name", "ruleCover", "type", cls);
                Object ih = persister.read(cls, n.getNext(), false);
                return DelegationHandler.createProxy((DelegationHandler) ih);
            }

            @Override
            public void write(OutputNode n, Object o) throws Exception {
                InvocationHandler handler = Proxy.getInvocationHandler(o);
                n.setAttribute("class", handler.getClass().getName());
                persister.write(handler, n);
            }
        };
        //jdk classes that are not persistable by simpelxml will use Reflection
        final Map<Class, String[]> unpersistableClasses =
            MapUtil.asMap(SimpleDateFormat.class, new String[] { "pattern" });
        final Converter reflectConverter = new Converter() {
            @Override
            public Object read(InputNode n) throws Exception {
                Reflection ref = persister.read(Reflection.class, n.getNext(), false);
                return ref.object();
            }

            @Override
            public void write(OutputNode n, Object o) throws Exception {
                persister.write(Reflection.reflectFields(o, unpersistableClasses.get(o.getClass())), n);
            }
        };
        Registry reg = new Registry() {
            public Converter lookup(Class type) throws Exception {
                if (Proxy.isProxyClass(type) || InvocationHandler.class.isAssignableFrom(type)) {
                    return proxyConverter;
                } else if (unpersistableClasses.keySet().contains(type))
                    return reflectConverter;
                else
                    return null;
            }
        };
        return new RegistryStrategy(reg);
    }
}

/**
 * workaround on simple-xml problem for field type byte[].class.
 * 
 * @author Tom
 * @version $Revision$
 */
class SimpleXmlArrayWorkaround implements Matcher {
    @SuppressWarnings("rawtypes")
    @Override
    public Transform<?> match(final Class type) throws Exception {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (type.equals(Class.class)) {
            return new Transform() {
                @Override
                public Object read(String clsName) throws Exception {
                    //loading the class through the ClassLoder.loadClass(name) may fail on object arrays, so we load it through Class.forName(name)
                    return clsName.contains(".") || clsName.startsWith("[") ? loader.getClass().forName(clsName)
                        : PrimitiveUtil
                            .getPrimitiveClass(clsName);
                }

                @Override
                public String write(Object arg0) throws Exception {
                    return arg0.toString();
                }
            };
        }
        return null;
    }

}
