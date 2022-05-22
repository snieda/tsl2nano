package de.tsl2.nano.incubation.specification;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.function.Consumer;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
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

	static final String TAG_DIR = ENV.getConfigPath() + "/tagdir";
	private Properties properties;

	enum Tag { APP, IMPORT, SPEC, MODEL, FLOW, WORKFLOW};

	
	public DocumentWorker() {
		init();
	}

	protected void init() {
		properties = FileUtil.loadProperties(ENV.getConfigPath() + "/" + getClass().getSimpleName().toLowerCase() + ".properties");
		if (properties.isEmpty()) {
			preInitProperties();
		}
		new File(TAG_DIR).mkdirs();
	}

	protected void preInitProperties() {
		properties.put("APP", null);
		properties.put("MODEL", null);
		properties.put("SPEC", null);
		properties.put("IMPORT", null);
	}

	public void read(String fileName) {
		long start = System.currentTimeMillis();
		Scanner sc = Util.trY( () -> new Scanner(new File(fileName)));
		String l, tag = null, tagfile;
		StringBuilder content = new StringBuilder();
		int line = 0;
		while (sc.hasNextLine()) {
			l = sc.nextLine();
			line++;
			content.append(l + "\n");
			try {
				if (isNewChapter(l)) {
					tag = readTag(l);
					if (content.length() > 0) {
						tagfile = writeLastChapter(line, tag, content);
						runTag(tag, tagfile);
						content.setLength(0);
					}
				}
			} catch (Exception e) {
				throw new IllegalStateException("Exception thrown reading line [" + line + "]:" + l, e);
			}
		}
	}

	private void runTag(String tag, String tagfile) {
		System.out.println("starting " + tag + " on " + tagfile);
		Consumer<File> worker = BeanClass.createInstance(properties.getProperty(tag));
		worker.accept(new File(tagfile));
		
		// TODO: let SpecificationExchange, Workflow, ExcelWorker and TransformableBeanReader implement Consumer
	}

	private String writeLastChapter(int line, String tag, StringBuilder content) {
		String tagfile = TAG_DIR + "/" + tag + "-" + line;
		FilePath.write(tagfile, content.toString().getBytes());
		return tagfile;
	}

	private String readTag(String l) {
		return StringUtil.substring(l, ":", "]");
	}

	private boolean isNewChapter(String l) {
		return l.startsWith("#");
	}

	static class StandardDocumentTag implements Consumer<File> {

		@Override
		public void accept(File f) {
			byte[] content = FilePath.read(f.getAbsolutePath());
			// TODO: what to do? set definitions, load and start actions?
		}
		
	}
}
