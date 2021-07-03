# Automatic Unit Test Creation

Thomas Schneider 04/2021

## Overview

maven artifact: *net.sf.tsl2nano:tsl2.nano.autotest:2.4.7*

This is a java unit test generation framework to do automated tests for you. If you don't want to know, how it works and only want to check, if it works for you, go to the last chapter *All together*

![unittest-result.png](doc/unittest-result.png)

## Introduction

Unit testing with a good code coverage is essential for modern software. test-driven development seems to be a productive way to implement new software. but unit tests are expensive. 
Developers are lazy - they always want to automate the things they have to do ;-)

Isn't there any way to automate some test creation? Why do we not declare expectations as annotations on our functions or methods to provide something like a specification for the implementation of the function? If we have a function - what's about the *inverse function*? Couldn't we declare an existing inverse function to be used by a test creation implementation?

But if we declare *Expections* or *Inverse functions*, we need a mechanism to fill method parameters randomly (or with expectation values). For complex objects , we need a test object implementation holding all java types as attributes. Further more we need a conversion framework to provide parameters in the right type.

The framework tsl2nano provides the base for the implementation. A parameterized Unit Test uses features like declared Expectations and Inverse Functions to do the tests. It is extendable to add more features with new annotions and tester-implemenations.

But, at the end I'm to lazy to create the Expectation Annoations by myself. Is there any way to let that create anyone for me?
Yes, the *AutoTestGenerator* can do that for you. It collects all available classes (may be filtered by you), finds out all
callable methods and tries to run them with randomized values. The result will be stored as Expectation Annoations in a file. The file will be read again, the expectations will be handled like they were created by you as method annotations. A pre-test checks, that the test won't fail. Now, a parametrized unit test *CurrentStatePreservationTest* runs all that tests.

It is up to you to copy/paste the generated annotations to the methods in your source code then the annotations would be found be the *AutoFunctionTest*. But only if you like ;-)

Using this autotest library on my tsl2 framwork with 30 maven modules and about 260.000 instructions I pushed my code coverage from 62% to 72% - and I found with that some bugs to be fixed :-)

Additionally, there are the following Implementations:

* _ValueRandomizer_ : is able to create random values of any java type
* _TypeBean_        : example implementation of a java bean holding all java types as bean attributes
* _BaseTest_        : base class for own unit tests providing expectations and textcomparisons (ignoring defined blocks of temporary text)
* _TextComparison_  : provides text comparisons with ignoring of defined regular expression blocks

### Constraints: What does it not

Methods on instances with complex constructors or with complex parameters may not work. They will be filtered.

If you have methods that return a value depending on a current system state, the results wont be stable and testable here.

Examples:

* your method returns the count of cached objects -> that will vary on each call
* your method returns something dependent on current system time, memory usage or file system state


## Code Review

The Test Creator (the class *AutoFunctionTest*) defines the features to be tested. Some properties like a *test name filter*, *test duplication* etc. can be set by system properties.

At the moment, we have two features with their tester implementations:

* InverseFunction -> InverseFunctionTester
* Expectations    -> ExpectationsTester

The *AutoTestGenerator* is able to generate unit tests for all methods in your classpath. Tests the state as is.

Two parametrized Unit Tests provide the real unit tests:

* *AutoFunctionTest*: will test all annotations of type _Expectation_ and _InverseFunction_
* CurrentStatePreservationTest*: uses *AutoTestGenerator* to create Expectation Tests for all found methods

## Usage of Test Annotations

First, you need a junit dependency on **junit:junit:4.12** or newer.

The annotations are packed into the tsl2nano core jar. So, you need a maven dependency for *tsl2.nano.core*. The *AutoFunctionTest* is included in the package *tsl2.nano.autotest*. Please add a maven dependency on scope *test*.

*tsl.nano.core*:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	<dependency>
		<groupId>net.sf.tsl2nano</groupId>
		<artifactId>tsl2.nano.core</artifactId>
		<version>2.4.7</version>
		<scope>test</scope>
	</dependency>
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

the test framework *tsl2.nano.autotest*:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	<dependency>
		<groupId>net.sf.tsl2nano</groupId>
		<artifactId>tsl2.nano.autotest</artifactId>
		<version>2.4.7</version>
		<scope>test</scope>
	</dependency>
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The Test Creator *AutoFunctionTest* should be activated by adding it to a Test Suite

