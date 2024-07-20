/*
 * created by: Tom
 * created on: 31.03.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.autotest.creator;

import static de.tsl2.nano.autotest.creator.AFunctionCaller.def;
import static de.tsl2.nano.autotest.creator.AutoTest.CHECK_TYPECONVERSION;
import static de.tsl2.nano.autotest.creator.AutoTest.CLEAN;
import static de.tsl2.nano.autotest.creator.AutoTest.DONTTEST;
import static de.tsl2.nano.autotest.creator.AutoTest.DUPLICATION;
import static de.tsl2.nano.autotest.creator.AutoTest.FAST_CLASSSCAN;
import static de.tsl2.nano.autotest.creator.AutoTest.FILENAME;
import static de.tsl2.nano.autotest.creator.AutoTest.FILTER;
import static de.tsl2.nano.autotest.creator.AutoTest.FILTER_COMPLEXTYPES;
import static de.tsl2.nano.autotest.creator.AutoTest.FILTER_ERROR_TYPES;
import static de.tsl2.nano.autotest.creator.AutoTest.FILTER_EXCLUDE;
import static de.tsl2.nano.autotest.creator.AutoTest.FILTER_FAILING;
import static de.tsl2.nano.autotest.creator.AutoTest.FILTER_NONINSTANCEABLES;
import static de.tsl2.nano.autotest.creator.AutoTest.FILTER_NULLRESULTS;
import static de.tsl2.nano.autotest.creator.AutoTest.FILTER_SINGELTONS;
import static de.tsl2.nano.autotest.creator.AutoTest.FILTER_TEST;
import static de.tsl2.nano.autotest.creator.AutoTest.FILTER_UNSUCCESSFUL;
import static de.tsl2.nano.autotest.creator.AutoTest.FILTER_VOID_PARAMETER;
import static de.tsl2.nano.autotest.creator.AutoTest.FILTER_VOID_RETURN;
import static de.tsl2.nano.autotest.creator.AutoTest.MAX_LINE_LENGTH;
import static de.tsl2.nano.autotest.creator.AutoTest.MODIFIER;
import static de.tsl2.nano.autotest.creator.AutoTest.PARALLEL;
import static de.tsl2.nano.autotest.creator.AutoTest.PRECHECK_TWICE;
import static de.tsl2.nano.autotest.creator.AutoTest.TESTNEVERFAIL;
import static org.junit.Assert.assertArrayEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import de.tsl2.nano.autotest.ValueRandomizer;
import de.tsl2.nano.core.IPreferences;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.ClassFinder;
import de.tsl2.nano.core.exception.ExceptionHandler;
import de.tsl2.nano.core.execution.ProgressBar;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.FormatUtil;
import de.tsl2.nano.core.util.MethodUtil;
import de.tsl2.nano.core.util.NumberUtil;
import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.core.util.parser.JSon;

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
 * 
 * Usage:
 * 	call createExpectationTesters() in your junit test with @parameterized annotation.
 * 
 * 
 * @author ts
 */
public class AutoTestGenerator {
	private static final String REGEX_UNMATCH = "XXXXXXXX";
	static String fileName = def(FILENAME, String.class).replace("autotest/", "");
	AtomicInteger methods_loaded = new AtomicInteger();
	List<Integer> counts = Collections.synchronizedList(new ArrayList<>(def(DUPLICATION, int.class)));
	AtomicInteger fails = new AtomicInteger();
	AtomicInteger nullresults = new AtomicInteger();
	AtomicInteger load_method_error = new AtomicInteger();
	AtomicInteger load_unsuccessful = new AtomicInteger();
	AtomicInteger filter_typeconversions = new AtomicInteger();
	AtomicInteger filter_errors = new AtomicInteger();
	AtomicInteger filter_unsuccessful = new AtomicInteger();
	AtomicInteger filter_nullresults = new AtomicInteger();
	AtomicInteger filter_complextypes = new AtomicInteger();
	private Statistics statistics = new Statistics();
	
	private AtomicReference<BufferedWriter> filteredFunctionWriter = new AtomicReference<BufferedWriter>();
	private ExceptionHandler uncaughtExceptionHandler = new ExceptionHandler();
	static ProgressBar progress;
	private AFunctionCaller maxDurationFct, maxMemUsageFct;
	private long start;
	
