/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 03.12.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.core.util;

import java.io.File;
import java.io.InputStream;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.DocAttributeSet;
import javax.print.attribute.HashDocAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.JobPriority;
import javax.print.attribute.standard.PageRanges;
import javax.print.attribute.standard.PrintQuality;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.log.LogFactory;

/**
 * provides generic print convenience methods for non-interactive prints.
 * <p/>
 * NOTE: use 'PCL6 Driver for Universal Print' or 'PDFCreator' to test the printing.
 * 
 * @author Tom
 * @version $Revision$
 */
public class PrintUtil {
    private static final Log LOG = LogFactory.getLog(PrintUtil.class);

    /**
     * delegates to {@link #print(String, String, InputStream, PrintQuality, int, int[]...)}.
     */
    static DocPrintJob print(String jobName,
            String printerName,
            InputStream stream) {
        return print(jobName, printerName, stream, null, 0);
    }

    /**
     * prints through standard javax.print services
     * 
     * @param jobName print job name
     * @param printerName
     * @param stream to be printed
     * @param (optional) quality on of enum {@link PrintQuality}
     * @param (optional) priority number between 1 and 100. if lower than 1, it will be ignored!
     * @param (optional) pageranges pages to be printed
     * @return the started print job
     */
    static DocPrintJob print(String jobName,
            String printerName,
            InputStream stream,
            PrintQuality quality,
            int priority,
            int[]... pageranges) {
        DocAttributeSet das = new HashDocAttributeSet();
        PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
        pras.add(new JobName(jobName, null));
        if (priority > 0)
            pras.add(new JobPriority(priority));//1-100
        if (pageranges.length > 0)
            pras.add(new PageRanges(pageranges));
        if (quality != null)
            pras.add(quality);
        PrintService ps = null;
        if (printerName != null) {
            PrintService[] services = PrintServiceLookup.lookupPrintServices(
                DocFlavor.INPUT_STREAM.AUTOSENSE, das);
            StringBuilder info = new StringBuilder();
            for (int i = 0; i < services.length; i++) {
                info.append("\n\t" + services[i].getName());
                if (services[i].getName().equals(printerName)) {
                    ps = services[i];
                    break;
                }
            }
            if (ps == null) {
                throw new IllegalArgumentException(
                    "The printer '"
                        + printerName
                        + "' is not available. Please set printerName=null to use the default-printer or select one of:"
                        + info + "\n");
            }
        } else {
            ps = PrintServiceLookup.lookupDefaultPrintService();
        }
        DocPrintJob job = ps.createPrintJob();
        Doc doc = new SimpleDoc(stream, DocFlavor.INPUT_STREAM.AUTOSENSE, das);
        try {
            LOG.info("printing '" + jobName + "' to " + ps.getName() + "...");
            job.print(doc, pras);
            LOG.info("printing '" + jobName + "' to " + ps.getName()
                + " finished");
            return job;
        } catch (PrintException e) {
            LOG.error("print '" + jobName + "' failed on : " + ps.getName()
                + toString(das.toArray())
                + toString(job.getAttributes().toArray()));
            throw new RuntimeException(e);
        }
    }

    private static Object toString(Object[] attributes) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < attributes.length; i++) {
            buf.append("\n\t" + attributes[i].getClass().getSimpleName() + ": " + attributes[i]);
        }
        return buf.toString();
    }

    /**
     * convenience to send an apache-fop transformed file to the given printer. delegates to
     * {@link XmlUtil#fop(File, String, File)} and {@link #print(String, String, InputStream)}.
     */
    public static void printFOP(File srcFile, String mimeType, File xsltFile, String printerName) {
        print(srcFile.getName(), printerName, ByteUtil.getInputStream(XmlUtil.fop(srcFile, mimeType, xsltFile)));
    }
}