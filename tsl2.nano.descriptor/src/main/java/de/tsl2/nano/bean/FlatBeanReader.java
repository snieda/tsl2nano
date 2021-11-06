package de.tsl2.nano.bean;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanModifier;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.CollectionUtil;
import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * <pre>
 * tries to import file content into beans of given {@link #type}. each line has to respect the given {@link #expression}. the {@link #expression} has following format:
 *
 * 	FIELDNAME1{non-alphanumeric-splitter1}FIELDNAME2{non-alphanumeric-splitter2}...
 * 
 * Features:
 * 	1. splitter with optional regular expression (marker: ^)
 *  2. splitter with optional ignoring part (marker: °)
 *  3. import file with comment (line start: #)
 *  4. import file with definition of variables (line start: §)
 *  5. value transformer as BiConsumer<String[], Map<String, Object> implementation
 *  6. default values ( key/values with attributename and value)
 *   
 * Example:
 * 
 * Bean Attributes: date, fromtime, untiltime, pause, sum, object, description
 * 
 * Expression:
 * 	
 * DATE: FROMTIME-UNTILTIME (PAUSE) XXX OBJECT: DESCRIPTION [+HOURS h OBJECT]
 * 
 * Line:
 * 21.06.: 07:30-17:00(0,5h)  9,0h TICKET-23 Analyse
 * 
 * Example:
	# definitions:
	§ type=org.anonymous.project.Charge
	§ expression=fromdate: fromtime-totime (pause^\)\:? ^value°:° chargeitem comment
	§ transformer=de.tsl2.nano.h5.timesheet.SBRImport
	§ fromtime=08:30
	§ totime=17:00
	$ pause=00:00:00
	§ value=0
	
	## ZEITEN ab 2019
	
	<details>
	  <summary>Click to expand!</summary>
	
	~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	
	
	07.01.: 08:30-18:00(1,0h): 8,5h Ticket-1234
	08.01.: 09:30-19:00(1,0h): 8,5h Ticket-2345
	...
 * </pre>
 * see {@link #read(String)}.
 * @author ts
 * @param <T> bean type
 */
public class FlatBeanReader<T> {

	private static final Log LOG = LogFactory.getLog(FlatBeanReader.class);
	
	private static final String SPLIT_NAME = "[^\\w]+";
	private static final String REGEX_NAME = "[\\w]+";

	Class<T> type;
	String expression;
	Map<String, Object> properties = new HashMap<>();
	
	/** read type and expression on file start */
	public FlatBeanReader() {
	}

	public FlatBeanReader(Class<T> type, String expression) {
		this.type = type;
		this.expression = expression;
	}

	protected void define(String paragraphLine) {
		String l = paragraphLine;
		String name = StringUtil.substring(l, "§", "=").trim();
		String value = StringUtil.substring(l, "=", null).trim();
		properties.put(name, value);

		switch(name) {
			case "type": 
				type = ObjectUtil.wrap(value, Class.class); 
				break;
			case "expression": 
				expression = value;
				init(expression);
				break;
		}
	}
	protected <T> T get(String key, T defaultValue) {
		return (T) (defaultValue != null ? ObjectUtil.wrap(properties.getOrDefault(key, defaultValue), defaultValue.getClass()) 
				: properties.getOrDefault(key, defaultValue));
	}

	protected void init(String expression) {
		String[] columns = getColumns(expression);
		String[] splitter = getSplitter(expression);
		properties.put("columns", columns);
		properties.put("splitter", splitter);
		String info = 	"\n========================================>" +
						"\nflat bean import : " + type.getName() +
						"\n  columns to import: " + StringUtil.toString(columns, -1) +
						"\n  column  splitter : " + StringUtil.toString(splitter, -1);
		LOG.info(info);
	}

	protected String[] getSplitter(String expression) {
		return Arrays.asList(expression.split(REGEX_NAME)).stream()
				.filter(s -> s.length() > 0)
				.map(s -> s.trim().isEmpty() || s.contains("^") || s.contains("°") ? s : s.trim())
				.collect(Collectors.toList()).toArray(new String[0]);
	}

	protected String[] getColumns(String expression) {
		return expression.split(SPLIT_NAME);
	}

	/**
	 * @param fileName
	 * @return loaded beans
	 */
	public Collection<T> read(String fileName) {
		long start = System.currentTimeMillis();
		if (expression != null) {
			init(expression);
		}
		String[] columns = get("columns", null);
		String[] splitter = get("splitter", null);
		LinkedList<T> content = new LinkedList<>();
		Bean<T> bean;
		Scanner sc = Util.trY( () -> new Scanner(new File(fileName)));
		String l, values[];
		int line = 0;
		List<Integer> ignoredlines = new LinkedList<>();
		while (sc.hasNextLine()) {
			l = sc.nextLine();
			line++;
			try {
				if (l.trim().isEmpty() || l.startsWith("#"))
					continue;
				else if (l.startsWith("§")) {
					define(l);
					columns = get("columns", null);
					splitter = get("splitter", null);
					continue;
				}
				checkState();
				values = StringUtil.splitFix(l, true, splitter);
				if (values == null) {
					ignoredlines.add(line);
					continue;
				}
				LOG.info("import " + line + ": " + l);
				transformValues(values, properties);
				bean = Bean.newBean(type);
//				bean.fromValueMap(bean.getInstance(), properties);
				// set default values, loaded from import file
				new BeanModifier().refreshValues(bean, CollectionUtil.getPropertiesOfType(properties, String.class));
				for (int i = 0; i < values.length; i++) {
					bean.setParsedValue(columns[i], values[i]);
				}
				content.add(bean.getInstance());
				properties.put("lastValues", values);
			} catch (Exception e) {
				throw new IllegalStateException("Exeption thrown reading line " + line + ":" + l, e);
			}
		}
		printInfo(content.size(), ignoredlines, System.currentTimeMillis() - start);
		return content;
	}

	void printInfo(int loaded, List<Integer> ignoredlines, long duration) {
		String info = "\n<====================================" + 
				"\nFlatBeanReader finished! (" + new SimpleDateFormat("HH:mm:ss").format(new Date(duration)) + ")" + 
				"\nlines loaded: " + loaded +
				"\nlines ignored: " + ignoredlines;
		LOG.info(info);
	}

	protected void transformValues(String[] values, Map<String, Object> properties) {
		String strTransformer;
		if ((strTransformer = get("transformer", null)) != null)
			((BiConsumer<String[], Map<String, Object>>)BeanClass.createInstance(strTransformer)).accept(values, properties);
	}

	protected void checkState() {
		assert !Util.isEmpty(expression);
		assert type != null;
	}
}
