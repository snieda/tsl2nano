package de.tsl2.nano.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import de.tsl2.nano.structure.IConnection;
import de.tsl2.nano.structure.INode;

/**
 * Provides creation of text blocks to be interpreted by Gravizo and rendered with Graphviz inside a markdown text.
 * <pre>
 * Use:
 *   new GraphLog("mynodes").create(MyListOfNodes).write();
 *   
 * this will create the file 'mymodes-graph.md.html'
 * 
 * @author Thomas Schneider
 */
public class GraphLog {
	private StringBuilder graph;
	private String name;
	
	public GraphLog(String name) {
		this.name = name;
		graph = new StringBuilder(gravizStart());
	}
	private String gravizStart() {
		return "![](http://g.gravizo.com/svg?digraph G {\n";
	}
	public GraphLog create(Iterable<? extends INode> nodes) {
		for (INode<?,?> n : nodes) {
			for (IConnection c : n.getConnections()) {
				add(n.getCore(), c.getDestination().getCore(), c.getDescriptor());
			}
		}
		return this;
	}
	public GraphLog add(Object node, Object dest, Object descriptor) {
		graph.append(node + " -> " + dest + "[label=\"" + descriptor + "\"];\n");
		return this;
	}
	
	String addMarkDeepStyle() {
		return "\n\n<!-- Markdeep: --><style class=\"fallback\">body{visibility:hidden;white-space:pre;font-family:monospace}</style><script src=\"markdeep.min.js\"></script><script src=\"https://casual-effects.com/markdeep/latest/markdeep.min.js?\"></script><script>window.alreadyProcessedMarkdeep||(document.body.style.visibility=\"visible\")</script>";
	}
	@Override
	public String toString() {
		return graph + gravizEnd() + addMarkDeepStyle();
	}
	private String gravizEnd() {
		return "})";
	}
	public void write() {
        try {
			Files.write(Paths.get(name + "-graph.md.html"), toString().getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
