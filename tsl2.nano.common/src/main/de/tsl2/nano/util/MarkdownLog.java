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
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.StringUtil;
import java.text.MessageFormat;
import static java.text.MessageFormat.format;
/**
 * <pre>
 * creates two kinds of markdown logs:
 * print      : logs in markdown foramt
 * printTable : prints out all values of given methods of a stream of class instances
 * </pre>
 */
public class MarkdownLog {
	static final int DEFAULT_WIDTH = 48;
	int width = DEFAULT_WIDTH;
	String title;
	StringBuilder buf;
	
	static final String CTAG = "<span style=\"color:{0}\">$1</span>";
	enum Style {H1("\n# "), H2("\n## "), H3("\n### "), H4("\n#### "), H5("\n##### "), H6("\n###### ")
		, STD("\n"), LIST("\n* "), CHECKED("\n- [x] "), UNCHECKED("\n- [ ] "), IMAGE("![]($1)"), LINK("[]($1)")
		, BOLD("*$1*"), CURSIVE("_$1_)")
		, GRAY(ctag("gray")), RED(ctag("red")), GREEN(ctag("green"))
		, BLUE(ctag("blue")), YELLOW(ctag("yellow")), ORANGE(ctag("orange"));
		String txt;
		private Style(String txt) {
			this.txt = txt;
		}
	};

	static String ctag(String v) {
		return MessageFormat.format(CTAG, v);
	}
	
	MarkdownLog(String title) {
		this.title = title;
		buf = new StringBuilder(Style.H2.txt + title + "\n");
	}

	/**
	 * @param title table title
	 * @param rows class instances to invoke the columnHeader-methods on
	 * @param columnHeaders method names as column headers
	 * @throws IOException
	 */
	public void printTable(String title, Stream<?> rows, Method... columnHeaders) throws IOException {
		buf.append("\n");
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
		write(title, str);
	}

	public void write() {
		addMarkDeepStyle(buf);
		try {
			write(title, buf);
		} catch (IOException e) {
			ManagedException.forward(e);
		}
	}
	
	protected void write(String title, StringBuilder str) throws IOException {
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
		return ENV.getConfigPath() + title + "md.html";
	}

	StringBuilder createHeader(String title, List<Method> columnHeaders) {
		columnHeaders.add(0, getPrincipalColumn());
		columnHeaders.forEach(m -> appendRow(buf, m.getName(), width(m), ' '));
		buf.append("\n");
		columnHeaders.forEach(m -> appendRow(buf, "", width(m), '-'));
		return buf;
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

	public void print(Object obj) {
		print(obj, Style.STD);
	}
	public void print(Object obj, Style style) {
		print(obj, style, null, null);
	}
	public void print(Object obj, Style style, String regex, Style regexStyle) {
		String tact = obj.toString();
		if (regex != null) {
			ManagedException.assertion(regexStyle != null, "regexStyle must not be null!");
			tact = tact.replaceAll(regex, regexStyle.txt);
		}
		buf.append(style.txt + tact);
	}
}
