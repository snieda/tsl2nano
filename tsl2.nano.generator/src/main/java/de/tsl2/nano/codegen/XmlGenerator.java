/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: 07.12.2008
 * 
 * Copyright: (c) Thomas Schneider 2008, all rights reserved
 */
package de.tsl2.nano.codegen;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.velocity.VelocityContext;
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
 *   1. xml-file
 *   2. xpath-nodelist xpath expression to filter a node list to generate code for each node
 *   3. velocity template file
 *   4. class name of generator specialization (default: XmlGenerator)
 *   5. property-file name (default: null, system-properties will be used anyway)
 *   
 * example:
 *  XmlGenerator api.xml codegen/api.vm
 *  
 * </pre>
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class XmlGenerator extends ACodeGenerator {
    private String xmlFile;
    private String xmlXPath;

    @Override
    public void initParameter(Properties args) {
        super.initParameter(args);
        xmlFile = args.getProperty(KEY_MODEL);
        xmlXPath = args.getProperty(KEY_FILTER);
    }

    @Override
    public void start(Properties args) {
        initParameter(args);
        generate();
    }
    
    /**
     * starts the generate process
     */
    @SuppressWarnings("rawtypes")
    public void generate() {
        NodeList nodeList =  XmlUtil.xpath(xmlXPath, xmlFile, NodeList.class);
        if (nodeList.getLength() == 0)
            throw new IllegalStateException("given xpath '" + xmlXPath + "' hits no xml element!");
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
        final String destFile = getDefaultDestinationFile(getUtil().getNodeText(node));
        final Properties p = getProperties();
        super.generate(node, getUtil().getNodeText(node), codeTemplate, destFile, p);
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

    @Override
    protected GeneratorXmlUtility getUtil() {
        return (GeneratorXmlUtility) super.getUtil();
    }
    @Override
    protected GeneratorUtility createUtilityInstance() {
        return new GeneratorXmlUtility();
    }

    @Override
    protected void fillVelocityContext(Object model,
            String modelFile,
            String templateFile,
            String destFile,
            Properties properties,
            GeneratorUtility util,
            VelocityContext context) {
        super.fillVelocityContext(model, modelFile, templateFile, destFile, properties, util, context);
        context.put("node", model);
    }
}
