package de.tsl2.nano.autotest.creator;

import static de.tsl2.nano.autotest.creator.AFunctionCaller.def;
import static de.tsl2.nano.autotest.creator.AFunctionTester.PREF_PROPS;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.ClassFinder;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.exception.ExceptionHandler;
import de.tsl2.nano.core.execution.ProgressBar;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.FormatUtil;
import de.tsl2.nano.core.util.NumberUtil;
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
	static String fileName = def("filename", "generated/generated-autotests-");
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
	private static Statistics statistics = new Statistics();
	
	private static BufferedWriter filteredFunctionWriter;
	private static ExceptionHandler uncaughtExceptionHandler = new ExceptionHandler();
	private static ProgressBar progress;
	
	public static void main(String[] args) {
		List<Method> methods = ClassFinder.self().find(def("filter", "") , Method.class, def("modifier", -1), null);
		generateExpectations(0, methods );
	}
	private static void printStartParameters() {
		String p = "\n" + StringUtil.fixString(79, '=') + "\n";
		String s = AutoTestGenerator.class.getSimpleName() + "(PREFIX: " + PREF_PROPS + ") started with:"
				+ "\n\tuser.dir               : " + System.getProperty("user.dir")
				+ "\n\tuser.name              : " + System.getProperty("user.name")
				+ "\n\tstart time             : " + DateUtil.getFormattedDateTime(new Date())
				+ "\n\tfilename pattern       : " + fileName
				+ "\n\tfast.classscan         : " + def("fast.classscan", true)
				+ "\n\tclean                  : " + def("clean", false)
				+ "\n\tduplication            : " + def("duplication", 10)
				+ "\n\tparallel               : " + def("parallel", false)
				+ "\n\tfilter                 : " + def("filter", "")
				+ "\n\tmodifier               : " + def("modifier", -1)
				+ "\n\tfilter.test            : " + def("filter.test", ".*(Test|IT)")
				+ "\n\tfilter.exclude         : " + def("filter.exclude", "XXXXXXXX")
				+ "\n\tfilter.unsuccessful    : " + def("filter.unsuccessful", true)
				+ "\n\tfilter.voidparameter   : " + def("filter.voidparameter", false)
				+ "\n\tfilter.voidreturn      : " + def("filter.voidreturn", false)
				+ "\n\tfilter.complextypes    : " + def("filter.complextypes", false)
				+ "\n\tfilter.failing         : " + def("filter.failing", false)
				+ "\n\tfilter.nullresults     : " + def("filter.nullresults", false);
		AFunctionTester.log(p + s +p);
		FileUtil.writeBytes((p + s + p).getBytes(), fileName + "statistics.txt", false);
	}
	@SuppressWarnings("rawtypes")
	public static Collection<? extends AFunctionTester> createExpectationTesters() {
		int duplication = def("duplication", 10);
		Collection<AFunctionTester> testers = Collections.synchronizedList(new LinkedList<>());
		try {
			printStartParameters();
			FileUtil.delete(fileName + "initialization-error.txt");
			prepareFilteredWriter();
			List<Method> methods;
			if (def("fast.classscan", true))
				methods = ClassFinder.self().findMethods(def("filter", ""), def("modifier", -1), null);
			else
				methods = ClassFinder.self().find(def("filter", ""), Method.class, def("modifier", -1), null);
			filterExcludes(methods);
			filterTestClasses(methods);
			progress = new ProgressBar(methods.size() * duplication);
			ArrayList<Integer> dupList = NumberUtil.numbers(duplication);
			AtomicBoolean fileexists = new AtomicBoolean(true);
			Util.stream(dupList, def("parallel", false)).forEach( i -> 
			{
				Thread.currentThread().setUncaughtExceptionHandler(uncaughtExceptionHandler );
				if (!AutoTestGenerator.getFile(i).exists() || def("clean", false)) {
					fileexists.set(false);
					AutoTestGenerator.generateExpectations(i, methods);
				}
				if (fileexists.get() || count > 0)
					testers.addAll(AutoTestGenerator.readExpectations(i));
			});
			if (def("filter.unsuccessful", true))
				load_unsuccessful = FunctionCheck.filterFailingTest(testers);
			return testers;
		} catch (Exception e) {
			ManagedException.writeError(e, fileName + "initialization-error.txt");
			ConcurrentUtil.sleep(3000);
			System.out.println("JUNIT TEST PARAMETERS FAILING:");
			e.printStackTrace();
			ManagedException.forward(e);
			return null;
		} finally {
			printStatistics(duplication +1, testers, statistics.getInfo(22));
			Util.trY(() -> filteredFunctionWriter.close());
			if (uncaughtExceptionHandler.hasExceptions()) {
				FileUtil.writeBytes(uncaughtExceptionHandler.toString().getBytes(), fileName + "uncaught-exceptions.txt", false);
//				throw new IllegalStateException(uncaughtExceptionHandler.toString());
			}
		}
	}
	private static void prepareFilteredWriter() throws IOException {
		if (!AutoTestGenerator.getFile(0).exists() || def("clean", false))
			FileUtil.delete(fileName + "filtered.txt");
		File userDirFile = FileUtil.userDirFile(fileName + "filtered.txt");
		filteredFunctionWriter = Files.newBufferedWriter(Paths.get(userDirFile.getPath()), CREATE, WRITE, APPEND);
	}

	private static void filterExcludes(List<Method> methods) {
		methods.removeIf(m -> m.toGenericString().matches(def("filter.exclude", "XXXXXXXX")));
	}
	private static void filterTestClasses(List<Method> methods) {
		methods.removeIf(m -> m.getDeclaringClass().getName().matches(def("filter.test", ".*(Test|IT)")));
	}
	static void generateExpectations(int iteration, List<Method> methods) {
		LogFactory.setPrintToConsole(false);
		BufferedWriter writer = Util.trY(() -> Files.newBufferedWriter(Paths.get(getFile(iteration).getPath()), CREATE, WRITE, APPEND));
		try {
			count = 0;
			methods_loaded = methods.size();
			String p = "\n" + StringUtil.fixString(79, '~') + "\n";
			log(p + "calling " + methods.size() + " methods to create expectations -> " + getFile(iteration) + p);
	
			Util.stream(methods, def("parallel", false)).forEach(m -> writeExpectation(new AFunctionCaller(iteration, m), writer));
			
			ConcurrentUtil.sleep(200);
			if (count > 0)
				log(new String(FileUtil.getFileBytes(getFile(iteration).getPath(), null)));
			log(p + count + " expectations written into '" + getFile(iteration) + p);
		} finally {
			FileUtil.close(writer, true);
		}
	}

	static File getFile(int iteration) {
		return FileUtil.userDirFile(fileName + iteration + ".txt");
	}

	private static void writeExpectation(AFunctionCaller f, BufferedWriter writer) {
		Thread.currentThread().setUncaughtExceptionHandler(uncaughtExceptionHandler );
		statistics.add(f);
		log("writeExpectation", true);
		String then = null;
		try {
			if (def("filter.voidparameter", false) && f.source.getParameterCount() == 0 && Modifier.isStatic(f.source.getModifiers()))
				f.status = Status.FUNC_WITHOUT_INPUT;
			else if (def("filter.voidreturn", false) && void.class.isAssignableFrom(f.source.getReturnType()))
				f.status = Status.FUNC_WITHOUT_OUTPUT;
			else if (def("filter.complextypes", false) && !FunctionCheck.hasSimpleTypes(f))
				f.status = Status.FUNC_COMPLEX_INPUT;
		
			if (f.status.isRefused()) {
				writeFilteredFunctionCall(f);
				filter_complextypes++;
				return;
			}
			f.run();
		} catch (Exception | AssertionError e) {
			if (f.status.in(StatusTyp.NEW) || f.status.isError() || def("filter.failing", false)) {
				writeFilteredFunctionCall(f);
				filter_errors++;
				return;
			}
			fails++;
			then = "fail(" + e.getClass().getName() + "(" + e.getMessage().replace('\n', ' ') + "))";
		}
		if (then == null) {
			if (f.getResult() == null) {
				if (def("filter.nullresults", false)) {
					filter_nullresults++;
					writeFilteredFunctionCall(f);
					return;
				} else {
					then = "null";
				}
			}
		}
		if (f.getResult() != null && !FunctionCheck.checkTypeConversion(f.getResult())) {
			writeFilteredFunctionCall(f);
			filter_typeconversions++;
			return;
		}
		String expect = ExpectationCreator.createExpectationString(f, then);
		if (expect == null) {
			writeFilteredFunctionCall(f);
			filter_typeconversions++;
			return;
		}
		Status testStatus;
		if (def("filter.unsuccessful", true) && (testStatus = FunctionCheck.checkTestSuccessful(f, expect)) != null) {
			f.status = testStatus;
			writeFilteredFunctionCall(expect);
			filter_unsuccessful++;
			return;
		}
		count++;
		Util.trY(() -> writer.append(expect));
//		FileUtil.writeBytes(expect.getBytes(), getFile(f.cloneIndex).getPath(), true);
	}

	private static void writeFilteredFunctionCall(AFunctionCaller f) {
		writeFilteredFunctionCall(f.toString() + "\n");
	}
	private static void writeFilteredFunctionCall(String call) {
		Util.trY(() -> filteredFunctionWriter.append(call));
//		FileUtil.writeBytes((call + "\n").getBytes(), fileName + "filtered.txt", true);
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
					log("ERROR: method-format for " + exp + " -> " + l + "\n");
				}
				exp = null;
			}
		}
		return expTesters;
	}

	private static void log(Object obj) {
		log(obj, false);
	}
	private static void log(Object obj, boolean increase) {
		if (increase)
			System.out.println(obj);
		else
			progress.increase(obj.toString());
	}
	private static void printStatistics(int iterations, Collection<AFunctionTester> testers, String groupByState) {
		String p = "\n" + StringUtil.fixString(79, '=') + "\n";
		String s = AutoTestGenerator.class.getSimpleName() + " created " + count + " expectations in file pattern: '" + fileName + "...'"
				+ "\n\tend time              : " + DateUtil.getFormattedDateTime(new Date())
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
				+ "\n\ttotally loaded        : " + testers.size()
				+ groupByState;
		AFunctionTester.log(p + s +p);
		FileUtil.writeBytes((p + s + p).getBytes(), fileName + "statistics.txt", true);
	}
}

