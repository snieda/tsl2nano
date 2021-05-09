package de.tsl2.nano.autotest.creator;

import static de.tsl2.nano.autotest.creator.AFunctionTester.PREF_PROPS;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.ClassFinder;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.FormatUtil;
import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * generates and prints {@link Expectations} annotations on all methods, fitting
 * the given class and method filter.
 * <p/>
 * these methods will be run with randomized parameter values. the results will
 * be used to fill the {@link Expect} values of the {@link Expectations}
 * annoation.
 * <p/>
 * so, the generated {@link Expectations} contain values to specify the code
 * implementation as is!
 * <p/>
 * works only on methods with simple parameter and return types. 
 * if the method returns null, no expectations will be created
 * 
 * @author ts
 */
public class AutoTestGenerator {
	static String fileName = "target/autotest/generated/generated-autotests-";
	static int methods_loaded = 0;
	static int count = 0;
	static int fails = 0;
	static int nullresults = 0;
	static int load_method_error = 0;
	static int load_unsuccessful = 0;
	static int filter_typeconversions = 0;
	static int filter_errors = 0;
	static int filter_unsuccessful = 0;
	static int filter_nullresults = 0;
	static int filter_complextypes = 0;
	
	public static void main(String[] args) {
		generateExpectations(0, def("filter", Util.FRAMEWORK_PACKAGE), Modifier.STATIC | Modifier.PUBLIC );
	}
	static <T> T def(String name, T value) {
		return Util.get(PREF_PROPS + name, value);
	}
	private static void printStartParameters() {
		String p = "\n" + StringUtil.fixString(79, '=') + "\n";
		String s = AutoTestGenerator.class.getSimpleName() + "(PREFIX: " + PREF_PROPS + ") started with:"
				+ "\n\tfilename pattern       : " + fileName
				+ "\n\tclean                  : " + def("clean", false)
				+ "\n\tduplication            : " + def("duplication", 10)
				+ "\n\tfilter                 : " + def("filter", Util.FRAMEWORK_PACKAGE)
				+ "\n\tmodifier               : " + def("modifier", -1)
				+ "\n\tfilter.unsuccessful    : " + def("filter.unsuccessful", true)
				+ "\n\tfilter.complextypes    : " + def("filter.complextypes", true)
				+ "\n\tfilter.failing         : " + def("filter.failing", false)
				+ "\n\tfilter.nullresults     : " + def("filter.nullresults", false);
		AFunctionTester.log(p + s +p);
	}
	@SuppressWarnings("rawtypes")
	public static Collection<? extends AFunctionTester> createExpectationTesters() {
		printStartParameters();
		int duplication = def("duplication", 10);
		Collection<AFunctionTester> testers = new LinkedList<>();
		boolean fileexists = true;
		for (int i=0; i<duplication; i++) {
			if (!AutoTestGenerator.getFile(i).exists() || def("clean", false)) {
				fileexists = false;
				AutoTestGenerator.generateExpectations(i, 
						def("filter", Util.FRAMEWORK_PACKAGE), 
						def("modifier", -1) );
			}
			if (fileexists || count > 0)
				testers.addAll(AutoTestGenerator.readExpectations(i));
		}
		if (def("filter.unsuccessful", true))
			load_unsuccessful = FunctionCheck.filterFailingTest(testers);
		printStatistics(duplication +1, testers);
		return testers;
	}

	static void generateExpectations(int iteration, String fuzzyfilter, int modifiers) {
		LogFactory.setPrintToConsole(false);
		count = 0;
		Map<Double, Method> methods = ClassFinder.self().fuzzyFind(fuzzyfilter, Method.class, modifiers, null);
		methods_loaded = methods.size();
		String p = "\n" + StringUtil.fixString(79, '~') + "\n";
		AFunctionCaller.log(p + "calling " + methods.size() + " methods to create expectations -> " + getFile(iteration) + p);

		methods.values().forEach(m -> writeExpectation(new AFunctionCaller(iteration, m)));
		
		ConcurrentUtil.sleep(1000);
		if (count > 0)
			AFunctionCaller.log(new String(FileUtil.getFileBytes(getFile(iteration).getPath(), null)));
		AFunctionCaller.log(p + count + " expectations written into '" + getFile(iteration) + p);
	}

	static File getFile(int iteration) {
		return FileUtil.userDirFile(fileName + iteration + ".txt");
	}

