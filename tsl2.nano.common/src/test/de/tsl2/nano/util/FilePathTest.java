package de.tsl2.nano.util;

import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;

import de.tsl2.nano.core.util.StringUtil;

public class FilePathTest {

	@Test
	public void testFilePath() {
		Collection<String> result = FilePath.foreach(System.getProperty("user.dir") + "/..", ".*[.]java", ".*[.]FilePath[.]java", ".*void main.*", f -> f.toAbsolutePath().normalize().toString());
		String resstr = StringUtil.toFormattedString(result, -1);
		System.out.println(resstr);
		assertTrue(result.size() > 0);
		FilePath.write("all-main-classes.txt", resstr.getBytes());
	}
}