class Statistics {
	Collection<AFunctionCaller> caller = Collections.synchronizedCollection(new LinkedList<>());
	public void add(AFunctionCaller fc) { caller.add(fc);}
	public String getInfo(int keyWidth) {
		int[] count = new int[StatusTyp.values().length];
		caller.forEach(f -> ++count[f.status.typ.ordinal()]);
		
		final StringBuilder buf = new StringBuilder("\nFUNCTIONS GROUPED BY STATE:");
		for (int i = 0; i < count.length; i++) {
			buf.append("\n\t" + StringUtil.fixString(StatusTyp.values()[i], keyWidth) + ": " + count[i]);
		}
		buf.append("\n\t" + StringUtil.fixString("<<< TOTALLY >>>", keyWidth) + ": " + caller.size());
		return buf.toString();
	}
}

class ExpectationCreator {
	private static final String PREF_WHEN = "@" + Expectations.class.getSimpleName() + "({@" + Expect.class.getSimpleName() + "( when = {";
	private static final String PREF_THEN = "then = \"";
	private static final String PREF_CONSTRUCT = "construct = \"";
	private static final String PREF_CONSTRUCT_TYPES = "constructTypes = \"";
	
	static String createExpectationString(AFunctionCaller f, String then) {
		try {
			if ((then == null || then.equals("null")) && void.class.isAssignableFrom(f.source.getReturnType())) {
				if (f.getParameter().length > 0 && f.getParameter()[0] != null)
					then = asString(f.getParameter()[0]); // see ExpectationTester.getResultIndex()
			}
			then = (then != null || f.getResult() == null ? then : asString(f.getResult()));
			String expect = "\n@" + Expectations.class.getSimpleName() + "({@" + Expect.class.getSimpleName() 
					+ "( when = " + Util.toJson(f.getParameter()) 
					+ " then = \"" + then
					+ (f.construction != null && f.construction.parameter != null ? 
							" constructTypes = " + Util.toJson(f.getConstruction().constructor.getParameterTypes())
							+ " construct = " + Util.toJson(f.getConstruction().parameter) : "") 
					+ "\"})\n"
					+ f.source + "\n\n";
			expect = expect.replace("]}", "}");
			return expect;
		} catch (Exception e) {
			f.status = new Status(StatusTyp.STORE_ERROR, null, e);
			return null;
		}
	}