	public static void main(String[] args) {
		if (args.length == 0) {
			log("Please provide a comma-separated list of full classnames to be loaded!");
			return;
		}
		String[] clsNames = args[0].split("\\s*,\\s*");
		Arrays.stream(clsNames).forEach(c -> ClassFinder.getClassesInPackage(BeanClass.load(c).getPackage().getName(), null));
		new AutoTestGenerator().createExpectationTesters();
	}
	private void printStartParameters() {
		Util.trY( () -> FileUtil.writeBytes((IPreferences.printInfo(AutoTest.class)).getBytes(), getTimedFileName() + "statistics.txt", false), false);
	}
	public String getTimedFileName() {
		return fileName + DateUtil.getShortTimestamp(start) + "-";
	}
	@SuppressWarnings("rawtypes")
	public Collection<? extends AFunctionTester> createExpectationTesters() {
		if (def(DONTTEST, false)) {
			log("\n##############################################");
			log(" donttest=true ==> Leaving AutoTestGenerator!");
			log("##############################################");
			return new LinkedList<>();
		}
		start = System.currentTimeMillis();
		def("timefilename", getTimedFileName()); //provide timed file name for other instances
		int duplication = def(DUPLICATION, 10);
		for (int i = 0; i < duplication; i++)
			counts.add(0);
		Collection<AFunctionTester> testers = Collections.synchronizedList(new LinkedList<>());
		try {
			printStartParameters();
			FileUtil.delete(fileName + "initialization-error.txt");
			List<Method> methods = getMethods();
			progress = new ProgressBar(methods.size() * duplication);
			ArrayList<Integer> dupList = NumberUtil.numbers(duplication);
			Util.stream(dupList, def(PARALLEL, false)).forEach(i -> {
				Thread.currentThread().setUncaughtExceptionHandler(uncaughtExceptionHandler );
				if (!getFile(i).exists() || def(CLEAN, false)) {
					generateExpectations(i, methods);
				}
			});
			Util.stream(dupList, def(PARALLEL, false)).forEach(i -> {
				Thread.currentThread().setUncaughtExceptionHandler(uncaughtExceptionHandler );
				if (getFile(i).exists() || counts.get(i) > 0)
					testers.addAll(readExpectations(i));
				
			});
			if (def(FILTER_UNSUCCESSFUL, true))
				load_unsuccessful.addAndGet(FunctionCheck.filterFailingTest(testers, fileName));
			return testers;
		} catch (Throwable e) {
			Util.trY( () -> ManagedException.writeError(e, fileName + "initialization-error.txt"), false);
			ConcurrentUtil.sleep(3000);
			log("JUNIT TEST PARAMETERS FAILING:");
			e.printStackTrace();
			ManagedException.forward(e);
			return null;
		} finally {
			if (statistics.statuss.isEmpty()) // -> no generation but only reading
				testers.forEach( t -> statistics.add(t));
			printStatistics(duplication +1, testers, statistics.getInfo(22));
			if (filteredFunctionWriter.get() != null) {
				Util.trY(() -> filteredFunctionWriter.get().close(), false);
				filteredFunctionWriter.set(null); // to avoid access with additional flush on parallel use
			}
			if (uncaughtExceptionHandler.hasExceptions()) {
				Util.trY( () -> FileUtil.writeBytes(uncaughtExceptionHandler.toString().getBytes(), getTimedFileName() + "uncaught-exceptions.txt", false), false);
//				throw new IllegalStateException(uncaughtExceptionHandler.toString());
			}
			if (progress != null)
				progress.setFinished();
		}
	}

	private List<Method> getMethods() throws IOException {
		prepareFilteredWriter();
		List<Method> methods;
		if (def(FAST_CLASSSCAN, true)) {
			methods = ClassFinder.self().findMethods(def(FILTER, ""), def(MODIFIER, -1), null);
		} else {
			methods = ClassFinder.self().find(def(FILTER, ""), Method.class, def(MODIFIER, -1), null);
		}
		FileUtil.writeBytes(("\nmatching methods in classpath: " + methods.size()).getBytes(),
				getTimedFileName() + "statistics.txt", true);
		filterMethods(methods);
		return methods;
	}
	private void filterMethods(List<Method> methods) {
		int filterExcludes = filterExcludes(methods);
		int filterTestClasses = filterTestClasses(methods);
		int filterSingeltons = filterSingeltons(methods);
		int filterNonInstanceable = filterNonInstanceable(methods);
		FileUtil.writeBytes(("\nfiltered methods             : " 
			+ methods.size() + " (" 
			+ FILTER_EXCLUDE + "->" + filterExcludes + " " 
			+ FILTER_TEST + "->" + filterTestClasses + " " 
			+ FILTER_SINGELTONS + "->" + filterSingeltons + " " 
			+ FILTER_NONINSTANCEABLES + "->" + filterNonInstanceable + " " 
			+ ")").getBytes(), getTimedFileName() + "statistics.txt", true);
	}
	private void prepareFilteredWriter() throws IOException {
		if (!getFile(0).exists() || def(CLEAN, false))
			FileUtil.delete(fileName + "filtered.txt");
		filteredFunctionWriter.set(FileUtil.getBAWriter(fileName + "filtered.txt"));
	}

