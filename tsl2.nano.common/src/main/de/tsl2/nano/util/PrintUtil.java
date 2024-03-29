/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 03.12.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.util;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocFlavor.INPUT_STREAM;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.DocAttributeSet;
import javax.print.attribute.HashDocAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.DocumentName;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.JobPriority;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.PageRanges;
import javax.print.attribute.standard.PrintQuality;
import javax.print.attribute.standard.RequestingUserName;
import javax.print.event.PrintJobEvent;
import javax.print.event.PrintJobListener;

import org.apache.commons.logging.Log;
import org.apache.xmlgraphics.util.MimeConstants;

import de.tsl2.nano.core.Argumentator;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.execution.IRunnable;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;

/**
 * provides generic print convenience methods for non-interactive prints.
 * <p/>
 * NOTE-1: use 'PCL6 Driver for Universal Print' or 'PDFCreator' to test the printing.
 * <p/>
 * NOTE-2: PCL-Problem: http://xmlgraphics.apache.org/fop/1.0/output.html:<br/>
 * 
 * If you run into the problem that the printed output is incomplete on Windows: this often happens to users printing to
 * a PCL printer. There seems to be an incompatibility between Java and certain PCL printer drivers on Windows. Since
 * most network-enabled laser printers support PostScript, try switching to the PostScript printer driver for that
 * printer model.
 * 
 * @author Tom
 * @version $Revision$
 */
public class PrintUtil {
    private static final Log LOG = LogFactory.getLog(PrintUtil.class);

    /**
     * prints all files in tree matching the given include expression
     * 
     * @param jobName jobname of print job
     * @param printerName printer to use
     * @param dir base directory to start from
     * @param include regular or ant expression for file matching
     * @return all print jobs done.
     */
    static Iterable<DocPrintJob> printFileSet(final String jobName,
            final String printerName,
            String dir,
            String include) {
        return FileUtil.forTree(dir, include, new IRunnable<DocPrintJob, File>() {
            @Override
            public DocPrintJob run(File context, Object... extArgs) {
                return print(jobName, printerName, context.getPath());
            }
        });
    }

    /**
     * delegates to {@link #print(String, String, InputStream, PrintQuality, int, int[]...)}.
     */
    public static DocPrintJob print(String fileName) {
        return print(null, fileName);
    }

    /**
     * delegates to {@link #print(String, String, InputStream, PrintQuality, int, int[]...)}.
     */
    public static DocPrintJob print(String printerName, String fileName) {
        return print(printerName + "-" + fileName, printerName, fileName);
    }

    /**
     * delegates to {@link #print(String, String, InputStream, PrintQuality, int, int[]...)}.
     */
    static DocPrintJob print(String jobName,
            String printerName,
            String fileName) {
        return print(jobName, printerName, FileUtil.getFile(fileName), null, null, null, null, 0);
    }

    /**
     * delegates to {@link #print(String, String, InputStream, PrintQuality, int, int[]...)}.
     */
    static DocPrintJob print(String jobName,
            String printerName,
            InputStream stream) {
        return print(jobName, printerName, stream, null, null, null, null, 0);
    }

    /**
     * prints through standard javax.print services
     * 
     * @param jobName print job name
     * @param printerName to select a special available printer
     * @param stream stream to be sent to the printer
     * @param mimeType (optional) mime-type of stream content
     * @param username (optional) user-name to be used by the printer
     * @param paperSize (optional) something like {@link MediaSizeName#ISO_A4}.
     * @param quality (optional) quality one of enum {@link PrintQuality}
     * @param priority (optional) priority number between 1 and 100. if lower than 1, it will be ignored!
     * @param pageranges (optional) pageranges pages to be printed
     * @return the started print job
     */
    public static DocPrintJob print(String jobName,
            String printerName,
            InputStream stream,
            String mimeType,
            String username,
            MediaSizeName paperSize,
            PrintQuality quality,
            int priority,
            int[]... pageranges) {
        HashPrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
        //sometimes, the user name will not be set
        if (username != null) {
            pras.add(new RequestingUserName(username, Locale
                .getDefault()));
        }
        if (paperSize != null) {
            pras.add(paperSize);
        }
        if (priority > 0)
         {
            pras.add(new JobPriority(priority));//1-100
        }
        if (pageranges != null && pageranges.length > 0) {
            pras.add(new PageRanges(pageranges));
        }
        if (quality != null) {
            pras.add(quality);
        }
        return print(jobName, printerName, stream, mimeType, pras);
    }

    /**
     * delegates to {@link #print(String, String, InputStream, String, PrintRequestAttributeSet)}
     */
    static DocPrintJob print(String jobName,
            String printerName,
            InputStream stream,
            String mimeType,
            PrintRequestAttribute... attributes) {
        HashPrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
        for (int i = 0; i < attributes.length; i++) {
            pras.add(attributes[i]);
        }
        return print(jobName, printerName, stream, mimeType, pras);
    }