	private static String asString(Object obj) {
		return ObjectUtil.isSingleValueType(obj.getClass()) ? FormatUtil.format(obj) : Util.toJson(obj);
	}

	static Expectations createExpectationFromLine(String l) {
		String when[], then, construct[];
		when = extractArray(l, PREF_WHEN);
		then = StringUtil.substring(l, PREF_THEN, "\"", 0, true);
		then = then != null && then.startsWith("{") ? StringUtil.substring(l, PREF_THEN, "}\"", 0, true) : then;
		Class[] constructTypes = loadClasses(extractArray(l, PREF_CONSTRUCT_TYPES));
		construct = extractArray(l, PREF_CONSTRUCT);
		return ExpectationCreator.createExpectation(when, then, constructTypes, construct);
	}

	private static Class[] loadClasses(String[] typenames) {
		if (typenames == null)
			return null;
		Class[] types = new Class[typenames.length];
		for (int i = 0; i < typenames.length; i++) {
			types[i] = BeanClass.load(typenames[i]);
		}
		return types;
	}

	private static String[] extractArray(String l, String prefix) {
		String arr[] = null, all;
		all = StringUtil.substring(l, prefix, "}", 0, true);
		if (all != null)
			arr = all.replaceAll("\"", "").split(",");
		return arr;
	}