	private static int filter(List<Method> methods, Predicate<Method> myFilter) {
		int size = methods.size();
		methods.removeIf(myFilter);
		return size - methods.size();
	}
	private static int filterExcludes(List<Method> methods) {
		return filter(methods, m -> m.toGenericString().matches(def(FILTER_EXCLUDE, REGEX_UNMATCH)));
	}
	private static int filterTestClasses(List<Method> methods) {
		return filter(methods, m -> m.getDeclaringClass().getName().matches(def(FILTER_TEST, ".*(Test|IT)")));
	}
	private static int filterSingeltons(List<Method> methods) {
		if (def(FILTER_SINGELTONS, true))
			return filter(methods, m -> BeanClass.getBeanClass(m.getDeclaringClass()).isSingleton());
		return 0;
	}
	private static int filterNonInstanceable(List<Method> methods) {
		if (def(FILTER_NONINSTANCEABLES, true))
			return filter(methods, m -> !BeanClass.isStatic(m) && !Util.isInstanceable((m.getDeclaringClass())));
		return 0;
	}
	private static boolean filterErrorType(Throwable e) {
		return ManagedException.getRootCause(e).toString().matches(def(FILTER_ERROR_TYPES, REGEX_UNMATCH));
	}
	void generateExpectations(int iteration, List<Method> methods) {
		LogFactory.setPrintToConsole(false);
		String p = "\n" + StringUtil.fixString(79, '~') + "\n";
		try (BufferedWriter writer = FileUtil.getBAWriter(getFile(iteration).getPath())) {
			methods_loaded.set(methods.size());
			log(p + "calling " + methods.size() + " methods to create expectations -> " + getFile(iteration) + p);
			AtomicReference<BufferedWriter> refWriter = new AtomicReference<>(writer);
			Util.stream(methods, def(PARALLEL, false)).forEach(m -> writeExpectation(new AFunctionCaller(iteration, m), refWriter));
		} catch (Exception e1) {
			ManagedException.forward(e1);
		} finally {
			if (filteredFunctionWriter != null)
				Util.trY( () ->filteredFunctionWriter.get().flush(), false);
			ConcurrentUtil.sleep(200);
			// if (count > 0)
			// 	log(new String(FileUtil.getFileBytes(getFile(iteration).getPath(), null)));
			log(p + counts.get(iteration) + " expectations written into '" + getFile(iteration) + p);
		}
	}

	File getFile(int iteration) {
		return FileUtil.userDirFile(fileName + iteration + ".txt");
	}