    /**
     * prints through standard javax.print services
     * 
     * @param jobName print job name
     * @param printerName to select a special available printer
     * @param stream stream to be sent to the printer. if null, a printer-info will be shown!
     * @param mimeType (optional) mime-type of stream content
     * @param pras additional print attributes
     * @return the print job
     */
    public static DocPrintJob print(String jobName,
            String printerName,
            InputStream stream,
            String mimeType,
            PrintRequestAttributeSet pras) {
        DocAttributeSet das = new HashDocAttributeSet();
        das.add(new DocumentName(jobName, Locale.getDefault()));

        INPUT_STREAM docFlavor = mimeType != null ? new DocFlavor.INPUT_STREAM(
            mimeType) : DocFlavor.INPUT_STREAM.AUTOSENSE;

        pras.add(new JobName(jobName + "/" + docFlavor.getMediaSubtype(), null));

        //seems that not all printers provide their supported docFlavors
        PrintService ps = getPrintService(printerName, null/*docFlavor*/, das);
        if (stream == null) {
        	if (ps != null)
        		logInfo(null, das, ps);
        	else
        		LOG.info("no printer-service available!");
            return null;
        }

        DocPrintJob job = ps.createPrintJob();
        Doc doc = new SimpleDoc(stream, docFlavor, das);
        job.addPrintJobListener(getPrintJobListener());
        try {
            LOG.info("printing '" + jobName + "' to " + ps.getName()
                + " with doc-flavor '" + docFlavor + "'...");
            job.print(doc, pras);
            logInfo(job, das, ps);
            LOG.info("printing '" + jobName + "' to " + ps.getName()
                + " finished");
            return job;
        } catch (PrintException e) {
            logInfo(job, das, ps);
            LOG.error("print '" + jobName + "' failed on '" + ps.getName() + "' with doc-flavor '" + docFlavor + "'");
            throw new RuntimeException(e);
        }
    }

    private static void logInfo(DocPrintJob job, DocAttributeSet das,
            PrintService ps) {
        String info = toString(das.toArray())
            + (job != null ? toString(job.getAttributes().toArray()) : "")
            + toString(ps.getAttributes().toArray())
            + toString(ps.getSupportedDocFlavors())
            + toString(ps.getSupportedAttributeCategories());
        LOG.info(info);
        if (!LogFactory.isPrintToConsole()) {
            System.out.println(info);
        }
    }

    private static PrintJobListener getPrintJobListener() {
        return new PrintJobListener() {

            @Override
            public void printJobRequiresAttention(PrintJobEvent pje) {
                LOG.info("print job requires attention: " + toString(pje));
            }

            @Override
            public void printJobNoMoreEvents(PrintJobEvent pje) {
                LOG.info("print job has nor more events: " + toString(pje));
            }

            @Override
            public void printJobFailed(PrintJobEvent pje) {
                LOG.info("print job failed: " + toString(pje));
            }

            @Override
            public void printJobCompleted(PrintJobEvent pje) {
                LOG.info("print job completed: " + toString(pje));
            }

            @Override
            public void printJobCanceled(PrintJobEvent pje) {
                LOG.info("print job canceled: " + toString(pje));
            }

            @Override
            public void printDataTransferCompleted(PrintJobEvent pje) {
                LOG.info("print job data transfer completed: " + toString(pje));
            }

            private Object toString(PrintJobEvent pje) {
                return pje.getPrintJob().getAttributes().get(JobName.class);
            }
        };
    }

    public static PrintService getPrintService(String printerName, String mimeType) {
        return getPrintService(printerName, mimeType != null ? new DocFlavor.INPUT_STREAM(mimeType)
            : DocFlavor.INPUT_STREAM.AUTOSENSE, new HashDocAttributeSet());
    }

    public static PrintService getPrintService(String printerName,
            INPUT_STREAM docFlavor, DocAttributeSet das) {
        PrintService ps = null;
        PrintService[] services = PrintServiceLookup.lookupPrintServices(
            docFlavor, das);
        StringBuilder info = new StringBuilder();
        for (int i = 0; i < services.length; i++) {
            info.append("\n\t" + services[i].getName());
            if (services[i].getName().equals(printerName)) {
                ps = services[i];
                break;
            }
        }
        if (ps == null) {
            if (printerName != null) {
                throw new IllegalArgumentException(
                    "The printer '"
                        + printerName + "' is not available"
                        + (docFlavor != null ? " for doc-flavor '"
                            + docFlavor.toString() : "")
                        + "'. Please set printerName=null to use the default-printer or select one of:"
                        + info + "\n");
            } else {
                LOG.info("available printers are:" + info);
                ps = PrintServiceLookup.lookupDefaultPrintService();
                LOG.info("\nusing default printer: " + ps.getName());
            }
        }
        return ps;
    }

