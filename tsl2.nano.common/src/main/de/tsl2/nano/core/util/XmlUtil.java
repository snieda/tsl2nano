/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jun 11, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.core.util;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;

import org.apache.commons.logging.Log;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
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
import de.tsl2.nano.core.execution.CompatibilityLayer;
import de.tsl2.nano.core.log.LogFactory;

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
     * generates through velocity
     * 
     * @param templateFile
     */
    public static void transformVel(String templateFile) {
        transformVel(templateFile, "generated/" + templateFile, new Properties());
    }

    /**
     * generates through velocity
     * 
     * @param templateFile source file, destination will be generated/ + templateFile
     * @param p properties
     */
    public static void transformVel(String templateFile, Properties p) {
        transformVel(templateFile, "generated/" + templateFile, p);
    }

    /**
     * generates through velocity
     * 
     * @param templateFile source file
     * @param destFile destination file
     * @param p properties
     */
    public static void transformVel(String templateFile, String destFile, Properties p) {
        try {
            /*
             * to avoid a static dependency to velocity, we use the compatibility layer
             */
//            org.apache.velocity.app.VelocityEngine engine = new org.apache.velocity.app.VelocityEngine(p);
//            engine.addProperty("resource.loader", "file, class, jar");
//            engine.addProperty("file.resource.loader.class",
//                "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
//            engine.addProperty("class.resource.loader.class",
//                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
//            engine.addProperty("jar.resource.loader.class",
//                "org.apache.velocity.runtime.resource.loader.JarResourceLoader");
//            org.apache.velocity.VelocityContext context = new org.apache.velocity.VelocityContext(p);
//            org.apache.velocity.Template template = engine.getTemplate(templateFile);
            /*
             * to avoid a static dependency to velocity, we use the compatibility layer
             */
            p.put("resource.loader", "file, class, jar");
            p.put("file.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
            p.put("class.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            p.put("jar.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.JarResourceLoader");

            //this code is untested yet!!!
            CompatibilityLayer layer = ENV.get(CompatibilityLayer.class);
            Object engine =
                layer.runOptional("org.apache.velocity.app.VelocityEngine", "VelocityEngine",
                    new Class[] { Properties.class }, p);
            Object context =
                layer.runOptional("org.apache.velocity.VelocityContext", "VelocityContext",
                    new Class[] { Properties.class }, p);
            Object template = layer.runOptional(engine, "getTemplate", new Class[] { String.class }, templateFile);

            final File dir = new File(destFile.substring(0, destFile.lastIndexOf("/")));
            dir.mkdirs();
            final BufferedWriter writer = new BufferedWriter(new FileWriter(destFile));

            layer.runOptional(template, "merge",
                layer.load("org.apache.velocity.VelocityContext", "java.io.BufferedWriter"), context, writer);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            ManagedException.forward(e);
        }
    }

    /**
     * delegates to {@link #fop(File, String, File)} and writes the result to the file srcFile + mimeType
     */
    static void ffop(File srcFile, String mimeType, File xsltFile) {
        String destFile = srcFile.getPath() + "." + StringUtil.substring(mimeType, "/", null);
        FileUtil.writeBytes(fop(srcFile, mimeType, xsltFile), destFile, false);
    }

    /**
     * Druckt eine xml-Datei über apache fop (version 1.1) in ein angegebenes mime-ausgabe format - entweder in eine
     * Datei oder direkt an den Drucker.
     * <p/>
     * Alle verwendbaren Ausgabe-Formate sind hier einzusehen: {@link org.apache.xmlgraphics.util.MimeConstants}.
     * 
     * <pre>
     * application/pdf
     * application/postscript
     * application/postscript
     * application/x-pcl
     * application/vnd.hp-PCL
     * application/x-afp
     * application/vnd.ibm.modcap
     * image/x-afp+fs10
     * image/x-afp+fs11
     * image/x-afp+fs45
     * image/x-afp+goca
     * text/plain
     * application/rtf
     * text/richtext
     * text/rtf
     * application/mif
     * image/svg+xml
     * image/gif
     * image/png
     * image/jpeg
     * image/tiff
     * text/xsl
     * image/x-emf
     * </pre>
     * 
     * @author Thomas Schneider
     */
    static byte[] fop(File srcFile, String mimeType, File xsltFile) {
        try {
            StreamSource source = new StreamSource(srcFile);
            // creation of transform source
            StreamSource transformSource = new StreamSource(xsltFile);
            // create an instance of fop factory
            FopFactory fopFactory = FopFactory.newInstance();
            // a user agent is needed for transformation
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
            // to store output
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();

            Transformer xslfoTransformer = TransformerFactory.newInstance().newTransformer(transformSource);
            // Construct fop with desired output format
            Fop fop = fopFactory.newFop(mimeType, foUserAgent, outStream);

            // ignore xsd informations in fo:root
            fopFactory
                .ignoreNamespace("http://www.w3.org/2001/XMLSchema-instance");

            // Resulting SAX events (the generated FO)
            // must be piped through to FOP
            Result res = new SAXResult(fop.getDefaultHandler());

            // Start XSLT transformation and FOP processing
            // everything will happen here..
            LOG.info("STARTE TRANSFORMATION...");
            xslfoTransformer.transform(source, res);
            LOG.info("TRANSFORMATION BEENDET...");
            return outStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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
     * delegates to {@link #jasperReport(String, boolean, boolean, Map, Connection)}
     */
    public void jasperReport(String srcName, String outputFilename) throws Exception {
        LOG.info("creating jasper report " + srcName);
        byte[] stream = jasperReport(srcName, false, false, null, null);
        FileUtil.write(new ByteArrayInputStream(stream), outputFilename);
        LOG.info("jasper report " + srcName + " written to " + outputFilename);
    }

    /**
     * jasperReport. for further informations, see http://www.tutorialspoint.com/jasper_reports/jasper_quick_guide.htm.
     * 
     * @param srcName xml/jrxml or jasper file to be read. if it is an xml/jrxml, the flag compile should be true.
     * @param compile
     * @param xml if true, report will be written to xml - otherwise to pdf
     * @param parameter (optional) extended parameters for jasper
     * @param connection (optional) database connection if needed
     * @return exported bytes to be saved or printed
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public byte[] jasperReport(String srcName, boolean compile, boolean xml, Map parameter, Connection connection) {
        InputStream inputStream = null;
        try {
            if (parameter == null) {
                parameter = new HashMap();
            }

            inputStream = FileUtil.getFile(srcName);

            if (compile) {
                LOG.debug("Compiling report..");
                ByteArrayOutputStream templateOutputStream = new ByteArrayOutputStream();
                JasperCompileManager.compileReportToStream(inputStream, templateOutputStream);
                inputStream = new ByteArrayInputStream(templateOutputStream.toByteArray());
            }
            JasperPrint print = JasperFillManager.fillReport(inputStream, parameter, connection);
            ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();

            LOG.debug("Exporting..");
            if (xml) {
                JasperExportManager.exportReportToXmlStream(print, arrayOutputStream);
            } else {
                JasperExportManager.exportReportToPdfStream(print, arrayOutputStream);
            }
            return arrayOutputStream.toByteArray();
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    ManagedException.forward(e);
                }
            }
        }
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
            String fileName, Class<RESULTTYPE> resultType) {
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
            InputStream stream, Class<RESULTTYPE> resultType) {
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

    public static final <T> T loadXml(String xmlFile, Class<T> type) {
        return loadXml(xmlFile, type, ENV.get(CompatibilityLayer.class), true);
    }

    @SuppressWarnings("unchecked")
    public static final <T> T loadXml(String xmlFile,
            Class<T> type,
            CompatibilityLayer compLayer,
            boolean assignClassloader) {
        //not available on android
        /*if (compLayer.isAvailable("javax.xml.bind.JAXB")) {
            return javax.xml.bind.JAXB.unmarshal(xmlFile, type);
        } else */if (compLayer.isAvailable("org.simpleframework.xml.core.Persister")) {
            if (assignClassloader) {
                ENV.assignClassloaderToCurrentThread();
            }
            LOG.debug("loading type '" + type.getName() + "' from '" + xmlFile + "'");
            return loadSimpleXml_(xmlFile, type);
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
    public static <T> T loadSimpleXml_(String xmlFile, Class<T> type) {
        FileInputStream fileInputStream = null;
        try {
            return new org.simpleframework.xml.core.Persister(getSimpleXmlProxyStrategy(), new SimpleXmlArrayWorkaround()).read(type,
                fileInputStream = new FileInputStream(new File(xmlFile)));
        } catch (Exception e) {
            //mark the loaded xml file as corrupt
            File file = new File(xmlFile);
            if (file.canWrite()) {
                fileInputStream = FileUtil.close(fileInputStream, false);
                LOG.info("renaming corrupted file '" + xmlFile + "' to: " + xmlFile + ".failed");
                if (!file.renameTo(new File(file.getPath() + ".failed")))
                    LOG.warn("couldn't rename corrupted file '" + xmlFile + "' to '" + xmlFile
                        + ".failed' !");
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
            new org.simpleframework.xml.core.Persister(getSimpleXmlProxyStrategy(), new Format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")).write(
                obj, new File(xmlFile));
            //workaround for empty files
            if (FileUtil.getFile(xmlFile).available() == 0) {
                new File(xmlFile).delete();
            }
        } catch (Exception e) {
            //as simple-xml doesn't delete corrupt created files, we move it to temp
            File file = new File(xmlFile);
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

    public static Strategy getSimpleXmlProxyStrategy() {
//        return new TreeStrategy() {
//            @Override
//            public Value read(Type type, NodeMap node, Map map) throws Exception {
//                // TODO Auto-generated method stub
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
        final Converter converter = new Converter() {
            @Override
            public Object read(InputNode n) throws Exception {
                //WORKAROUND to evaluate the class name. where can we extract the class name from?
                final Class cls = BeanClass.load("de.tsl2.nano.h5.RuleCover");
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
        Registry reg = new Registry() {
            public Converter lookup(Class type) throws Exception {
                return Proxy.isProxyClass(type) || InvocationHandler.class.isAssignableFrom(type) ? converter : null;
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
    @Override
    public Transform<?> match(@SuppressWarnings("rawtypes") final Class type) throws Exception {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (type.equals(Class.class)) {
            return new Transform() {
                @Override
                public Object read(String clsName) throws Exception {
                    //loading the class through the ClassLoder.loadClass(name) may fail on object arrays, so we load it through Class.forName(name)
                    return clsName.contains(".") || clsName.startsWith("[") ? loader.loadClass(clsName) : PrimitiveUtil
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

/**
 * workaround on simple-xml problem for field type byte[].class.
 * 
 * @author Tom
 * @version $Revision$
 */
class SimpleXmlProxyWorkaround implements Matcher {
    @Override
    public Transform<?> match(@SuppressWarnings("rawtypes") final Class type) throws Exception {
        if (Proxy.isProxyClass(type)) {
            return new Transform() {
                Persister persister = new Persister();

                @Override
                public Object read(String object) throws Exception {
                    return persister.read(object, (String) null);
                }

                @Override
                public String write(Object arg0) throws Exception {
                    StringWriter writer = new StringWriter();
                    persister.write(Proxy.getInvocationHandler(arg0), writer);
                    return writer.getBuffer().toString();
                }
            };
        }
        return null;
    }

}