	private void writeExpectation(AFunctionCaller f, AtomicReference<BufferedWriter> writer) {
		Thread.currentThread().setUncaughtExceptionHandler(uncaughtExceptionHandler );
		try {
			log("writeExpectation: " + f.cloneIndex + ": " + f.getFunctionDescription(), progress);
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
					filter_complextypes.incrementAndGet();
					return;
				}
				f.runWithTimeout();
			} catch (Exception | AssertionError e) {
				if (f.status.in(StatusTyp.NEW) || f.status.isFatal() || def(FILTER_FAILING, false) || filterErrorType(e)) {
					writeFilteredFunctionCall(f);
					filter_errors.incrementAndGet();
					return;
				}
				fails.incrementAndGet();
				then = "fail(" + AFunctionTester.getErrorMsg(e) + ")";
			}
			if (then == null) {
				if (f.getResult() == null) {
					if (def(FILTER_NULLRESULTS, false)) {
						filter_nullresults.incrementAndGet();
						writeFilteredFunctionCall(f);
						return;
					} else {
						then = "null";
					}
				}
			}
			if (f.getResult() != null && def(CHECK_TYPECONVERSION, false)
					&& !FunctionCheck.checkTypeConversion(f.getResult())) {
				writeFilteredFunctionCall(f);
				filter_typeconversions.incrementAndGet();
				return;
			}
			String expect = ExpectationCreator.createExpectationString(f, then);
			if (expect == null) {
				writeFilteredFunctionCall(f);
				filter_typeconversions.incrementAndGet();
				return;
			}
			Status testStatus;
			if (def(FILTER_UNSUCCESSFUL, true) && (testStatus = FunctionCheck.checkTestSuccessful(f, expect)) != null) {
				f.status = testStatus;
				writeFilteredFunctionCall("STATUS: " + f.status + expect);
				filter_unsuccessful.incrementAndGet();
				return;
			}
			counts.set(f.cloneIndex, counts.get(f.cloneIndex) + 1);
			maxDurationFct = maxDurationFct == null || maxDurationFct.duration < f.duration ? f : maxDurationFct; 
			maxMemUsageFct = maxMemUsageFct == null || maxMemUsageFct.memusage < f.memusage ? f : maxMemUsageFct; 
			Util.trY(() -> writer.get().append(expect));
		} finally {
			statistics.add(f);
		}
	}

	private void writeFilteredFunctionCall(AFunctionCaller f) {
		String fct;
		try {
			fct = f.getFunctionDescription();
			if (fct.length() > def(MAX_LINE_LENGTH, Integer.class)) {
				log(f.getFunctionDescription() + " with to long parameter json string: " + fct.length());
				fct = fct.substring(0, def(MAX_LINE_LENGTH, Integer.class)) + "...";
			}
		} catch (Throwable e) {
			e.printStackTrace();
			fct = f.cloneIndex + ":" + f.getFunctionDescription()
					+ " caused error on AFunctionCaller.toString() on writingFilterFunctinoCall(): " + e.getMessage();

		}
		writeFilteredFunctionCall(fct + "\n");
	}
	private void writeFilteredFunctionCall(String call) {
		Util.trY(() -> filteredFunctionWriter.get().append(call), false);
	}
	public Collection<ExpectationFunctionTester> readExpectations(int iteration) {
		return readExpectations(iteration, getFile(iteration));
	}
	
	Collection<ExpectationFunctionTester> readExpectations(int iteration, File file) {
		log("\nREADING " + counts.get(iteration) + " EXPECTATIONS FROM " + file.getPath() + "...\n");
		LinkedHashSet<ExpectationFunctionTester> expTesters = new LinkedHashSet<>();
		Scanner sc = Util.trY( () ->new Scanner(file));
		ProgressBar progress = new ProgressBar(
				(int) (counts.get(iteration) > 0 ? counts.get(iteration) : file.length() / 1000));
		Expectations exp = null;
		Method method = null;
		while (sc.hasNextLine()) {
			String l = sc.nextLine().trim();
			if (l.length() == 0 || l.startsWith("#") || (exp == null && !l.startsWith("@")))
				continue;
			if (exp == null) {
				exp = ExpectationCreator.createExpectationFromLine(l);
			} else {
				if (l.matches(MethodUtil.REGEX_FULL_METHOD_EXPRESSION)) {
					method = ExpectationCreator.extractMethod(l);
					progress.increase(" " + iteration + ": " + (method != null
							? " " + method.getDeclaringClass().getSimpleName() + "." + method.getName()
							: " ..."));
					if (method != null)
						expTesters.add(new ExpectationFunctionTester(iteration, method, exp));
				} else {
					load_method_error.incrementAndGet();
					log("ERROR: method-format for " + StringUtil.toString(exp, 120) + " -> " + l + "\n");
				}
				exp = null;
			}
		}
		log("\nEXPECTATION READING ON ITERATION " + iteration + " FINSIHED!\n");
		return expTesters;
	}

	private static void log(Object obj) {
		log(obj, null);
	}
	private static void log(Object obj, ProgressBar progress) {
		if (progress == null)
			AFunctionCaller.log(obj + "\n");
		else
//			synchronized (progress) {
				progress.increase(" " + obj.toString());
//			}
	}
	private void printStatistics(int iterations, Collection<AFunctionTester> testers, String groupByState) {
		String p = "\n" + StringUtil.fixString(79, '=') + "\n";
		Integer dup = def(DUPLICATION, 10);
		int count = counts.stream().reduce(0, Integer::sum);
		String s = AutoTestGenerator.class.getSimpleName() + " created " + count + " expectations in file pattern: '" + fileName + "...'"
				+ "\n\tend time              : " + DateUtil.getFormattedDateTime(new Date()) + "\tduration: " + DateUtil.getFormattedTime(System.currentTimeMillis() - start)
				+ "\n\ttestneverfail         : " + def(TESTNEVERFAIL, false)
				+ "\n\tclassfinder cls/mthds : " + ClassFinder.self().getLoadedClassCount() + " / " + ClassFinder.self().getLoadedMethodCount()
				+ "\n\tmethods loaded        : " + methods_loaded.get() + "\t(rate: "
				+ methods_loaded.get() / (float) ClassFinder.self().getLoadedMethodCount() + ")"
				+ "\n\tduplications          : " + dup + "\t(methods loaded * duplications: "
				+ methods_loaded.get() * dup + ")"
				+ "\nGENERATION PROCESS:"
				+ "\n\tcreated with fail     : " + fails.get()
				+ "\n\tcreated with null     : " + nullresults.get()
				+ "\n\tcreated totally       : " + count
				+ "\n\tfiltered type error   : " + filter_typeconversions.get()
				+ "\n\tfiltered complex types: " + filter_complextypes.get()
				+ "\n\tfiltered errors       : " + filter_errors.get()
				+ "\n\tfiltered nulls        : " + filter_nullresults.get()
				+ "\n\tmax duration          : " + (maxDurationFct != null ? maxDurationFct.duration + " msec\t\t<- "  + maxDurationFct.cloneIndex + ":" + maxDurationFct.getFunctionDescription() : "")
				+ "\n\tmax mem usage         : " + (maxMemUsageFct != null ? ByteUtil.amount(maxMemUsageFct.memusage) + "\t\t\t<- " + maxMemUsageFct.cloneIndex + ":" + maxMemUsageFct.getFunctionDescription() : "")
				+ "\n\tuncaught exceptions   : " + uncaughtExceptionHandler.getExceptions().size()
				+ groupByState
				+ "\nLOADING PROCESS (Unit Testing):"
				+ "\n\tfiltered unsuccessful : " + filter_unsuccessful.get()
				+ "\n\tload errors           : " + load_method_error.get()
				+ "\n\tloaded unsuccessful   : " + load_unsuccessful.get()
				+ "\n\ttotally loaded/tested : " + testers.size() + " (load-rate: "
				+ (testers.size() / (float) (dup + 1)) / (float) methods_loaded.get() + ", total-rate: "
				+ (testers.size() / (dup + 1)) / (float) ClassFinder.self().getLoadedMethodCount() + ")"
				+ "\n\n" + ValueRandomizer.getDependencyInjector();
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
	private static final String PREF_WHEN = "@" + Expectations.class.getSimpleName() + "({@"
			+ Expect.class.getSimpleName() + "( when = [";
	private static final String PREF_THEN = "then = \"-->";
	private static final String POST_WHEN = "] " + PREF_THEN;
	private static final String POST_THEN = "<--\"";
	private static final String PREF_CONSTRUCT_TYPES = "constructTypes = [";
	private static final String PREF_CONSTRUCT = "construct = [";
	private static final String POST_CONSTRUCT_TYPES = "] " + PREF_CONSTRUCT;
	private static final String POST_END = "])}";
	
	static String createExpectationString(AFunctionCaller f, String then) {
		try {
			if ((then == null || then.equals("null")) && void.class.isAssignableFrom(f.source.getReturnType())) {
				if (f.getParameter().length > 0 && f.getParameter()[0] != null)
					then = asString(f.getParameter()[0]); // see ExpectationTester.getResultIndex()
			}
			then = (then != null || f.getResult() == null ? then : asString(f.getResult()));
			String expect = "\n@" + Expectations.class.getSimpleName() + "({@" + Expect.class.getSimpleName() 
					+ "( when = " + Util.toJson(f.getParameter()) 
					+ " " + PREF_THEN + then + POST_THEN
					+ (f.construction != null && f.construction.parameter != null ? 
							" constructTypes = " + Util.toJson(f.getConstruction().constructor.getParameterTypes())
									+ " construct = " + Util.toJson(prepareParameter(f.getConstruction().parameter))
							: "")
					+ ")})\n" + f.source + "\n";
			return expect;
		} catch (Exception e) {
			f.status = new Status(StatusTyp.STORE_ERROR, null, e);
			return null;
		}
	}

	private static Object[] prepareParameter(Object[] parameter) {
		for (int i = 0; i < parameter.length; i++) {
			if (parameter[i] instanceof Class) {
				parameter[i] = ((Class) parameter[i]).getName();
			}
		}
		return parameter;
	}

	private static String asString(Object obj) {
		return AFunctionTester.convertMultilineString(ObjectUtil.isSingleValueType(obj.getClass()) ? FormatUtil.format(obj) : Util.toJson(obj));
	}

	static Expectations createExpectationFromLine(String l) {
		String when[], then, construct[];
		when = extractArray(l, PREF_WHEN, POST_WHEN);
		then = StringUtil.substring(l, PREF_THEN, POST_THEN, 0, true);
		Class[] constructTypes = loadClasses(extractArray(l, PREF_CONSTRUCT_TYPES, POST_CONSTRUCT_TYPES));
		construct = extractArray(l, PREF_CONSTRUCT, POST_END);
		return ExpectationCreator.createExpectation(when, then, constructTypes, construct);
	}

	private static Class[] loadClasses(String[] typenames) {
		if (typenames == null)
			return null;
		Class[] types = new Class[typenames.length];
		for (int i = 0; i < typenames.length; i++) {
			types[i] = ObjectUtil.loadClass(StringUtil.trim(typenames[i], "\"{}"));
		}
		return types;
	}

	private static String[] extractArray(String l, String prefix, String postfix) {
		String all = StringUtil.substring(l, prefix, postfix, 0, true);
		return all != null ? new JSon().splitArray("[" + all + "]") : null;
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
					: ObjectUtil.loadClass(pars[i]);
		}
		return types;
	}
}

