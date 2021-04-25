package de.tsl2.nano.util.test;

import java.util.Map;

import de.tsl2.nano.core.util.MapUtil;

/**
 * Tests with {@link ReverseFunction} annotation to be found by
 * {@link ReverseFunctionTest}.
 * 
 * @author Thomas Schneider
 */
public class TestFunctionWithReverse {
	private String value;

	@ReverseFunction(methodName = "setValue", parameters = { String.class })
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public static Number increase(Number input) {
		return input.doubleValue() + 1;
	}

	@ReverseFunction(methodName = "increase", parameters = { Number.class }, 
			compareParameterIndex = 0, 
			bindParameterIndexesOnReverse = {-1})
	public static Number decrease(Number input) {
		return input.doubleValue() - 1;
	}
	
	static Map<String, Object> fileData;
	enum FileType {CSV, TABSHEET, MARKDOWN};
	public static void writeFile(Object data, FileType fileType, String path, String filename, boolean append) {
		fileData = MapUtil.asMap(path + filename + fileType, data);
	}
	@ReverseFunction(methodName = "writeFile", 
			parameters = {Object.class, FileType.class, String.class, String.class, boolean.class}, 
			compareParameterIndex = 0, bindParameterIndexesOnReverse = {2, 3, 1})
	public static Object readFile(String path, String filename, FileType fileType) {
		return fileData.get(path + filename + fileType);
	}
}