	private static void writeExpectation(AFunctionCaller f) {
		String then = null;
		try {
			if ((def("filter.complextypes", true) && !FunctionCheck.hasSimpleTypes(f)) 
					|| f.source.getParameterCount() == 0) {
				filter_complextypes++;
				return;
			}
			f.run();
		} catch (Throwable e) {
			if (!f.status.is(StatusTyp.INITIALIZED) || def("filter.failing", false)) {
				filter_errors++;
				return;
			}
			fails++;
			then = "fail(" + e.getClass().getName() + "(" + e.getMessage() + "))";
		}
		if (then == null) {
			if (f.getResult() == null) {
				if (def("filter.nullresults", false)) {
					filter_nullresults++;
					return;
				} else {
					then = "null";
				}
			}
		}
		if (f.getResult() != null && !FunctionCheck.checkTypeConversion(f.getResult())) {
			filter_typeconversions++;
			return;
		}
		String expect = ExpectationCreator.createExpectationString(f, then);
		if (def("filter.unsuccessful", true) && !FunctionCheck.checkTestSuccessful(f, expect)) {
			filter_unsuccessful++;
			return;
		}
		count++;
		FileUtil.writeBytes(expect.getBytes(), getFile(f.cloneIndex).getPath(), true);
	}

	public static Collection<ExpectationFunctionTester> readExpectations(int iteration) {
		return readExpectations(iteration, getFile(iteration));
	}
	
	static Collection<ExpectationFunctionTester> readExpectations(int iteration, File file) {
		LinkedHashSet<ExpectationFunctionTester> expTesters = new LinkedHashSet<>();
		Scanner sc = Util.trY( () ->new Scanner(file));
		Expectations exp = null;
		Method method = null;
		while (sc.hasNextLine()) {
			String l = sc.nextLine();
			if (exp == null && !l.startsWith("@") )
				continue;
			if (exp == null) {
				exp = ExpectationCreator.createExpectationFromLine(l);
			} else {
				if (l.matches("\\w+.*\\(.*\\)")) {
					method = ExpectationCreator.extractMethod(l);
					if (method != null)
						expTesters.add(new ExpectationFunctionTester(iteration, method, exp));
					else
						load_method_error++;
				} else {
					load_method_error++;
					AFunctionCaller.log("ERROR: method-format for " + exp + "\n");
				}
				exp = null;
			}
		}
		return expTesters;
	}

	private static void printStatistics(int iterations, Collection<AFunctionTester> testers) {
		String p = "\n" + StringUtil.fixString(79, '=') + "\n";
		String s = AutoTestGenerator.class.getSimpleName() + " created " + count + " expectations in file pattern: '" + fileName + "...'"
				+ "\n\ttestneverfail         : " + def("testneverfail", false)
				+ "\n\tmethods loaded        : " + methods_loaded
				+ "\n\tduplications          : " + def("duplication", 10)
				+ "\n\tcreated with fail     : " + fails
				+ "\n\tcreated with null     : " + nullresults
				+ "\n\tcreated totally       : " + count
				+ "\n\tfiltered type error   : " + filter_typeconversions
				+ "\n\tfiltered complex types: " + filter_complextypes
				+ "\n\tfiltered errors       : " + filter_errors
				+ "\n\tfiltered nulls        : " + filter_nullresults
				+ "\n\tfiltered unsuccessful : " + filter_unsuccessful
				+ "\n\tload errors           : " + load_method_error
				+ "\n\tloaded unsuccessful   : " + load_unsuccessful
				+ "\n\ttotally loaded        : " + testers.size();
		AFunctionTester.log(p + s +p);
	}
}

class ExpectationCreator {
	private static final String PREF_WHEN = "@" + Expectations.class.getSimpleName() + "({@" + Expect.class.getSimpleName() + "( when = {";
	private static final String PREF_THEN = "then = \"";
	
	static String createExpectationString(AFunctionCaller f, String then) {
		String expect = "\n@" + Expectations.class.getSimpleName() + "({@" + Expect.class.getSimpleName() 
				+ "( when = " + Util.toJson(f.getParameter()) 
				+ " then = \"" + (then != null || f.getResult() == null ? then : (ObjectUtil.isSingleValueType(f.getResult().getClass()) ? FormatUtil.format(f.getResult()) : Util.toJson(f.getResult())) + "\"})\n")
				+ f.source + "\n\n";
		expect = expect.replace("]}", "}");
		return expect;
	}

