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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.stream.Format;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.tsl2.nano.core.Environment;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.execution.CompatibilityLayer;
import de.tsl2.nano.core.log.LogFactory;

/**
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class XmlUtil {
    private static final Log LOG = LogFactory.getLog(XmlUtil.class);
    /**
     * transform
     * 
     * @param templateFile
     */
    public static void transform(String templateFile) {
        transform(templateFile, "generated/" + templateFile, new Properties());
    }

    /**
     * generates through velocity
     * 
     * @param templateFile source file, destination will be generated/ + templateFile
     * @param p properties
     */
    public static void transform(String templateFile, Properties p) {
        transform(templateFile, "generated/" + templateFile, p);
    }

    /**
     * generates through velocity
     * 
     * @param templateFile source file
     * @param destFile destination file
     * @param p properties
     */
    public static void transform(String templateFile, String destFile, Properties p) {
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
            CompatibilityLayer layer = Environment.get(CompatibilityLayer.class);
            Object engine = layer.runOptional("org.apache.velocity.app.VelocityEngine", "VelocityEngine", new Class[]{Properties.class}, p);
            Object context = layer.runOptional("org.apache.velocity.VelocityContext", "VelocityContext", new Class[]{Properties.class}, p);
            Object template = layer.runOptional(engine, "getTemplate", new Class[]{String.class}, templateFile);
            
            final File dir = new File(destFile.substring(0, destFile.lastIndexOf("/")));
            dir.mkdirs();
            final BufferedWriter writer = new BufferedWriter(new FileWriter(destFile));

            layer.runOptional(template, "merge", layer.load("org.apache.velocity.VelocityContext", "java.io.BufferedWriter"), context, writer);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            ManagedException.forward(e);
        }
    }

    /**
     * xpath
     * 
     * @param xmlFile
     * @param xpath
     * @return
     */
    public static String[] xpath(String xmlFile, String xpath) {
//        SAXParserFactory.newInstance();
        XPath xPath = XPathFactory.newInstance().newXPath();
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        try {
            domFactory.setNamespaceAware(true);
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);

            XPathExpression expr = xPath.compile(xpath);
            //on standard expressions, a node-set will be returned
            try {
                NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                String[] res = new String[nodes.getLength()];
                Node n;
                for (int i = 0; i < nodes.getLength(); i++) {
                    n = nodes.item(i);
                    res[i] = (n.getNodeValue() != null ? n.getNodeValue() : n.getTextContent());
                    System.out.println(res[i]);
                }
                return res;
            } catch(Exception  ex) {
                //on e.g. count expressions, no node-set but a string will be returned
                String result = (String) expr.evaluate(doc, XPathConstants.STRING);
                System.out.println(result);
                return new String[]{String.valueOf(result)};
            }
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }
    
    public static final <T> T loadXml(String xmlFile, Class<T> type) {
        return loadXml(xmlFile, type, Environment.get(CompatibilityLayer.class), true);
    }
    @SuppressWarnings("unchecked")
    public static final <T> T loadXml(String xmlFile, Class<T> type, CompatibilityLayer compLayer, boolean assignClassloader) {
        //not available on android
        /*if (compLayer.isAvailable("javax.xml.bind.JAXB")) {
            return javax.xml.bind.JAXB.unmarshal(xmlFile, type);
        } else */if (compLayer.isAvailable("org.simpleframework.xml.core.Persister")) {
            if (assignClassloader)
                Environment.assignClassloaderToCurrentThread();
            LOG.debug("loading type '" + type.getName() + "' from '" + xmlFile + "'");
            return loadSimpleXml_(xmlFile, type);
        } else {
            return (T) FileUtil.loadXml(xmlFile);
        }
    }

    /**
     * only for internal use
     * @param xmlFile
     * @param type
     * @return
     */
    public static <T> T loadSimpleXml_(String xmlFile, Class<T> type) {
        try {
            return new org.simpleframework.xml.core.Persister().read(type, new FileInputStream(new File(xmlFile)));
        } catch (Exception e) {
            //don't use the ManagedException.forward(), because the LogFactory is using this, too!
            throw new RuntimeException(e);
        }
    }
    
    public static final void saveXml(String xmlFile, Object obj) {
        CompatibilityLayer compLayer = Environment.get(CompatibilityLayer.class);
        //not available on android
        /*if (compLayer.isAvailable("javax.xml.bind.JAXB")) {
            javax.xml.bind.JAXB.marshal(obj, xmlFile);
        } else */if (compLayer.isAvailable("org.simpleframework.xml.core.Persister")) {
            LOG.debug("saving file '" + xmlFile + "' with object '" + obj + "'");
            saveSimpleXml_(xmlFile, obj);
        } else {
            FileUtil.saveXml((Serializable)obj, xmlFile);
        }
    }

    /**
     * only for internal use
     * @param xmlFile
     * @param obj
     */
    public static void saveSimpleXml_(String xmlFile, Object obj) {
        try {
            new org.simpleframework.xml.core.Persister(new Format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")).write(obj, new File(xmlFile));
            //workaround for empty files
            if (FileUtil.getFile(xmlFile).available() == 0)
                new File(xmlFile).delete();
        } catch (Exception e) {
            //as simple-xml doesn't delete corrupt created files, we move it to temp
            File file = new File(xmlFile);
            if (file.exists()) {
                File temp = new File(xmlFile + ".failed");
                if (!temp.exists() || temp.delete())
                    file.renameTo(temp);
            }
            //don't use the ManagedException.forward(), because the LogFactory is using this, too!
            throw new RuntimeException(e);
        }
    }
}
