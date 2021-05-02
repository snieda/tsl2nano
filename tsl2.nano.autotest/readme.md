# Automatic Unit Test Creation

Thomas Schneider 04/2021

## Introduction

Unit testing with a good code coverage is essential for modern software. test-driven development seems to be a productive way to implement new software. but unit tests are expensive. 
Developers are lazy - they always want to automate the things they have to do ;-)

Isn't there any way to automate some test creation? Why do we not declare expectations as annotations on our functions or methods to provide something like a specification for the implementation of the function? If we have a function - what's about the inverse function? Couldn't we declare an existing inverse function to be used by a test creation implementation?

But if we declare Expections or Inverse functions, we need a mechanism to fill method parameters randomly (or with expectation values). For complex objects , we need a test object implementation holding all java types as attributes. Further more we need a conversion framework to provide parameters in the right type.

The framework tsl2nano provides the base for the implementation. A parameterized Unit Test uses features like declared Expectations and Inverse Functions to do the tests. It is extendable to add more features with new annotions and tester-implemenations.

Additionally, there are the following Implementations:

* ValueRandomizer: is able to create random values of any java type
* TypeBean       : example implementation of a java bean holding all java types as bean attributes
* BaseTest       : base class for own unit tests providing expectations and textcomparisons (ignoring defined blocks of temporary text)
* TextComparison : provides text comparisons with ignoring of defined regular expression blocks

## Code Review

The Test Creator (the class *AutoFunctionTest*) defines the features to be tested. Some properties like a *test name filter*, *test duplication* etc. can be set by system properties.

At the moment, we have two features with their tester implementations:

* InverseFunction -> InverseFunctionTester
* Expectations    -> ExpectationsTester

## Usage

The annotations are packed into the tsl2nano core jar. So, you need a maven dependency for *tsl2.nano.core*. The *AutoFunctionTest* is included in the package *tsl2.nano.autotest*. Please add a maven dependency on scope *test*.

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

example with an parametrized test, creating a set of randomized values:

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

### TypeBean

in the ValueRandomizer example you have already seen the creation of randomized TypeBeans. In the following
simple example we directly create one instance of TypeBean.class with randomized values:

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

