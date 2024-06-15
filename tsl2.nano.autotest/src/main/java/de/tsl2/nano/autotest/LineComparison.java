/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 31.03.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.autotest;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Provides field wise comparison of given rows/lines.
 * <p/>
 * normally , you will compare an expected line with an actual one. the expected
 * may contain ignorable values like timestamps and ids. these should be
 * replaced in the expected line with 'XXXX'. this will result in replacing
 * these values in the actual line , too.
 * <p/>
 * Example: new LineComparison(';', null, null).compare(
 * getContent('myexpectedfile'), getContent('myactualresultfile') );
 * <p/>
 * this call will output all differences and return the diff count.
 * 
 * @author Thomas Schneider
 */
public class LineComparison {
	String diff = ";";
	String columns[];
	SimpleDateFormat sdf;

	private static final String IGNORE = "XXXX";

	public LineComparison(String diff) {
		this(diff, null, null);
	}

	public LineComparison(String diff, String[] columns, SimpleDateFormat sdf) {
		this.diff = diff;
		this.columns = columns;
		this.sdf = sdf;
	}

	String getDiffRegEx() {
		return "[" + diff + "]";
	}

	public static String getContent(String file) throws IOException {
		return new String(Files.readAllBytes(Paths.get(file)));
	}

	protected String synchronizeIgnoresWithExpected(String expected, String content) {
		String[] exp = expected.split(getDiffRegEx());
		String[] con = content.split(getDiffRegEx());
		StringBuilder res = new StringBuilder(content.length());
		for (int i = 0; i < exp.length; i++) {
			res.append((exp[i].equals(IGNORE) ? IGNORE : con[i]) + diff);
		}
		return res.substring(0, res.length() - 1);
	}

	protected String ignoreLast(String content, String stringToIgnore) {
		int i = content.lastIndexOf(stringToIgnore);
		content = new StringBuilder(content).replace(i, i + stringToIgnore.length(), IGNORE).toString();
		return content;
	}

	public int compare(String[] expected, Object[] row, Integer... ignoreColumns) {
		return compare(expected, row, columns, sdf,
				(ignoreColumns != null ? Arrays.asList(ignoreColumns) : new ArrayList<Integer>()));
	}

	/**
	 * @param expected      expected values
	 * @param row           actual values
	 * @param columns       (optional) column names
	 * @param sdf           (optional) date format
	 * @param ignoreColumns (optional) column indexes to ignore
	 * @return count of failures
	 */
	int compare(String[] expected, Object[] row, String[] columns, SimpleDateFormat sdf, List<Integer> ignoreColumns) {
		String val;
		int failures = 0;
		String e;
		for (int i = 0; i < row.length; i++) {
			if ((ignoreColumns != null && ignoreColumns.contains(i)) || (columns != null && columns[i].endsWith("ID")))
				continue;
			e = expected[i].trim();
			if (row[i] != null || (row[i] == null && e != null)) {
				val = toString(row[i], sdf);
				if (!e.equals(val)) {
					log("\t" + i + " " + (columns != null ? columns[i] : "index " + i) + ": " + e + " <=> " + val);
					failures++;
				}
			}
		}
		return failures;
	}

	public static void log(Object msg, Object... args) {
		System.out.println(String.format(msg.toString(), args));
	}

	public static String toString(Object value, SimpleDateFormat sdf) {
		if (value == null)
			return "";
		else if (value instanceof Date)
			return sdf != null ? sdf.format(value) : DateFormat.getInstance().format(value);
		else if (value instanceof BigDecimal)
			return NumberFormat.getInstance().format(value);
		else
			return String.valueOf(value);
	}
}
