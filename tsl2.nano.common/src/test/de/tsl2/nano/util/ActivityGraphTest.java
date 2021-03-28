package de.tsl2.nano.util;

import java.util.Arrays;

import org.junit.Test;

import de.tsl2.nano.structure.ANode;

public class ActivityGraphTest {

	@Test
	public void test() {
		ANode a = new ANode("A"), b, c;
		a.connect((b = new ANode("B")), "start").getDestination().connect((c = new ANode("C")), "true").getDestination().connect(a, "false").getDestination();
		b.connect(c, "perhaps");
		String s = new ActivityGraph("test").create(Arrays.asList(a, b, c)).toString();
		System.out.println(s);
//		assertEquals(t, s);
	}

}