	static ExpectationsImpl createExpectation(String[] when, String then, Class[] constructTypes, String[] construct) {
		return new ExpectationsImpl(new ExpectImpl(-1, null, when, StringUtil.toString(then), -1, constructTypes, construct));
	}

	@SuppressWarnings("unchecked")
	static Method extractMethod(String m) {
		String name = StringUtil.substring(m, " ", "(", false);
		name = StringUtil.substring(name, " ", "(", true);
		String methodName = StringUtil.substring(name, ".", null, true);
		String clsName = StringUtil.substring(name, null, "." + methodName);
		String pars = StringUtil.substring(m, "(", ")");
		try {
			return BeanClass.load(clsName).getDeclaredMethod(methodName, Util.isEmpty(pars) ? new Class[0] : createParameterTypes(pars.split("[,]")));
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
	static Status checkTestSuccessful(AFunctionCaller t, String expect) {
		ExpectationFunctionTester tester = null;
		try {
			tester = new ExpectationFunctionTester(t.source, ExpectationCreator.createExpectationFromLine(expect));
			tester.testMe();
			tester.testMe(); //do it twice, sometimes a value changes the first time. would be better to do the initial run() twice!
			return null;
		} catch (Exception | AssertionError e) {
			return tester != null ? tester.status : new Status(StatusTyp.TEST_FAILED, null, e);
		}
	}

	static boolean checkTypeConversion(Object result) {
		try {
			String strResult = FormatUtil.format(result);
			Object recreatedResult = null;
			recreatedResult = ObjectUtil.wrap(strResult, result.getClass());
			return AFunctionTester.best(result).equals(AFunctionTester.best(recreatedResult));
		} catch (Exception e) {
			return false;
		}
	}

	static boolean hasSimpleTypes(AFunctionCaller f) {
		if (!isSimpleType(f.source.getReturnType()))
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
			} catch (Exception | AssertionError e) {
				failing.add(t);
			}
		}
		testers.removeAll(failing);
		return failing.size();
	}
}

// TODO: replace this implementation by using the AdapterProxy
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
	private String[] construct;
	private Class[] constructTypes;

	@SuppressWarnings("rawtypes")
	public ExpectImpl(int parIndex, String whenPar, String[] when, String then, int resultIndex, Class[] constructTypes, String[] construct) {
		super();
		this.parIndex = parIndex;
		this.whenPar = whenPar;
		this.when = when;
		this.then = then;
		this.resultIndex = resultIndex;
		this.constructTypes = constructTypes;
		this.construct = construct;
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
	public Class[] constructTypes() {
		return constructTypes;
	}
	@Override
	public String[] construct() {
		return construct;
	}
	@Override
	public String toString() {
		return Util.toJson(this);
	}
}