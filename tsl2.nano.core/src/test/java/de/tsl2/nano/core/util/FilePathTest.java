package de.tsl2.nano.core.util;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

public class FilePathTest {

	@Test
	public void testFilePath() {
		File file = new File("all-main-classes.txt");
		if (file.exists())
			file.delete();
		Collection<String> result = FilePath.foreach(System.getProperty("user.dir") + "/..", ".*[.]java",
				".*[.]FilePath[.]java", ".*void main.*", f -> f.toAbsolutePath().normalize().toString());
		Collections.sort(new ArrayList<>(result));
		String resstr = StringUtil.toFormattedString(result, -1);
		System.out.println(resstr);
		assertTrue(result.size() > 0);
		FilePath.write(file.getPath(), resstr.getBytes());
	}
}
