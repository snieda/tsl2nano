package de.tsl2.nano.util.test;

public class TestFunctionWithReverse {
	private String value;

	@ReverseFunction(methodName = "setValue", parameters = {String.class})
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
