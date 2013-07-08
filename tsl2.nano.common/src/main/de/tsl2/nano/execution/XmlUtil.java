/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jun 11, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.execution;

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

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.simpleframework.xml.stream.Format;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.tsl2.nano.Environment;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.util.FileUtil;

/**
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class XmlUtil {
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
            VelocityEngine engine = new VelocityEngine(p);
            engine.addProperty("resource.loader", "file, class, jar");
            engine.addProperty("file.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
            engine.addProperty("class.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            engine.addProperty("jar.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.JarResourceLoader");
            VelocityContext context = new VelocityContext(p);
            Template template = engine.getTemplate(templateFile);
            final File dir = new File(destFile.substring(0, destFile.lastIndexOf("/")));
            dir.mkdirs();
            final BufferedWriter writer = new BufferedWriter(new FileWriter(destFile));

            template.merge(context, writer);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            ForwardedException.forward(e);
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
            ForwardedException.forward(e);
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    public static final <T> T loadXml(String xmlFile, Class<T> type) {
        CompatibilityLayer compLayer = Environment.get(CompatibilityLayer.class);
        //not available on android
        /*if (compLayer.isAvailable("javax.xml.bind.JAXB")) {
            return javax.xml.bind.JAXB.unmarshal(xmlFile, type);
        } else */if (compLayer.isAvailable("org.simpleframework.xml.core.Persister")) {
            try {
                Environment.assignClassloaderToCurrentThread();
                return new org.simpleframework.xml.core.Persister().read(type, new FileInputStream(new File(xmlFile)));
            } catch (Exception e) {
                ForwardedException.forward(e);
                return null;
            }
        } else {
            return (T) FileUtil.loadXml(xmlFile);
        }
    }
    public static final void saveXml(String xmlFile, Object obj) {
        CompatibilityLayer compLayer = Environment.get(CompatibilityLayer.class);
        //not available on android
        /*if (compLayer.isAvailable("javax.xml.bind.JAXB")) {
            javax.xml.bind.JAXB.marshal(obj, xmlFile);
        } else */if (compLayer.isAvailable("org.simpleframework.xml.core.Persister")) {
            try {
                new org.simpleframework.xml.core.Persister(new Format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")).write(obj, new File(xmlFile));
            } catch (Exception e) {
                ForwardedException.forward(e);
            }
        } else {
            FileUtil.saveXml((Serializable)obj, xmlFile);
        }
    }
}
