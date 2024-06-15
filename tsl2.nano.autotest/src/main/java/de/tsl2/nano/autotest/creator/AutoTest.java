package de.tsl2.nano.autotest.creator;

import de.tsl2.nano.core.IPreferences;

public enum AutoTest implements IPreferences {
	DONTTEST, FORBIDSYSTEMEXIT, DUPLICATION(9), PARALLEL, TIMEOUT(60), CHECK_TYPECONVERSION(false),
	PRECHECK_TWICE(true), MAX_LINE_LENGTH(1024 * 10), IGNORE_APPROVED, APPROVED, TESTNEVERFAIL,
			FILENAME("autotest/generated/generated-autotests-"),
	FAST_CLASSSCAN(true), CLEAN, FILTER(""), MODIFIER(-1), 
	FILTER_TEST(".*(IT|Test)"), FILTER_EXCLUDE(REGEX_UNMATCH), FILTER_UNSUCCESSFUL(true), FILTER_VOID_PARAMETER, FILTER_VOID_RETURN, FILTER_COMPLEXTYPES, FILTER_SINGELTONS(true), 
	FILTER_NONINSTANCEABLES(true), FILTER_ERROR_TYPES(REGEX_UNMATCH), FILTER_FAILING, FILTER_NULLRESULTS, 
	ALLOW_SINGLE_CHAR_ZERO, ALLOW_SINGLE_BYTE_ZERO, CREATE_RANDDOM_MAX_DEPTH(10), VALUESET_GROUP("default"), VALUESET_AVOID_COLLISION(true);

	public static final String PREFIX_FUNCTIONTEST = "tsl2.functiontest.";

	static {
		IPreferences.init(AutoTest.class, PREFIX_FUNCTIONTEST);
	}
	
	Object value;
	AutoTest() {
		set(false);
	}
	AutoTest(Object value) {
		set(value);
	}
	@Override
	public Object get() {
		return value;
	}
	private void set(Object value) {
		this.value = value;
	}
	@Override
	public String toString() {
		return name() + "[" + value + "]";
	}
}
