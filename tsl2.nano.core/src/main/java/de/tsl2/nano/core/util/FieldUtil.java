package de.tsl2.nano.core.util;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class FieldUtil extends ByteUtil {
	
	public static Object[][] toObjectArrays(final Collection<?> list, String... attributes) {
		Object[][] rows = new Object[list.size()][];
		if (list.size() > 0 && (attributes == null || attributes.length == 0))
			attributes = getFieldNames(list.iterator().next().getClass());
		int i = 0;
		for (Object o : list) {
			rows[i++] = o != null && o.getClass().isArray() ? (Object[]) o : toObjectArray(o, attributes);
		}
		return rows;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object[] toObjectArray(final Object obj, String... attributes) {
		return foreach((f, values) -> values.add(getValue(obj, f)), new ArrayList(), obj, attributes).toArray();
	}

	public static String toString(final Object obj, String... attributes) {
		return foreach((f, b) -> b.append(f.getName() + "=" + getValue(obj, f) + ","), new StringBuilder(), obj, attributes).toString();
	}

	public static Map<String, Object> toMap(final Object obj, String... attributes) {
		return foreach((f, m) -> m.put(f.getName(), getValue(obj, f)), new LinkedHashMap<String, Object>(), obj, attributes);
	}

	public static Map<String, Object> fromMap(final Object obj, Map<String, Object> map) {
		return foreach((f, m) -> setValue(obj, f, m.get(f.getName())), map, obj);
	}

	public static Object getValue(Object obj, Field f) {
		try {
			return f.get(obj);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static void setValue(Object obj, Field f, Object value) {
		try {
			f.set(obj, value);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static <Container, Result> Container foreach(BiConsumer<Field, Container> callback, Container container,
			Object obj, String... attributes) {
		Class<? extends Object> cls = obj.getClass();
		if (attributes == null || attributes.length == 0)
			attributes = getFieldNames(cls);
		for (int i = 0; i < attributes.length; i++) {
			try {
				callback.accept(cls.getDeclaredField(attributes[i].toLowerCase()), container);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return container;
	}

	public static String[] getFieldNames(Class<? extends Object> cls) {

		return Arrays.stream(cls.getDeclaredFields())
			.filter(f -> !f.isSynthetic())
            .map(f -> f.getName())
            .sorted()
            .toArray(String[]::new);
	}

	public static <T> T print(T result) {
		return print("\t=> result: ", result, null, System.out);
	}

	public static <T> T print(String title, T result, String[] header) {
		return print(title, result, header, System.out);
	}

	public static <T> T print(String title, T obj, String[] header, PrintStream ps) {
		if (!ObjectUtil.isEmpty(obj) && obj.getClass().isArray()) {
			new ObjectPrinter((Object[][]) obj).print(ps);
		} else if (!ObjectUtil.isEmpty(obj) && obj instanceof Collection) {
			Object row0 = ((Collection) obj).iterator().next();
			header = header != null ? header : row0.getClass().isArray() ? null : ObjectUtil.getFieldNames(row0.getClass());
			new ObjectPrinter(title, ObjectUtil.toObjectArrays((Collection) obj), header).print(System.out);
		} else
			ps.append(title + obj);
		return obj;
	}
}

class ObjectPrinter {
	private static final char SPACE = ' ';
	private static final char LINE = '-';
	private static final String SLINE = String.valueOf(LINE);
	private static final String DEFAULT_DELIMITER = "|";
	String title;
	String[] header;
	Object[][] rows;
	String[] frame;
	private String leftDelimiter;
	private String rightDelimiter;
	private int[] colsizes;
	int MAX_WIDTH = Integer.valueOf(System.getProperty("tsl2.nano.printer.maxwidth", "60"));

	public ObjectPrinter(Object[][] rows) {
		this(null, rows, null);
	}

	public ObjectPrinter(String title, Object[][] rows, String[] header) {
		this(title, rows, header, DEFAULT_DELIMITER, DEFAULT_DELIMITER + "\n");
	}

	public ObjectPrinter(String title, Object[][] rows, String[] header, String leftDelimiter, String rightDelimiter) {
		this.title = title;
		this.rows = rows;
		this.header = evalHeader(header, rows);
		this.frame = evalFooter();
		this.leftDelimiter = leftDelimiter;
		this.rightDelimiter = rightDelimiter;
	}

	private String[] evalFooter() {
		String[] footer = new String[header.length];
		for (int i = 0; i < footer.length; i++) {
			footer[i] = SLINE;
		}
		return footer;
	}

	private String[] evalHeader(String[] header, Object[][] rows) {
		if (header == null) {
			header = new String[evalColumnSizes(rows).length];
			for (int c = 0; c < colsizes.length; c++) {
				header[c] = "" + (c + 1);
			}
		} else {
			evalColumnSizes(rows);
			evalColumnSizes(colsizes, header);
		}
		return header;
	}

	void print(PrintStream ps) {
		ps.append("\n" + (title != null ? title + "\n" : ""));
		print(ps, new Object[][] { header }, SPACE);
		print(ps, new Object[][] { frame }, LINE);
		print(ps, rows, SPACE);
		print(ps, new Object[][] { frame }, LINE);
	}

	void print(PrintStream ps, Object[][] rows, char fillchar) {
		int[] colsizes = evalColumnSizes(rows);
		if (colsizes.length > 0) {
			String cell, spacer;
			for (int r = 0; r < rows.length; r++) {
				for (int c = 0; c < colsizes.length; c++) {
					cell = String.valueOf(rows[r][c]);
					spacer = cell.equals(SLINE) ? SLINE : " ";
					ps.append(leftDelimiter + spacer + filterreturn(fixwith(cell, colsizes[c], fillchar)));
				}
				ps.append(rightDelimiter);
			}
		}
	}

	private String filterreturn(String txt) {
		return txt.replace('\r', SPACE).replace('\n', SPACE);
	}

	private String fixwith(String txt, int width, char fillchar) {
		StringBuilder b = new StringBuilder(width);
		b.append(txt.length() > width ? txt.substring(0, width) : txt);
		for (int i = txt.length(); i < width; i++)
			b.append(fillchar);
		return b.toString();
	}

	private int[] evalColumnSizes(Object[][] rows) {
		if (colsizes == null) {
			int length = rows.length > 0 ? rows[0].length : 0;
			int[] colsizes = new int[length];
			Object[] row;
			for (int r = 0; r < rows.length; r++) {
				row = rows[r];
				evalColumnSizes(colsizes, row);
			}
			this.colsizes = colsizes;
		}
		return colsizes;
	}

	private void evalColumnSizes(int[] colsizes, Object[] row) {
		for (int c = 0; c < colsizes.length; c++) {
			colsizes[c] = Math.max(Math.min(MAX_WIDTH, String.valueOf(row[c]).length()), colsizes[c]);
		}
	}
}
