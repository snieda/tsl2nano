package de.tsl2.nano.util;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import de.tsl2.nano.action.Parameter;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.StringUtil;

/**
 * prints out all values of given methods of a stream of class instances
 */
public class MarkdownLog {
	static final int DEFAULT_WIDTH = 48;
	int width = DEFAULT_WIDTH;
	
	
	/**
	 * @param title table title
	 * @param rows class instances to invoke the columnHeader-methods on
	 * @param columnHeaders method names as column headers
	 * @throws IOException
	 */
	public void printTable(String title, Stream<?> rows, Method... columnHeaders) throws IOException {
		if (columnHeaders.length == 0)
			columnHeaders = getSortedMethodsWithoutParameters();
		List<Method> headers = new LinkedList<Method>(Arrays.asList(columnHeaders));
		StringBuilder str = createHeader(title, headers);
		
		final Method[] cheaders = columnHeaders;
		rows.forEach(g -> {
			str.append("\n");
			appendRow(str, g.getClass().getSimpleName(), width, ' ');
			Arrays.stream(cheaders).forEach(m -> {
				try {
					m.setAccessible(true);
					appendRow(str, m.invoke(g), width(m), ' ');
				} catch (Exception e) {
					e.printStackTrace();
					appendRow(str, e.toString(), width(m), ' ');
				}
			});
		});
		createFooter(str, headers);
		System.out.println(str);
		
		addMarkDeepStyle(str);
		log("writing markdown table to " + getFileName(title));
		Files.write(Paths.get(getFileName(title)), str.toString().getBytes());
	}

	Method[] getSortedMethodsWithoutParameters() {
		Method[] methodsWithoutParameter = BeanClass.getBeanClass(Parameter.class).getMethods(Object.class, Modifier.PUBLIC, Modifier.PROTECTED);
		Arrays.sort(methodsWithoutParameter, new Comparator<Method>() {
			@Override
			public int compare(Method o1, Method o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		return methodsWithoutParameter;
	}

	String getFileName(String title) {
		return title + "md.html";
	}

	StringBuilder createHeader(String title, List<Method> columnHeaders) {
		StringBuilder str = new StringBuilder("\n## " + title + "\n\n");
		columnHeaders.add(0, getPrincipalColumn());
		columnHeaders.forEach(m -> appendRow(str, m.getName(), width(m), ' '));
		str.append("\n");
		columnHeaders.forEach(m -> appendRow(str, "", width(m), '-'));
		return str;
	}

	Method getPrincipalColumn() {
		try {
			return Method.class.getMethod("getName", new Class[0]);
		} catch (NoSuchMethodException | SecurityException e) {
			ManagedException.forward(e);
			return null;
		}
	}

	StringBuilder createFooter(StringBuilder str, List<Method> columnHeaders) {
		str.append("\n");
		columnHeaders.forEach(m -> appendRow(str, "", width(m), '-'));
		str.append("\n");
		return str;
	}

	void appendRow(StringBuilder str, Object content, int width, char space) {
		String trailing = space ==' ' && str.indexOf("|") >= 0 ? " " : "";
		str.append(StringUtil.fixString(trailing + format(content), width, space, true) + "|");
	}
	void addMarkDeepStyle(StringBuilder str) {
		str.append("\n\n<!-- Markdeep: --><style class=\"fallback\">body{visibility:hidden;white-space:pre;font-family:monospace}</style><script src=\"markdeep.min.js\"></script><script src=\"https://casual-effects.com/markdeep/latest/markdeep.min.js?\"></script><script>window.alreadyProcessedMarkdeep||(document.body.style.visibility=\"visible\")</script>");
	}

	int width(Method m) {
		return m.getReturnType().isArray() || Collection.class.isAssignableFrom(m.getReturnType()) ? 256 : width;
	}

	String format(Object obj) {
		return obj instanceof Class ? ((Class)obj).getSimpleName() : obj instanceof Boolean ? bool(obj) : String.valueOf(obj);
	}

	String bool(Object obj) {
		return ((Boolean)obj) ? "X" : "-";
	}

	void log(Object o) {
		System.out.println(o);
	}
}
