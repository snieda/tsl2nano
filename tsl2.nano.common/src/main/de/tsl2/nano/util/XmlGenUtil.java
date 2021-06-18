/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jun 11, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.util;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.execution.CompatibilityLayer;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.serialize.XmlUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;

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
public class XmlGenUtil extends XmlUtil {
    private static final Log LOG = LogFactory.getLog(XmlGenUtil.class);

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
            final BufferedWriter writer = new BufferedWriter(new FileWriter(FileUtil.userDirFile(destFile)));

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

}
