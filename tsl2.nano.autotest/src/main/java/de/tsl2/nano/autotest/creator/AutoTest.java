package de.tsl2.nano.autotest.creator;

public enum AutoTest implements IPreferences {
	DONTTEST, FORBIDSYSTEMEXIT, DUPLICATION(9), PARALLEL, TIMEOUT(20), PRECHECK_TWICE(true), TESTNEVERFAIL, FILENAME("autotest/generated/generated-expectations-"), 
	FAST_CLASSSCAN(true), CLEAN, FILTER(""), MODIFIER(-1), 
	FILTER_TEST(".*(IT|Test)"), FILTER_EXCLUDE(REGEX_UNMATCH), FILTER_UNSUCCESSFUL(true), FILTER_VOID_PARAMETER, FILTER_VOID_RETURN, FILTER_COMPLEXTYPES, FILTER_SINGELTONS(true), 
	FILTER_ERROR_TYPES(REGEX_UNMATCH), FILTER_FAILING, FILTER_NULLRESULTS, 
	ALLOW_SINGLE_CHAR_ZERO, ALLOW_SINGLE_BYTE_ZERO, CREATE_RANDDOM_MAX_DEPTH(10), USE_VALUESET("default");

	static String PREFIX = "tsl2.functiontest.";
	static {
		IPreferences.init(AutoTest.class, PREFIX);
	}
	
	Object value;
	AutoTest() {
		value = false;
	}
	AutoTest(Object value) {
		this.value = value;
	}
	@Override
	public Object get() {
		return value;
	}
	
	public boolean enabled() {
		return value instanceof Boolean && Boolean.TRUE.equals(value);
	}
}