The usage will be shown by some examples. For further informations, have a look at the code or the java doc.

### Expectations

looking at a specific parameter (through its index) , setting it with 'whenPar()' and checking it with 'then()'

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	@Expectations({@Expect(parIndex = 0, whenPar = "1.0", then = "2.0"), @Expect(parIndex = 0, whenPar = "2.0", then = "3.0")})
	public static Number increase(Number input) {
		return input.doubleValue() + 1;
	}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

looking at all method parameters:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	@Expectations({@Expect(when = {"1", "2", "3"}, then = "123")})
	public String concatNumbers(short first, int second, Long third) {
		return "" + first + second + third;
	}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

### InverseFunction

checking simple getter and setter:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	@InverseFunction(methodName = "setValue", parameters = { String.class })
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


having a simple mathematic function with its inverse function:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	@Expectations({@Expect(parIndex = 0, whenPar = "1.0", then = "2.0"), @Expect(parIndex = 0, whenPar = "2.0", then = "3.0")})
	public static Number increase(Number input) {
		return input.doubleValue() + 1;
	}

	@InverseFunction(methodName = "increase", parameters = { Number.class }, compareParameterIndex = 0)
	public static Number decrease(Number input) {
		return input.doubleValue() - 1;
	}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

writing/reading to something like a stream:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	static Map<String, Object> fileData;
	enum FileType {CSV, TABSHEET, MARKDOWN};

	public static void writeFile(Object data, FileType fileType, String path, String filename, boolean append) {
		fileData = MapUtil.asMap(path + filename + fileType, data);
	}
	@InverseFunction(methodName = "writeFile", 
			parameters = {Object.class, FileType.class, String.class, String.class, boolean.class}, 
			compareParameterIndex = 0, bindParameterIndexesOnInverse = {2, 3, 1})
	public static Object readFile(String path, String filename, FileType fileType) {
		return fileData.get(path + filename + fileType);
	}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


### ValueRandomizer

The value randomizer can create randomized values for all java types. 