    private static String toString(Object[] attributes) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < attributes.length; i++) {
            buf.append("\n\t" + attributes[i].getClass().getSimpleName() + ": " + attributes[i]);
        }
        return buf.toString();
    }

    /**
     * convenience to send an apache-fop transformed file to the given printer. delegates to
     * {@link XmlGenUtil#fop(File, String, File)} and {@link #print(String, String, InputStream)}.
     */
    public static void printFOP(File srcFile, String mimeType, File xsltFile, String printerName) {
        print(srcFile.getName(), printerName, ByteUtil.getInputStream(XmlGenUtil.fop(srcFile, mimeType, xsltFile)));
    }

    private static Map<String, String> getManual() {
        LinkedHashMap<String, String> man = new LinkedHashMap<String, String>();
        man.put(
            "source",
            "(!) source file name (may be a fileset like '**/*.pdf') to be read and sent to the given printer.It should have a printer-known mimetype like 'application/postscript'. if source = 'printer-info', nothing will be done except showing all informations of given printer. If source is an xml file, a fop transformation will be done.");
        man.put("printer",
            "printer name to send the stream to. if printer name is the only argument, all printers properties will be shown");
        man.put("papersize", Argumentator.staticNames(MediaSizeName.class, MediaSizeName.class));
        man.put("jobname", "job name to be shown by the printers job and as document name");
        man.put("mimetype", Argumentator.staticValues(MimeConstants.class, String.class));
        man.put("quality", Argumentator.staticNames(PrintQuality.class, PrintQuality.class));
        man.put("priority", "value between 1 and 100");
        man.put("pageranges", "page to print. e.g. '1-2;5-6;8-12'");
        man.put(
            "xsltfile",
            "apache fop transformation file. then, the source file has to be an xml data file to be transformed by fop transformation");
        man.put("example-1", "print source=printer-info");
        man.put("example-2", "print source=printer-info printer=PDFCreator");
        man.put("example-3", "print source=myfile.pdf printer=PDFCreator");
        man.put("example-4", "print source=**/*.pdf printer=PDFCreator papersize=ISO_A4");
        return man;
    }

    public static void main(String[] args) {
        Argumentator am = new Argumentator("print", getManual(), args);
        if (am.check(System.out)) {
            String source = am.get("source");
            String printer = am.get("printer");
            String jobname = am.get("jobname");
            if (jobname == null) {
                jobname = source;
            }
            String ps = am.get("papersize");
            MediaSizeName paper = null;
            if (ps != null) {
                paper = (MediaSizeName) BeanClass.getStatic(MediaSizeName.class, ps);
            }
            PrintQuality quality = null;
            String qual = am.get("quality");
            if (qual != null) {
                quality = (PrintQuality) BeanClass.getStatic(PrintQuality.class, qual);
            }
            Integer priority;
            String prio = am.get("priority");
            priority = prio != null ? Integer.valueOf(prio) : 0;
            int[][] pageranges = null;//am.get("pageranges");
            InputStream stream;
            if (source.equals("printer-info")) {//simple printer info
                print("printer-info", printer, (InputStream) null);
                return;
            } else if (source.endsWith(".xml")) {//fop transformation
                String xslt = am.get("xsltfile");
                if (xslt == null) {
                    throw new IllegalArgumentException(
                        "if source is an xml file, an xslt-transformation file has to be given!");
                }
                File xsltFile = new File(xslt);
                String mimeType = am.get("mimetype");
                if (mimeType == null) {
                    mimeType = "application/pdf";
                }
                if (source.contains("*")) {
                    throw new IllegalArgumentException(
                        "doing an apache fop transformation, no file filter is allowed. please provide an explicit file name!");
                }
                byte[] fop = XmlGenUtil.fop(new File(source), mimeType, xsltFile);
                //write the output to a file for e.g. debugging
                FileUtil.writeBytes(fop, source + "." + StringUtil.substring(mimeType, "/", null), false);
                stream = ByteUtil.getInputStream(fop);
            } else if (source.contains("*")) {//fileset
                List<File> fileset = FileUtil.getFileset("./", source);
                for (File file : fileset) {
                    stream = FileUtil.getFile(file.getPath());
                    print(jobname, printer, stream, am.get("mimetype"), am.get("username"),
                        paper, quality, priority, pageranges);
                }
                return;
            } else {
                stream = FileUtil.getFile(source);
            }
            print(jobname, printer, stream, am.get("mimetype"), am.get("username"),
                paper, quality, priority, pageranges);
        }
    }

}