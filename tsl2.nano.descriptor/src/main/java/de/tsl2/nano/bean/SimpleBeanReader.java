package de.tsl2.nano.bean;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.stream.Collectors;

import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * tries to import file content into beans of given {@link #type}. each line has to respect the given {@link #expression}. the {@link #expression} has following format:
 * <pre>
 * 	FIELDNAME1{non-alphanumeric-splitter1}FIELDNAME2{non-alphanumeric-splitter2}...
 * </pre>
 * Example:
 * <pre>
 * Bean Attributes: date, fromtime, untiltime, pause, sum, object, description
 * 
 * Expression:
 * 	
 * DATE: FROMTIME-UNTILTIME (PAUSE) XXX OBJECT: DESCRIPTION [+HOURS h OBJECT]
 * 
 * Line:
 * 21.06.: 07:30-17:00(0,5h)  9,0h TICKET-23 Analyse

 * </pre>
 * see {@link #read(String)}.
 * @author ts
 * @param <T> bean type
 */
public class SimpleBeanReader<T> {
	private static final String SPLIT_NAME = "[^\\w]+";
	Class<T> type;
	String expression;
	
	public SimpleBeanReader(Class<T> type, String expression) {
		this.type = type;
		this.expression = expression;
	}
	
	/**
	 * @param fileName
	 * @return loaded beans
	 */
	public Collection<T> read(String fileName) {
		LinkedList<T> content = new LinkedList<>();
		String[] columns = expression.split(SPLIT_NAME);
		String[] splitter = Arrays.asList(expression.split("[\\w]+")).stream()
				.filter(s -> s.length() > 0)
				.map(s -> s.trim().isEmpty() ? s : s.trim())
				.collect(Collectors.toList()).toArray(new String[0]);
		Bean<T> bean;
		
		Scanner sc = Util.trY( () -> new Scanner(new File(fileName)));
		String l, values[];
		while (sc.hasNextLine()) {
			l = sc.nextLine();
			if (l.trim().isEmpty() || l.startsWith("#"))
				continue;
			values = StringUtil.splitFix(l, false, splitter);
		
			bean = Bean.newBean(type);
			for (int i = 0; i < values.length; i++) {
				bean.setParsedValue(columns[i], values[i]);
			}
			content.add(bean.getInstance());
		}
		return content;
	}
}