	static Expectations createExpectationFromLine(String l) {
		String when[], whenall, then;
		whenall = StringUtil.substring(l, PREF_WHEN, "}");
		when = whenall.replaceAll("\"", "").split(",");
		then = StringUtil.substring(l, PREF_THEN, "\"", 0, true);
		return ExpectationCreator.createExpectation(when, then);
	}

	static ExpectationsImpl createExpectation(String[] when, String then) {
		return new ExpectationsImpl(new ExpectImpl(-1, null, when, StringUtil.toString(then), -1));
	}

	@SuppressWarnings("unchecked")
	static Method extractMethod(String m) {
		String name = StringUtil.substring(m, " ", "(", false);
		name = StringUtil.substring(name, " ", "(", true);
		String methodName = StringUtil.substring(name, ".", null, true);
		String clsName = StringUtil.substring(name, null, "." + methodName);
		String pars = StringUtil.substring(m, "(", ")");
		try {
			return BeanClass.load(clsName).getMethod(methodName, createParameterTypes(pars.split("[,]")));
		} catch (Exception e) {
			AFunctionTester.log(e.toString() + "\n");
			return null;
		}
	}

	@SuppressWarnings("rawtypes")
	static Class[] createParameterTypes(String[] pars) {
		Class[] types = new Class[pars.length];
		for (int i = 0; i < types.length; i++) {
			types[i] = pars[i].contains("[]") 
					? BeanClass.loadArrayClass(pars[i]) 
					: pars[i].contains(".")
						? BeanClass.load(pars[i]) 
						: PrimitiveUtil.getPrimitiveClass(pars[i]);
		}
		return types;
	}
}

class FunctionCheck {
	static boolean checkTestSuccessful(AFunctionCaller t, String expect) {
		try {
			new ExpectationFunctionTester(t.source, ExpectationCreator.createExpectationFromLine(expect)).testMe();
			return true;
		} catch (Throwable e) {
			return false;
		}
	}

	static boolean checkTypeConversion(Object result) {
		String strResult = FormatUtil.format(result);
		Object recreatedResult = null;
		try {
			recreatedResult = ObjectUtil.wrap(strResult, result.getClass());
			return AFunctionTester.best(result).equals(AFunctionTester.best(recreatedResult));
		} catch (Exception e) {
			return false;
		}
	}

	static boolean hasSimpleTypes(AFunctionCaller f) {
		if (!isSimpleType(f.source.getReturnType()) || void.class.isAssignableFrom(f.source.getReturnType()))
			return false;
		return Arrays.stream(f.source.getParameterTypes()).allMatch(t -> isSimpleType(t));
	}

	static boolean isSimpleType(Class<?> t) {
		return ObjectUtil.isStandardType(t) && ObjectUtil.isSingleValueType(t);
	}
	
	@SuppressWarnings("rawtypes")
	static int filterFailingTest(Collection<AFunctionTester> testers) {
		LinkedList<AFunctionTester> failing = new LinkedList<>();
		for (AFunctionTester t : testers) {
			try {
				t.testMe();
			} catch (Throwable e) {
				failing.add(t);
			}
		}
		testers.removeAll(failing);
		return failing.size();
	}
}

class ExpectationsImpl implements Expectations {

	private Expect[] value;

	public ExpectationsImpl(Expect... value) {
		this.value = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Expectations.class;
	}

	@Override
	public Expect[] value() {
		return value;
	}
	
	@Override
	public String toString() {
		return Util.toJson(this);
	}
	
}
class ExpectImpl implements Expect {

	private int parIndex;
	private String whenPar;
	private String[] when;
	private String then;
	private int resultIndex;

	public ExpectImpl(int parIndex, String whenPar, String[] when, String then, int resultIndex) {
		super();
		this.parIndex = parIndex;
		this.whenPar = whenPar;
		this.when = when;
		this.then = then;
		this.resultIndex = resultIndex;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Expect.class;
	}

	@Override
	public int parIndex() {
		return parIndex;
	}

	@Override
	public String whenPar() {
		return whenPar;
	}

	@Override
	public String[] when() {
		return when;
	}

	@Override
	public String then() {
		return then;
	}

	@Override
	public int resultIndex() {
		return resultIndex;
	}

	@Override
	public String toString() {
		return Util.toJson(this);
	}
}