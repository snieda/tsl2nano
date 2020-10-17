package de.tsl2.nano.util;

import java.util.Arrays;

import org.junit.Test;

import de.tsl2.nano.structure.ANode;

public class GraphLogTest {

	@Test
	public void test() {
		//cycling net
		ANode a = new ANode("A"), b, c;
		a.connect((b = new ANode("B")), 1).getDestination().connect((c = new ANode("C")), 2).getDestination().connect(a, 3).getDestination();
		String s = new GraphLog("test").create(Arrays.asList(a, b, c)).toString();
		System.out.println(s);
	}

}