Example with an parametrized test, creating a set of randomized values:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@RunWith(Parameterized.class)
	public class ParameterizedBeanTest {

	private Object[] typeBeans;

	/** will be called for each item in the result collection of parameters() */
	public ParameterizedBeanTest(Object[] typeBeans) {
		this.fileType = filetype;
		this.typeBeans = typeBeans;
		filename = filename + filetype;
	}

	@Parameters(name="{0}")
	public static Collection<Object[]> parameters() {
		LinkedList<Object[]> list = new LinkedList<>();
		for (FileType fileType : FileType.values()) {
			// here you see the standard call to fill a TypeBean.class with random values (creating two instances)
			list.add(new Object[] { ValueRandomizer.provideRandomizedObjects(2, TypeBean.class)});
		}
		return list;
	}
	
	@Test
	public void testMyBean() {
		// here we use the member, filled on construction
    	checkMyBeans(typeBeans);
	}

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

#### ValueSets

The ValueRandomizer respects zero numbers (on test duplication > 2) and provides loading of value sets.

A value set will be loaded from file with file name equal to the java class name. The values have to be separated by java carriage return ('\n').

On Comparables like numbers, the value set may only be an area with a min and max value, separated by '<->'.

Area of numbers:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
-5.0<->5.0

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


Example:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
randomize value type: java.lang.String -> will be loaded from optional file 'string.set'
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

sample content for string.set:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Name1
Name2
Name3
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

For type String (->string.set) there is a default valueset with german city names.

#### Exceptional instancing on PrintWriters

The String Constructor of class *PrintWriter* will create a file in the *user.dir* folder with given name as file name. On testing , this may result in files with random names inside your working directory. To avoid that, the *ValueRandomizer* will explicitly create the randomized PrintWriters into the test directory - using the File-Constructor.


### TypeBean

in the ValueRandomizer example you have already seen the creation of randomized TypeBeans. In the following
simple example we directly create one instance of TypeBean.class with randomized values

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// giving a parameter of true, the TypeBean will fill itself with randomized values
	TypeBean myTestBean = new TypeBean(true);
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

### TextComparison

example of using text comparison with ignore expressions:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
      exptectedHtml = new String(FileUtil.getFileBytes(expFileName, null));
      BaseTest.assertEquals(exptectedHtml, html, true, MapUtil.asMap("\\:[0-9]{5,5}", ":XXXXX",
          "20\\d\\d(-\\d{2})*", BaseTest.XXX,
          "[0-9]{1,6} Sec [0-9]{1,6} KB", "XXX Sec XXX KB", 
          "statusinfo-[0-9]{13,13}\\.txt", "statusinfo-XXXXXXXXXXXXX.txt",
          BaseTest.REGEX_DATE_US, BaseTest.XXX,
          BaseTest.REGEX_DATE_DE, BaseTest.XXX,
          BaseTest.REGEX_TIME_DE, BaseTest.XXX,
          ));
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

## Usage of AutoTestGenerator and CurrentStatePreservationTest

The *CurrentStatePreservationTest* tries to do all for you. Calling the *AutoTestGenerator* will find all available classes and methods in your classpath, trying to call all methods, storing the results and restoring them as unit tests through the ExpectationTester again.

If you call the test a second time, all generated tests are restored. So, no new generation will be done -> this works as preservation of current state.

There are some parameters (system properties) you can specify. Here, you see the properties (they all have the prefix 'tsl2.functiontest.', e.g.: 'tsl2.functiontest.clean') with their default values:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
AutoTestGenerator(PREFIX: tsl2.functiontest.) started with:
	user.dir               : /home/xxxx/workspace/tsl2nano-code/tsl2.nano.autotest/target/autotest
	user.name              : xxxx
	start time             : 25.06.2021 17:22:36
	forbidSystemExit       : false
	parallel               : true
	timeout (sec)          : 100
	filename pattern       : generated/generated-autotests-
	fast.classscan         : true
	clean                  : false
	duplication            : 10
	filter                 : .*(de.tsl2.nano.core).*
	modifier               : -1
	filter.test            : .*(Test|IT)
	filter.exclude         : XXXXXXXX
	filter.unsuccessful    : true
	filter.voidparameter   : false
	filter.voidreturn      : false
	filter.complextypes    : false
	filter.failing         : false
	filter.nullresults     : false
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

* *forbidSystemExit*: if true, a *securitymanager* will be created to disable calls to *System.exit()*. May have collisions with other SecuriyManagers and other permissions - so be careful with this.
* *timeout* : (default: 100) time in seconds for each single test (extra thread will interupt unit test). if parallel is false or timeout is -1, no timeout will be checked.
* *filename*: path to generate the auto tests into. on duplication > 0, you will have more than one generated file (e.g.: generated-autotests-0 and generated-autotests-1)
* *fast.classscan*: if true, no class re-scan is done. may find less matches than with re-scan, but is much faster (should be used on classloader with more than 1000 classes)
* *filter.test*: filter for test classes (default: ".*(Test|IT)")
* *filter.exclude*: additional excluding regex filter for methods, that should not be tested by this tests
* *testneverfail*: (default: false) only to check the resulting test coverage - if true, no test of *AllAutoTests* will ever fail. Please dont publish that to your application!
* *clean*: (default: false) whether to delete all previously generated test files prior to start the new tests.
* *duplication*: (default: 10) a duplication of 10 will generate 11 random calls on each method. A duplication > 2 will result in the use of zero numbers in the first test set.
* *filter*: (default: this framework package path) fuzzy class+method filter. NOTE: it's fuzzy finding, means, all 'similar' findings will be included!
* *modifier*: (default: -1) java method mofifier bitfield like *public* (=1) *static* (=8) etc. Please have a look at the java class *java.lang.reflect.Modifier* to see all possibilities 
* *filter.unsuccessful*: (default: true): if true, a pre-check is done, calling the test for a failing result. If the test will fail, it will be filtered from the real test.
* *filter.voidparameter*: (default: false) functions without parameters in a static class can only be tested against an exception (see fail). if set to true, methods without parameters in a static class will be filtered. Doing that, you may have to filter (filter.exclude) some of your classes and/or methods.
* *filter.voidreturn*: (default: false) whether to filter methods having a return type of *void*. without an output like the returned result, an expectation can only tested against having a specific exception or not.
* *filter.complextypes*: (default: false) all method parameter types and the result type will be checked, if they are standard data types (provided by jdk) and single value types.(nothing like arrays, collections and maps)
* *filter.failing*: (default:false) whether it is allowed to have a method call , throwing an exception as expected result.
* *filter.nullresults*: (default: false) whether it is allowed to have a method call, returning *null* as result.

The *AutoTestGenerator* finds methods to test, calls them and does a full test - comparing the result to the first call. If successful done, it writes the file *...target/autotest/generated/generated-autotests-XXX.txt with XXX as number of iteration (given by duplication).

Example of generated-autotests-XXX.txt:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
@Expectations({@Expect( when = {"88.00600103264951","Schwalmtal-Storndorf","Zuzenhausen","0"} then = "88.00600103264951"})
public static java.lang.String de.tsl2.nano.core.util.StringUtil.substring(java.lang.StringBuilder,java.lang.String,java.lang.String,int)


@Expectations({@Expect( when = {"Petzow","Extertal-Almena","Ripperterhof"} then = "Petzow"})
public static java.lang.String de.tsl2.nano.core.util.StringUtil.substring(java.lang.String,java.lang.String,java.lang.String)


@Expectations({@Expect( when = {"Korb","Buchfart","Uplengen - Ockenhausen","true","true"} then = "null"})
public static java.lang.String de.tsl2.nano.core.util.StringUtil.substring(java.lang.String,java.lang.String,java.lang.String,boolean,boolean)


@Expectations({@Expect( when = {"Oberhofen","Göggenhofen","Großkarlbach","true"} then = "Oberhofen"})
public static java.lang.String de.tsl2.nano.core.util.StringUtil.substring(java.lang.String,java.lang.String,java.lang.String,boolean)


@Expectations({@Expect( when = {"82.40402009066662","Telgte","Waldsassen"} then = "0"})
public static int de.tsl2.nano.core.util.StringUtil.replaceAll(java.lang.StringBuilder,java.lang.String,java.lang.String)


@Expectations({@Expect( when = {"Großheirath","0"} then = "fail(de.tsl2.nano.core.ManagedException(java.lang.ArithmeticException: / by zero))"})
public static final java.lang.String[] de.tsl2.nano.core.util.StringUtil.split(java.lang.String,int)


@Expectations({@Expect( when = {"Göttingen","Wettenberg"} then = "Göttin"})
public static java.lang.String de.tsl2.nano.core.util.StringUtil.trim(java.lang.String,java.lang.String)


@Expectations({@Expect( when = {"Hadersbach","0","Königswinter"} then = "fail(de.tsl2.nano.core.ManagedException(java.lang.ArithmeticException: / by zero))"})
public static final java.lang.String de.tsl2.nano.core.util.StringUtil.format(java.lang.String,int,java.lang.String)

@Expectations({@Expect( when = {"[32]","[7]"} then = "false"})
public static boolean de.tsl2.nano.core.util.ByteUtil.equals(byte[],byte[])
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

There you can see the different types of randomized values and method invokings ending in an exception, that will be used as expected result on the next test run.
This generated files should be stored in your test resources folder to be loaded on next test run. Then you have current state preservation test. Or you copy the lines with @Expectectation annotations and
put them into your source code at the position of the given method.

At the end, you will get a short statistical overview:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
AutoTestGenerator created 458 expectations in file pattern: 'generated/generated-autotests-...'
	end time              : 25.06.2021 17:23:17
	testneverfail         : false
	classfinder cls/mthds : 646 / 11152
	methods loaded        : 512	(rate: 0.045911048)
	duplications          : 10	(methods loaded * duplications: 5120)
GENERATION PROCESS:
	created with fail     : 1500
	created with null     : 0
	created totally       : 458
	filtered type error   : 289
	filtered complex types: 0
	filtered errors       : 2189
	filtered nulls        : 0
	GENERATED FUNCTION TESTERS GROUPED BY STATE:
		NEW                   : 0
		FUNC_WITHOUT_INTPUT   : 0
		FUNC_WITHOUT_OUTPUT   : 0
		FUNC_COMPLEX_INPUT    : 0
		PARAMETER_UNDEFINED   : 0
		PARAMETER_ERROR       : 470
		INITIALIZED           : 0
		INSTANCE_ERROR        : 1720
		NULL_RESULT           : 0
		EXECUTION_ERROR       : 1189
		OK                    : 289
		STORE_ERROR           : 0
		TEST_FAILED           : 680
		TESTED                : 772
		<<< TOTALLY >>>       : 5120
LOADING PROCESS:
	filtered unsuccessful : 680
	load errors           : 53
	loaded unsuccessful   : 23
	totally loaded        : 1500 (load-rate: 0.29296875, total-rate: 0.013450502)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

* *methods loaded*: if the test was not started before, generating the auto tests - or the property clean=true was set, then the *AutoTestGenerator* will load classes and methods (filtered throuth given property filter)
* *duplications*: the count of method calling clones to test the methods with different randomized value parameters
* *created with fail*: if the property *filter.failing* is true (default:false) this is the number of tests throwing an exception
* *created with null*: if the property *filter.nullresults* is true (default:false) this is the number of tests with a result value of null
* *filtered with type error*: if the parameters or the return value is not convertable from/to String, the method will be filtered
* *filtered complex types*: if a method as no return value (->void) or no parameters, or the parameters are to complex, the method will be filtered
* *filtered.unsuccessful*: if the property *filter.unsuccessful* is true (default:true) a test is done before the real unit test. if this test fails, it will be filtered and not provided to the real unit test.
* *load errors*: after *AutoTestGenerator* has generated auto tests, they will be loaded to be provided as unit tests. on load there may occur any errors like unavailable method, wrong method parameters (perhaps anything changed in the source code) or problems on creating the parameter values from string.
* *loaded unsuccessful*: if property filter.unsuccessful is true (default:true), this will do a test run after loading from generated file and before providing it to the real unit test. NOTE: This will hide the failing tests of your current state!
* *totally loaded*: totally loaded real unit tests. this belongs to the number of methods and duplications for the generating process, and on the filtered tests with errors on loading and initializing - or that would fail.

## The Workaround initializing a Suite having parameterized Unit Tests

With the tools of JUnit 4 it is not possible to define a method that is called before creating all parameterized tests of a suite. A @BeforeClass or a static block in the Suite class is started after calling all parameterized() methods. So, we have created a specific Test class that only has to start a method for initializations before calling all unit parameterizings.

Workaround Initialization Test Class:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
@RunWith(Parameterized.class)
public class InitAllAutoTests {

	@Parameters
	public static Collection<?> parameters() {
		BeanClass.callStatic("de.tsl2.nano.util.autotest.creator.AllAutoTests", "init");
		return Arrays.asList();
	}

	@Test public void nothing() {}
}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

## Tip: check the AutoGeneration while debugging with Standard Java Exception Breakpoints

The simplest way to use this framework is to use the *as is* unit tests by *CurrentStatePreservationTest*. But you can try to check for simple errors/exceptions on running the test in your IDE (like eclipse) in debug mode and activating standard java exception breakpoints.

Stopping on these exception breakpoints, you can check, if it is an error inside your method. Using this test framework, I found some unexpected calls to my methods, so I fixed them.

Two files are written, to let you see, what was done:
* *autotest/generated/autotest-generated-filtered.txt*   : lists all filtered function calls
* *autotest/generated/autotest-generated-statistics.txt* : lists statistics about filtered and created expectation tests

## Examples

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
package de.tsl2.nano.util.autotest.creator;

import static de.tsl2.nano.autotest.creator.InitAllAutoTests.matchPackage;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.tsl2.nano.autotest.creator.AutoFunctionTest;
import de.tsl2.nano.autotest.creator.CurrentStatePreservationTest;
import de.tsl2.nano.autotest.creator.InitAllAutoTests;
import de.tsl2.nano.core.Main;

@RunWith(Suite.class)
@SuiteClasses({InitAllAutoTests.class, AutoFunctionTest.class, CurrentStatePreservationTest.class})
public class AllAutoTests {
	public static void init() {
		System.setProperty("tsl2.functiontest.filter.voidparameter", "true");
		System.setProperty("tsl2.functiontest.filter.voidreturn", "true");
	
		System.setProperty("tsl2.functiontest.filter.exclude", ".*(SystemUtil.executeRegisteredLinuxBrowser|SystemUtil.softExitOnCurrentThreadGroup|ThreadState.top|LogFactory).*");
		System.setProperty("tsl2.functiontest.filter", matchPackage(Main.class, FuzzyFinder.class));
	}
}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

## Some Hints

* use **InitAllAutoTests.matchPackages(..your class names..)** in your AllAutoTests class to be sure , your classes are loaded by classloader
* use **InitAllAutoTests.set(on|off, propertyNames)** as convenience to activate some properties (e.g.: set(true, "parallel"), avoiding to write System.setProperty("tsl2.functiontest.parallel", "true") ) 

### Example to test exactly one function (e.g. for debugging)

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		String matchPackage = matchPackage(MyClassOnTest.class);
		matchPackage = ".*AnyDeclaringClass.anymethodToTest.*"; 
		System.setProperty("tsl2.functiontest.filter", matchPackage);
		System.setProperty("tsl2.functiontest.duplication", "1");
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

## Problems and Solutions

In different environment, there may be problems. We try to solve some of them:

* **java.lang.ClassNotFoundException: org.junit.runner.manipulation.Filter**
	* -> Did you store you file really in the src/test/java path (perhaps you put it into main?)
	* -> Eclipse Problem. Add the junit library manually to your test classpath
* **java.lang.ArrayStoreException: sun.reflect.annotation.TypeNotPresentExceptionProxy**
	* -> Did you store you file really in the src/test/java path (perhaps you put it into main?)
* **No Classes were found**
	* -> Please set the filter in the manner *".*" + AnyClassToTest.class.getPackage().getName() + ".*"* - so your *AnyClassToTest* is loaded with all imports directly on start
* **I cannot see exception stacktraces**
	* -> start your jvm with parameter -XX:-OmitStackTraceInFastThrow
* **AllAutoTests hangs until an timeout**
	* use JVisualVM to open the hanging process in FeatureTab **Sampler**. Hit **CPU Samples** and open the callstack tree of  *main* thread completely. Perhaps you can see an endless or blocking loop (like Scanner.hasNextLine(), Semaphore.tryAquire(), etc.) in your code under test.
* **AllAutoTests stops with an timeout - started with maven surefire**
	* -> increase the surefire properties *surefire.exitTimeout* or *surefire.timeout*
* **AllAutoTests ends always with test failures or errors**
	* -> try to evaluate the classes and methods causing these errors. set system properties in your AllAutoTests class like:  *parallel=false*, *modifier=7* (->public), *filter.exclude=..my-class-and-or-methodnames* *filter.complextypes=true*, *filter.voidparameter=true*, *filter.voidreturn=true*
* **Files are written outside of target or test directories**
* -> set a breakpoint to *java.io.FileOutputStream.open()* with breakpoint-property '!path.getAbsoluteFile().contains("target/")'. Mostly, a parameter of type *PrintWriter* will invoke a call to *PrintWriter(String)* that create randomized files through the ValueRandomizer

## All Together

To enable all standard test features of this framework, do the following:

* Add a maven dependency for *net.sf.tsl2nano:tsl2.nano.autotest:2.4.7* on scope *test*.
* Optionally, do the steps of chapter *Usage of Test Annotations* to add manual annoations (@Expectation and @InverseFunction) to your methods.
* create a java unit test class **de.tsl2.nano.util.autotest.creator.AllAutoTests** in your test source directory and fill it with following code:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
package de.tsl2.nano.util.autotest.creator;
...
@RunWith(Suite.class)
@SuiteClasses({InitAllAutoTests.class, AutoFunctionTest.class, CurrentStatePreservationTest.class})
public class AllAutoTests {
	public static void init() {
		System.setProperty("tsl2.functiontest.filter", ".*" + AnyClassToTest.class.getPackage().getName() + ".*");
	}
}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

* The class must be exactly : *de.tsl2.nano.util.autotest.creator.AllAutoTests* inside your *src/test/java* folder
* The first Suite class has to be *InitAllAutoTests.class* to start the init() method before all
* The method must be exactly: *public static void init()*
* Please replace 'AnyClassToTest' with your base class name
* If you evaluate the package name in this manner, you are sure, the class 'AnyClassToTest' is loaded by the classloader and the classfinder will find the package.
* If you have more than one package to be tested, use the convenience method *InitAllAutoTests.matchPackage(MyClass1, MyClass2, ...)*

**NOTE**:
This static class name and init method name are used as workaround to the problem that a junit suite on parameterized unit tests has no standard possibility to initialize the test suite before parametrizing all tests. So, we use the trick of the empty *InitAllAutoTests* calling *de.tsl2.nano.util.autotest.creator.AllAutoTests.init()* by reflection.

** NOTE II **:
If the test doesn't work, have a look in chapter *Problems and Solutions*
