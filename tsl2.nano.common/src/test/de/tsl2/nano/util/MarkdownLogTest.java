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
import de.tsl2.nano.util.MarkdownLog.Style;

public class MarkdownLogTest implements ENVTestPreparation{

	@Before
	public void setUp() {
		ENVTestPreparation.super.setUp("common");
	}
	
	@Test
	public void testMarkdownTable() throws SecurityException, IOException {
		Parameter<Boolean> p1 = new Parameter<>("test1", null, true);
		Parameter<Boolean> p2 = new Parameter<>("test2", null, false);
		String title = "markdownlogtest";
		MarkdownLog md = new MarkdownLog(title);
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

	@Test
	public void testMarkdownPrint() {
		MarkdownLog md = new MarkdownLog("test");
		md.print("ueberschrift", Style.H1);
		md.print("irgendeintext");
		md.print("der ist kursiv.", Style.STD, "(kursiv)", Style.CURSIVE);
		md.print("das ist farbig.", Style.UNCHECKED, "(farbig)", Style.BLUE);
		md.print("ist der dick gecheckt", Style.CHECKED, "(dick)", Style.BOLD);
		md.print("das ist ein link", Style.STD, "(link)", Style.LINK);
		md.print("das ist ein image", Style.STD, "(image)", Style.IMAGE);
		md.write();
		System.out.println(md.buf);
		
		String expected = "\n## test\n"
				+ "\n"
				+ "# ueberschrift\n"
				+ "irgendeintext\n"
				+ "der ist _kursiv_).\n"
				+ "- [ ] das ist <span style=\"color:blue\">farbig</span>.\n"
				+ "- [x] ist der *dick* gecheckt\n"
				+ "das ist ein [](link)\n"
				+ "das ist ein ![](image)\n"
				+ "\n"
				+ "<!-- Markdeep: --><style class=\"fallback\">body{visibility:hidden;white-space:pre;font-family:monospace}</style><script src=\"markdeep.min.js\"></script><script src=\"https://casual-effects.com/markdeep/latest/markdeep.min.js?\"></script><script>window.alreadyProcessedMarkdeep||(document.body.style.visibility=\"visible\")</script>";
		assertEquals(expected, md.buf.toString());
		
	}
}
