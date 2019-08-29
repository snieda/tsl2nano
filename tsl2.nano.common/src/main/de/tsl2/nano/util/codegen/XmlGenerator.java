/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: 07.12.2008
 * 
 * Copyright: (c) Thomas Schneider 2008, all rights reserved
 */
package de.tsl2.nano.util.codegen;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.classloader.RuntimeClassloader;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.ClassFinder;
import de.tsl2.nano.core.serialize.XmlUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * <pre>
 * This class is able to load model classes from a given path or a given jar-file.
 * 
 * If you use a jar-file, be sure to set the classpath to find all needed classes!
 * 
 * how-to-start: start the main of this class with parameter:
 *   1. package path to find model classes to generate presenters for
 *   2. velocity template file
 *   3. class name of generator specialization (default: PackageGenerator)
 *   4. property-file name (default: null, system-properties will be used anyway)
 *   
 * example:
 *  PackageGenerator bin/org/anonymous/project/ de.tsl2.nano.codegen.PackageGenerator
 *  PackageGenerator lib/mymodel.jar de.tsl2.nano.codegen.PresenterGenerator
 *  
 * To constrain the generation to a specific package path, set the environment variable "bean.generation.packagename".
 * </pre>
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class XmlGenerator extends ACodeGenerator {
    Properties properties = null;
    private String xmlFile;
    private String xmlXPath;

    /**
     * main
     * 
     * @param args will be ignored
     * @throws Exception on any error
     */
    public static void main(String args[]) throws Exception {
        if (args.length == 0 || args.length > 4) {
            String help =
                "syntax : XmlGenerator <xml-file> <xpath-nodelist> [code-template] [[generator-class [property-file]]\n"
                    + "example: XmlGenerator data.xml codegen/openapi.vm de.tsl2.nano.codegen.XmlGenerator\n"
                    + "\nreading system variables:\n"
                    + " - bean.generation.outputpath      : output base path (default: src/gen)\n"
                    + " - bean.generation.nameprefix      : class+package name prefix (default: package + code-template)\n"
                    + " - bean.generation.namepostfix     : class name postfix (default: {code-template}.java)\n"
                    + " - bean.generation.unpackaged      : no package structure from origin will be inherited (default: false)\n"
                    + " - bean.generation.singleFile      : generate only the first occurrency (default: false)\n";
            System.out.println(help);
            System.exit(1);
        }

        XmlGenerator gen;
        if (args.length > 2) {
            gen = (XmlGenerator) ClassGenerator.instance(args[2]);
            if (args.length > 3 && args[3] != null && args[3].length() > 0) {
                gen.getProperties()
                    .putAll(FileUtil.loadProperties(args[3], Thread.currentThread().getContextClassLoader()));
            }
        } else {
            gen = (XmlGenerator) ClassGenerator.instance(new XmlGenerator());
        }
        gen.xmlFile = args[0];
        gen.codeTemplate = args.length > 1 ? args[1] : "codegen/openapi.vm";
        gen.generate();
    }

    /**
     * starts the generate process
     */
    @SuppressWarnings("rawtypes")
    public void generate() {
        NodeList nodeList =  XmlUtil.xpath(xmlXPath, xmlFile, NodeList.class);
        prepareProperties(nodeList);
        boolean singleFile = Boolean.getBoolean("bean.generation.singleFile");
        Node n;
        for (int i = 0; i < nodeList.getLength(); i++) {
            n = nodeList.item(i);
            try {
                generate(n);
                if (singleFile)
                    break;
            } catch (final Exception e) {
                ManagedException.forward(e);
            }
        }
    }

    private void prepareProperties(NodeList nodeList) {
        getProperties().put("allNodes", nodeList);
    }

    /**
     * starts the generator for the given model class
     * 
     * @param node type to generate
     * @throws Exception on any error
     */
    protected void generate(Node node) throws Exception {
        final String destFile = getDefaultDestinationFile(node.getNodeName());
        final Properties p = getProperties();
        super.generate(node, node.getNodeName(), getTemplate(node), destFile, p);
    }

    /**
     * @return properties for the velocity context
     */
    public Properties getProperties() {
        if (properties == null) {
            properties = new Properties();
        }
        return properties;
    }

    /**
     * override this method, to define the path to your own template.
     * 
     * @param type
     * 
     * @return file name of presenter constant class
     */
    protected String getTemplate(Node node) {
        return codeTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDefaultDestinationFile(String modelFile) {
        modelFile = super.getDefaultDestinationFile(modelFile);
        boolean unpackaged = Boolean.getBoolean("bean.generation.unpackaged");
        return unpackaged ? modelFile : appendPackage(modelFile, extractName(codeTemplate));
    }

    /**
     * appends the given package suffix to the given model class package path
     * 
     * @param modelFile   bean or model java class
     * @param packageName source package
     * @return package + file name
     */
    protected String appendPackage(String modelFile, String packageName) {
        final int i = modelFile.lastIndexOf('/');
        modelFile = modelFile.substring(0, i) + "/"
            + packageName
            + modelFile.substring(modelFile.lastIndexOf('/'), modelFile.length());
        return modelFile;
    }
}
