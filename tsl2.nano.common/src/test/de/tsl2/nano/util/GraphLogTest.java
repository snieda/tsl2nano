package de.tsl2.nano.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;

import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.structure.ANode;

public class GraphLogTest {

	@Test
	public void test() {
		String t = "![](http://g.gravizo.com/svg?digraph G {\n"
				+ "\"A\" -> \"B\"[label=\"1\" ];\n"
				+ "\"B\" -> \"C\"[label=\"2\" ];\n"
				+ "\"C\" -> \"A\"[label=\"3\" ];\n"
				+ "})\n"
				+ "\n"
				+ "<!-- Markdeep: --><style class=\"fallback\">body{visibility:hidden;white-space:pre;font-family:monospace}</style><script src=\"markdeep.min.js\"></script><script src=\"https://casual-effects.com/markdeep/latest/markdeep.min.js?\"></script><script>window.alreadyProcessedMarkdeep||(document.body.style.visibility=\"visible\")</script>";
		//cycling net
		ANode a = new ANode("A"), b, c;
		a.connect((b = new ANode("B")), 1).getDestination().connect((c = new ANode("C")), 2).getDestination().connect(a, 3).getDestination();
		String s = new GraphLog("test").create(Arrays.asList(a, b, c)).toString();
		System.out.println(s);
		assertEquals(t, s);
	}

	@Test
	public void testStyler() {
		Map<Integer, String> cc = MapUtil.asMap(1, "blue", 20, "green");
		
		ANode a = new ANode("A"), b, c;
		a.connect((b = new ANode("B")), 1).getDestination().connect((c = new ANode("C")), 2).getDestination().connect(a, 3).getDestination();
		String s = new GraphLog("test", d -> "color=" + cc.get((Integer)Math.round((int)d*10)))
				.create(Arrays.asList(a, b, c)).toString();
		System.out.println(s);
		assertTrue(s.contains("color=green"));
	}

}
