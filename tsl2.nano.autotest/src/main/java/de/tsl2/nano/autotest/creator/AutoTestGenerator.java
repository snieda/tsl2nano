package de.tsl2.nano.autotest.creator;

import static de.tsl2.nano.autotest.creator.AFunctionCaller.def;
import static de.tsl2.nano.autotest.creator.AutoTest.*;
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

import de.tsl2.nano.core.IPreferences;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.ClassFinder;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.exception.ExceptionHandler;
import de.tsl2.nano.core.execution.ProgressBar;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ByteUtil;
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
	private static final String REGEX_UNMATCH = "XXXXXXXX";
	static String fileName = def(FILENAME, "generated/generated-autotests-");
	int methods_loaded = 0;
	int count = 0;
	int fails = 0;
	int nullresults = 0;
	int load_method_error = 0;
	int load_unsuccessful = 0;
	int filter_typeconversions = 0;
	int filter_errors = 0;
	int filter_unsuccessful = 0;
	int filter_nullresults = 0;
	int filter_complextypes = 0;
	private Statistics statistics = new Statistics();
	
	private BufferedWriter filteredFunctionWriter;
	private ExceptionHandler uncaughtExceptionHandler = new ExceptionHandler();
	static ProgressBar progress;
	private AFunctionCaller maxDurationFct, maxMemUsageFct;
	private long start;
	
	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("Please provide a comma-separated list of full classnames to be loaded!");
			return;
		}
		String[] clsNames = args[0].split("\\s*,\\s*");
		Arrays.stream(clsNames).forEach(c -> ClassFinder.getClassesInPackage(BeanClass.load(c).getPackage().getName(), null));
		new AutoTestGenerator().createExpectationTesters();
	}
	private void printStartParameters() {
		FileUtil.writeBytes((IPreferences.printInfo(AutoTest.class)).getBytes(), getTimedFileName() + "statistics.txt", false);
	}
	public String getTimedFileName() {
		return fileName + DateUtil.getShortTimestamp(start) + "-";
	}
	@SuppressWarnings("rawtypes")
	public Collection<? extends AFunctionTester> createExpectationTesters() {
		if (def(DONTTEST, false)) {
			log("donttest=true ==> Leaving AutoTestGenerator!");
			return new LinkedList<>();
		}
		start = System.currentTimeMillis();
		def("timefilename", getTimedFileName()); //provide timed file name for other instances
		int duplication = def(DUPLICATION, 10);
		Collection<AFunctionTester> testers = Collections.synchronizedList(new LinkedList<>());
		try {
			printStartParameters();
			FileUtil.delete(fileName + "initialization-error.txt");
			prepareFilteredWriter();
			List<Method> methods;
			if (def(FAST_CLASSSCAN, true))
				methods = ClassFinder.self().findMethods(def(FILTER, ""), def(MODIFIER, -1), null);
			else
				methods = ClassFinder.self().find(def(FILTER, ""), Method.class, def(MODIFIER, -1), null);
			FileUtil.writeBytes(("\nmatching methods in classpath: " + methods.size()).getBytes(), getTimedFileName() + "statistics.txt", true);
			filterExcludes(methods);
			filterTestClasses(methods);
			filterSingeltons(methods);
			filterNonInstanceable(methods);
			FileUtil.writeBytes(("\nfiltered methods             : " + methods.size()).getBytes(), getTimedFileName() + "statistics.txt", true);
			progress = new ProgressBar(methods.size() * duplication);
			ArrayList<Integer> dupList = NumberUtil.numbers(duplication);
			AtomicBoolean fileexists = new AtomicBoolean(true);
			Util.stream(dupList, def(PARALLEL, false)).forEach( i -> 
			{
				Thread.currentThread().setUncaughtExceptionHandler(uncaughtExceptionHandler );
				if (!getFile(i).exists() || def(CLEAN, false)) {
					fileexists.set(false);
					generateExpectations(i, methods);
				}
				if (fileexists.get() || count > 0)
					testers.addAll(readExpectations(i));
				
			});
			if (def(FILTER_UNSUCCESSFUL, true))
				load_unsuccessful = FunctionCheck.filterFailingTest(testers, fileName);
			return testers;
		} catch (Throwable e) {
			ManagedException.writeError(e, fileName + "initialization-error.txt");
			ConcurrentUtil.sleep(3000);
			System.out.println("JUNIT TEST PARAMETERS FAILING:");
			e.printStackTrace();
			ManagedException.forward(e);
			return null;
		} finally {
			if (statistics.statuss.isEmpty()) // -> no generation but only reading
				testers.forEach( t -> statistics.add(t));
			printStatistics(duplication +1, testers, statistics.getInfo(22));
			if (filteredFunctionWriter != null) {
				Util.trY(() -> filteredFunctionWriter.close());
				filteredFunctionWriter = null; // to avoid access with additional flush on parallel use
			}
			if (uncaughtExceptionHandler.hasExceptions()) {
				FileUtil.writeBytes(uncaughtExceptionHandler.toString().getBytes(), getTimedFileName() + "uncaught-exceptions.txt", false);
//				throw new IllegalStateException(uncaughtExceptionHandler.toString());
			}
			if (progress != null)
				progress.setFinished();
		}
	}
	private void prepareFilteredWriter() throws IOException {
		if (!getFile(0).exists() || def(CLEAN, false))
			FileUtil.delete(fileName + "filtered.txt");
		filteredFunctionWriter = FileUtil.getBAWriter(fileName + "filtered.txt");
	}

	private static void filterExcludes(List<Method> methods) {
		methods.removeIf(m -> m.toGenericString().matches(def(FILTER_EXCLUDE, REGEX_UNMATCH)));
	}
	private static void filterTestClasses(List<Method> methods) {
		methods.removeIf(m -> m.getDeclaringClass().getName().matches(def(FILTER_TEST, ".*(Test|IT)")));
	}
	private static void filterSingeltons(List<Method> methods) {
		if (def(FILTER_SINGELTONS, true))
			methods.removeIf(m -> BeanClass.getBeanClass(m.getDeclaringClass()).isSingleton());
	}
	private static void filterNonInstanceable(List<Method> methods) {
		if (def(FILTER_NONINSTANCEABLES, true))
			methods.removeIf(m -> !Util.isInstanceable((m.getDeclaringClass())));
	}
	private static boolean filterErrorType(Throwable e) {
		return ManagedException.getRootCause(e).toString().matches(def(FILTER_ERROR_TYPES, REGEX_UNMATCH));
	}
	void generateExpectations(int iteration, List<Method> methods) {
		LogFactory.setPrintToConsole(false);
		BufferedWriter writer = Util.trY(() -> Files.newBufferedWriter(Paths.get(getFile(iteration).getPath()), CREATE, WRITE, APPEND));
		String p = "\n" + StringUtil.fixString(79, '~') + "\n";
		try {
			count = 0;
			methods_loaded = methods.size();
			log(p + "calling " + methods.size() + " methods to create expectations -> " + getFile(iteration) + p);
	
			Util.stream(methods, def(PARALLEL, false)).forEach(m -> writeExpectation(new AFunctionCaller(iteration, m), writer));
		} finally {
			if (filteredFunctionWriter != null)
				Util.trY( () ->filteredFunctionWriter.flush(), false);
			FileUtil.close(writer, true);
			ConcurrentUtil.sleep(200);
			if (count > 0)
				log(new String(FileUtil.getFileBytes(getFile(iteration).getPath(), null)));
			log(p + count + " expectations written into '" + getFile(iteration) + p);
		}
	}

	File getFile(int iteration) {
		return FileUtil.userDirFile(fileName + iteration + ".txt");
	}

	private void writeExpectation(AFunctionCaller f, BufferedWriter writer) {
		Thread.currentThread().setUncaughtExceptionHandler(uncaughtExceptionHandler );
		try {
			log("writeExpectation", progress);
			String then = null;
			try {
				if (f.source.isSynthetic() || f.source.isBridge() || f.source.getName().startsWith("$")) // -> e.g. $jacocoInit() -> javaagent code enhancing
					f.status = Status.FUNC_SYNTHETIC;
				if (def(FILTER_VOID_PARAMETER, false) && f.source.getParameterCount() == 0 && Modifier.isStatic(f.source.getModifiers()))
					f.status = Status.FUNC_WITHOUT_INPUT;
				else if (def(FILTER_VOID_RETURN, false) && void.class.isAssignableFrom(f.source.getReturnType()))
					f.status = Status.FUNC_WITHOUT_OUTPUT;
				else if (def(FILTER_COMPLEXTYPES, false) && !FunctionCheck.hasSimpleTypes(f))
					f.status = Status.FUNC_COMPLEX_INPUT;
			
				if (f.status.isRefused()) {
					writeFilteredFunctionCall(f);
					filter_complextypes++;
					return;
				}
				f.runWithTimeout();
			} catch (Exception | AssertionError e) {
				if (f.status.in(StatusTyp.NEW) || f.status.isFatal() || def(FILTER_FAILING, false) || filterErrorType(e)) {
					writeFilteredFunctionCall(f);
					filter_errors++;
					return;
				}
				fails++;
				then = "fail(" + AFunctionTester.getErrorMsg(e) + ")";
			}
			if (then == null) {
				if (f.getResult() == null) {
					if (def(FILTER_NULLRESULTS, false)) {
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
			if (def(FILTER_UNSUCCESSFUL, true) && (testStatus = FunctionCheck.checkTestSuccessful(f, expect)) != null) {
				f.status = testStatus;
				writeFilteredFunctionCall("STATUS: " + f.status + expect);
				filter_unsuccessful++;
				return;
			}
			count++;
			maxDurationFct = maxDurationFct == null || maxDurationFct.duration < f.duration ? f : maxDurationFct; 
			maxMemUsageFct = maxMemUsageFct == null || maxMemUsageFct.memusage < f.memusage ? f : maxMemUsageFct; 
			Util.trY(() -> writer.append(expect));
		} finally {
			statistics.add(f);
		}
	}

	private void writeFilteredFunctionCall(AFunctionCaller f) {
		writeFilteredFunctionCall(f.toString() + "\n");
	}
	private void writeFilteredFunctionCall(String call) {
		Util.trY(() -> filteredFunctionWriter.append(call));
//		FileUtil.writeBytes((call + "\n").getBytes(), fileName + "filtered.txt", true);
	}
	public Collection<ExpectationFunctionTester> readExpectations(int iteration) {
		return readExpectations(iteration, getFile(iteration));
	}
	
	Collection<ExpectationFunctionTester> readExpectations(int iteration, File file) {
		log("\nREADING " + count + " EXPECTATIONS FROM " + file.getPath() + "...\n");
		LinkedHashSet<ExpectationFunctionTester> expTesters = new LinkedHashSet<>();
		Scanner sc = Util.trY( () ->new Scanner(file));
		ProgressBar progress = new ProgressBar((int) (count > 0 ? count : file.length() / 450));
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
					progress.increase(method != null ? " " + method.getDeclaringClass().getSimpleName() + "." + method.getName() : "...");
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
		log("\nEXPECTATION READING FINSIHED!\n");
		return expTesters;
	}

	private static void log(Object obj) {
		log(obj, null);
	}
	private static void log(Object obj, ProgressBar progress) {
		if (progress == null)
			System.out.println(obj);
		else
			progress.increase(obj.toString());
	}
	private void printStatistics(int iterations, Collection<AFunctionTester> testers, String groupByState) {
		String p = "\n" + StringUtil.fixString(79, '=') + "\n";
		Integer dup = def(DUPLICATION, 10);
		String s = AutoTestGenerator.class.getSimpleName() + " created " + count + " expectations in file pattern: '" + fileName + "...'"
				+ "\n\tend time              : " + DateUtil.getFormattedDateTime(new Date()) + "\tduration: " + DateUtil.getFormattedTime(System.currentTimeMillis() - start)
				+ "\n\ttestneverfail         : " + def(TESTNEVERFAIL, false)
				+ "\n\tclassfinder cls/mthds : " + ClassFinder.self().getLoadedClassCount() + " / " + ClassFinder.self().getLoadedMethodCount()
				+ "\n\tmethods loaded        : " + methods_loaded + "\t(rate: " + methods_loaded / (float)ClassFinder.self().getLoadedMethodCount() + ")"
				+ "\n\tduplications          : " + dup + "\t(methods loaded * duplications: " + methods_loaded * dup + ")"
				+ "\nGENERATION PROCESS:"
				+ "\n\tcreated with fail     : " + fails
				+ "\n\tcreated with null     : " + nullresults
				+ "\n\tcreated totally       : " + count
				+ "\n\tfiltered type error   : " + filter_typeconversions
				+ "\n\tfiltered complex types: " + filter_complextypes
				+ "\n\tfiltered errors       : " + filter_errors
				+ "\n\tfiltered nulls        : " + filter_nullresults
				+ "\n\tmax duration          : " + (maxDurationFct != null ? maxDurationFct.duration + " msec\t\t<- "  + maxDurationFct.cloneIndex + ":" + maxDurationFct.getFunctionDescription() : "")
				+ "\n\tmax mem usage         : " + (maxMemUsageFct != null ? ByteUtil.amount(maxMemUsageFct.memusage) + "\t\t\t<- " + maxMemUsageFct.cloneIndex + ":" + maxMemUsageFct.getFunctionDescription() : "")
				+ groupByState
				+ "\nLOADING PROCESS:"
				+ "\n\tfiltered unsuccessful : " + filter_unsuccessful
				+ "\n\tload errors           : " + load_method_error
				+ "\n\tloaded unsuccessful   : " + load_unsuccessful
				+ "\n\ttotally loaded        : " + testers.size() + " (load-rate: " + (testers.size() / (float)dup) / (float)methods_loaded + ", total-rate: " 
												 + (testers.size() / dup) / (float)ClassFinder.self().getLoadedMethodCount()  + ")"
				;
		AFunctionTester.log(p + s +p);
		FileUtil.writeBytes((p + s + p).getBytes(), getTimedFileName() + "statistics.txt", true);
	}
}

class Statistics {
	Collection<Status> statuss = Collections.synchronizedCollection(new LinkedList<>());
	public void add(AFunctionCaller fc) { statuss.add(fc.status);}
	public String getInfo(int keyWidth) {
		int[] count = new int[StatusTyp.values().length];
		statuss.forEach(s -> ++count[s.typ.ordinal()]);
		
		final StringBuilder buf = new StringBuilder("\n\tGENERATED FUNCTION TESTERS GROUPED BY STATE:");
		for (int i = 0; i < count.length; i++) {
			buf.append("\n\t\t" + StringUtil.fixString(StatusTyp.values()[i], keyWidth) + ": " + count[i]);
		}
		buf.append("\n\t\t" + StringUtil.fixString("<<< TOTALLY >>>", keyWidth) + ": " + statuss.size());
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
					+ " then = \"" + then + "\""
					+ (f.construction != null && f.construction.parameter != null ? 
							" constructTypes = " + Util.toJson(f.getConstruction().constructor.getParameterTypes())
							+ " construct = " + Util.toJson(f.getConstruction().parameter) : "") 
					+ "})\n"
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
		all = StringUtil.substring(l, prefix, "} ", 0, true);
		if (!Util.isEmpty(all)) {
			String obj;
			do { // ugly, but the json from MapUtil creates some additional \" that we use to distinguish inner maps and arrays
				obj = StringUtil.substring(all, ",\"{\"", "}", true, true);
				if (obj != null) {
					String obj1 = obj.replaceAll("\"", "");
					all = all.replace("\"" + obj, obj1);
				}
			} while(obj != null);
			arr = all.split("\",\"");
			for (int i = 0; i < arr.length; i++) {
				arr[i] = arr[i].replaceAll("\"", "");
			}
		}
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
			tester = new ExpectationFunctionTester(t.cloneIndex, t.source, ExpectationCreator.createExpectationFromLine(expect));
			tester.testMe();
			if (def(PRECHECK_TWICE, true))
				tester.testMe(); //do it twice, sometimes a value changes the first time. would be better to do the initial run() twice!
			t.status = tester.status;
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
	static int filterFailingTest(Collection<AFunctionTester> testers, String filePrefix) {
		LinkedList<AFunctionTester> failing = new LinkedList<>();
		int size = testers.size();
		try ( BufferedWriter removedFunctionWriter = FileUtil.getBAWriter(filePrefix + "removed-functions.txt")) {
			for (AFunctionTester t : testers) {
				try {
					t.testMe();
				} catch (Throwable e) {
					failing.add(t);
					Util.trY( () -> removedFunctionWriter.write(t.getID() + "\n"));
				}
			}
			testers.removeAll(failing);
			if (testers.size() != size-failing.size())
				throw new IllegalStateException("failing testers were not removed from list - check your equals()!");
		} catch (IOException e1) {
			ManagedException.forward(e1);
		}
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