class FunctionCheck {
	static Status checkTestSuccessful(AFunctionCaller t, String expect) {
		ExpectationFunctionTester tester = null;
		try {
			tester = new ExpectationFunctionTester(t.cloneIndex, t.source, ExpectationCreator.createExpectationFromLine(expect));
			if (def(CHECK_TYPECONVERSION, false))
				checkExpectationLoading(t, tester);
			tester.testMe();
			if (def(PRECHECK_TWICE, true))
				tester.testMe(); //do it twice, sometimes a value changes the first time. would be better to do the initial run() twice!
			t.status = tester.status;
			return null;
		} catch (Exception | AssertionError e) {
			return tester != null ? tester.status
					: new Status(StatusTyp.PARSING_ERROR, "tester couldn't be created in cause of parsing problems", e);
		}
	}

	private static void checkExpectationLoading(AFunctionCaller t, ExpectationFunctionTester tester) {
		try {
			if (t.getConstruction() != null && tester.getConstruction() != null)
				assertArrayEquals(t.getConstruction().parameter, tester.getConstruction().parameter);
		} catch (Exception e) {
			tester.status = new Status(StatusTyp.TYPECONVERSION_CHECK_FAIL, "expectation instance construction failed",
					e);
		}

		try {
			if (t.getParameter() != null)
				assertArrayEquals(t.getParameter(), tester.getParameter());
		} catch (Exception e) {
			tester.status = new Status(StatusTyp.TYPECONVERSION_CHECK_FAIL, "loading expectation failed", e);
		}
	}

	static boolean checkTypeConversion(Object result) {
		try {
			String strResult = FormatUtil.format(result);
			Object recreatedResult = null;
			recreatedResult = ObjectUtil.wrap(strResult, result.getClass());
			return Objects.deepEquals(AFunctionTester.best(result), AFunctionTester.best(recreatedResult));
		} catch (Throwable e) { //catch Throwable as it is possible that something like OutOfMemoryError occur
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
		Collection<AFunctionTester> failing = Collections.synchronizedCollection(new LinkedList<>());
		int size = testers.size();
		try ( BufferedWriter removedFunctionWriter = FileUtil.getBAWriter(filePrefix + "removed-functions.txt")) {
			AtomicReference<BufferedWriter> refWriter = new AtomicReference<>(removedFunctionWriter);
			Util.stream(testers, def(PARALLEL, false)).forEach( t -> {
				try {
					t.testMe();
				} catch (Throwable e) {
					failing.add(t);
					Util.trY( () -> refWriter.get().append(t.getID() + "\n"), false);
				}
			});
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