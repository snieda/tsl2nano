package de.tsl2.nano.incubation.specification;

import java.io.File;
import java.util.Properties;
import java.util.Scanner;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.incubation.specification.documentconsumer.ExcelWorkerConsumer;
import de.tsl2.nano.incubation.specification.documentconsumer.FileImportConsumer;
import de.tsl2.nano.incubation.specification.documentconsumer.SimpleDocumentTag;
import de.tsl2.nano.incubation.specification.documentconsumer.SpecificationExchangeConsumer;
import de.tsl2.nano.incubation.specification.documentconsumer.WorkflowConsumer;
import de.tsl2.nano.util.FilePath;

/**
 * reads a document like a markdown text, delegates the work through registered workers on tags to work on the content between the chapters.
 * 
 * <pre> 
# [TSL:APP] Create an Html Application from Scratch

The *[TSL:APP]* tag is a reservation for future. perhaps some libraries will be defined and downloaded or plugins are defined.

## 1. [TSL:MODEL] Create a Data Model and save it as DDL

The *[TSL:MODEL]* defines the uml model as plant-uml directly, or a path to load and save a ddl file to the environment directory.

## 2. [TSL:SPEC] Create Specification Files

The *[TSL:SPEC]* tag lets load a specification file or defines the properties inside this block

Fill *specification.properties* or *specification.properties.csv* to define the following elements.

Example:

§myrule=(x*y)+1

### 2.1 Rules&Queries&Actions&WebUrls

| Rule    | Content |
|---------|---------|


### 2.2 [TSL:DEC] Decision Tables


### 2.2 Virtual Types

add bean attributes that belong to rule definitions

### 2.3 Field Property Rules

Type    Field   Property    Rule

### 2.4 Type Actions

add bean actions that belong to rule/action definitions

### [TSL:FLOW] Flow as simple workflow

write simple text in format of gravizo to define a workflow

### [TSL:WORKER] Excelworker defining data for simple workflows

in markdown: a markdown table

### change actions

## [TSL:IMPORT] Import human readable data 
## Start the Application Framework

## Do some manual extensions

 * </pre>
 * 
 * @author Thomas Schneider
 */
public class DocumentWorker {
	private static final String LINK = "\\s+\\[.*\\]\\(.*\\)";

	private static final Log LOG = LogFactory.getLog(DocumentWorker.class);
	
	private static final String TAG_MATCH = ".*\\[\\w+\\:\\w+\\].*";
	static final String TAG_DIR = ENV.getConfigPath() + "/tagdir";
	private Properties properties;
	
	public DocumentWorker() {
		init();
	}

	protected void init() {
		properties = FileUtil.loadOptionalProperties(getPropertyFileName());
		if (properties.isEmpty()) {
			preInitProperties();
		}
		new File(TAG_DIR).mkdirs();
	}

	protected String getPropertyFileName() {
		return ENV.getConfigPath() + getClass().getSimpleName().toLowerCase() + ".properties";
	}

	protected void preInitProperties() {
		properties.put("APP", SimpleDocumentTag.class.getName());
		properties.put("MODEL", SimpleDocumentTag.class.getName());
		properties.put("SPEC", SpecificationExchangeConsumer.class.getName());
		properties.put("FLOW", WorkflowConsumer.class.getName());
		properties.put("IMPORT", FileImportConsumer.class.getName());
		properties.put("WORKER", ExcelWorkerConsumer.class.getName());
		FileUtil.saveProperties(getPropertyFileName(), properties);
	}

	public void consume(String fileName) {
		long start = System.currentTimeMillis();
		LOG.info("\n=============================================================================");
		LOG.info("starting documentworker on " + fileName);
		LOG.info("=============================================================================\n");
		Message.send("starting documentworker on " + fileName);
		Scanner sc = Util.trY( () -> new Scanner(new File(fileName)));
		String l, tag = null, tagfile;
		StringBuilder content = new StringBuilder();
		int line = 0;
		while (sc.hasNextLine()) {
			l = sc.nextLine();
			line++;
			try {
				if (isNewChapterWithTag(l)) {
					if (content.length() > 0) {
						tagfile = writeLastChapter(line, tag, content);
						runTag(tag, tagfile);
						tag = readTag(l);
						content.setLength(0);
					} else {
						tag = readTag(l);
					}
				}
				else if (l.matches(LINK)) {
					String link = StringUtil.substring(l, "(", ")");
					content.append(new String(FilePath.read(link)));
				} else if (!isCodeTag(l))
					content.append(l + "\n");
			} catch (Exception e) {
				throw new IllegalStateException("Exception thrown reading line [" + line + "]:" + l, e);
			}
		}
		LOG.info("documentworker finished (time: " + DateUtil.fromStartTime(start) + ", file: " + fileName + ")");
		LOG.info("=============================================================================\n");

	}

	private void runTag(String tag, String tagfile) {
		LOG.info("starting " + tag + " on " + tagfile);
		if (!properties.containsKey(tag))
			throw ManagedException.implementationError("can't consume tag", tag, properties.keySet().toArray());
		Class<Consumer<File>> workerType = BeanClass.load(properties.getProperty(tag));
		Consumer<File> worker;
		if ((worker = ENV.get(workerType)) == null)
			BeanClass.createInstance(workerType);
		worker.accept(new File(tagfile));
	}

	protected String writeLastChapter(int line, String tag, StringBuilder content) {
		String tagfile = TAG_DIR + "/" + tag + "-" + line;
		FilePath.write(tagfile, content.toString().getBytes());
		return tagfile;
	}

	private String readTag(String l) {
		return StringUtil.substring(l, ":", "]");
	}

	private boolean isNewChapterWithTag(String l) {
		return isChapter(l) && l.matches(TAG_MATCH);
	}

	private boolean isCodeTag(String l) {
		return l.startsWith("~~~") || l.startsWith("===");
	}

	private boolean isChapter(String l) {
		return l.startsWith("#");
	}

}
