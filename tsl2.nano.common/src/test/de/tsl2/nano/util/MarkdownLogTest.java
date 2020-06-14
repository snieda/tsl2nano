package de.tsl2.nano.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.action.Parameter;
import de.tsl2.nano.core.util.ENVTestPreparation;

public class MarkdownLogTest implements ENVTestPreparation{

	@Before
	public void setUp() {
		ENVTestPreparation.setUp();
	}
	
	@Test
	public void testMarkdownLog() throws SecurityException, IOException {
		Parameter<Boolean> p1 = new Parameter<>("test1", null, true);
		Parameter<Boolean> p2 = new Parameter<>("test2", null, false);
		MarkdownLog md = new MarkdownLog();
		String title = "markdownlogtest";
		md.printTable(title, Arrays.asList(p1, p2).stream());
		
		StringBuilder expected = new StringBuilder("\n" + 
				"## markdownlogtest\n" + 
				"\n" + 
				"getName                                         | getClass                                       | getConstraint                                  | getName                                        | getValue                                       | toString                                       |\n" + 
				"------------------------------------------------|------------------------------------------------|------------------------------------------------|------------------------------------------------|------------------------------------------------|------------------------------------------------|\n" + 
				" Parameter                                      | Parameter                                      | null                                           | test1                                          | X                                              | test1: true                                    |\n" + 
				" Parameter                                      | Parameter                                      | null                                           | test2                                          | -                                              | test2: false                                   |\n" + 
				"------------------------------------------------|------------------------------------------------|------------------------------------------------|------------------------------------------------|------------------------------------------------|------------------------------------------------|\n" + 
				"");
		md.addMarkDeepStyle(expected);
		String content = new String(Files.readAllBytes(Paths.get(md.getFileName(title))));
		assertEquals(expected.toString(), content);
	}

}
