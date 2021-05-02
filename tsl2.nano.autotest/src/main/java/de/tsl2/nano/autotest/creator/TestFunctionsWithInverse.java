package de.tsl2.nano.autotest.creator;

import java.util.Map;

import de.tsl2.nano.core.util.MapUtil;

/**
 * Tests with {@link InverseFunction} annotation to be found by
 * {@link AutoFunctionTest}.
 * 
 * @author Thomas Schneider
 */
public class TestFunctionsWithInverse {
	private String value;

	@InverseFunction(methodName = "setValue", parameters = { String.class })
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Expectations({@Expect(parIndex = 0, whenPar = "1.0", then = "2.0"), @Expect(parIndex = 0, whenPar = "2.0", then = "3.0")})
	public static Number increase(Number input) {
		return input.doubleValue() + 1;
	}

	@InverseFunction(methodName = "increase", parameters = { Number.class }, compareParameterIndex = 0)
	public static Number decrease(Number input) {
		return input.doubleValue() - 1;
	}
	
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
	
	@Expectations({@Expect(when = {"1", "2", "3"}, then = "123")})
	public String concatNumbers(short first, int second, Long third) {
		return "" + first + second + third;
	}
}
