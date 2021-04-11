package de.tsl2.nano.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.function.Function;

import de.tsl2.nano.core.ENV;
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
	protected StringBuilder graph;
	protected String name;
	protected Function<Object, String> styler;
	
	public GraphLog(String name) {
		this(name, null);
	}
	
	/**
	 * @param name name of graph. will be used inside the file name, too
	 * @param styler functional expression to add a styling like a color 
	 * 		to the link (e.g.: d -> "color=" + cc.get((Integer)Math.round((int)d*10)))
	 */
	public GraphLog(String name, Function<Object, String> styler) {
		this.name = name;
		this.styler = styler;
		graph = new StringBuilder(gravizStart());
	}
	protected String gravizStart() {
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
		graph.append("\"" + node + "\" -> \"" + dest + "\"[label=\"" + descriptor + "\" " + style(descriptor) + "];\n");
		return this;
	}
	
	private String style(Object descriptor) {
		return styler == null ? "" : styler.apply(descriptor);
	}

	String addMarkDeepStyle() {
		return "\n\n<!-- Markdeep: --><style class=\"fallback\">body{visibility:hidden;white-space:pre;font-family:monospace}</style><script src=\"markdeep.min.js\"></script><script src=\"https://casual-effects.com/markdeep/latest/markdeep.min.js?\"></script><script>window.alreadyProcessedMarkdeep||(document.body.style.visibility=\"visible\")</script>";
	}
	@Override
	public String toString() {
		return graph + gravizEnd() + addMarkDeepStyle();
	}
	protected String gravizEnd() {
		return "})";
	}
	public void write() {
        try {
			Files.write(Paths.get(getFileName()), toString().getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public String getFileName() {
		return ENV.getConfigPath() + name + "-graph.md.html";
	}
	
	/** convenience to create the graphlog and write the gravito file */
	public static void createGraphFile(String name, Collection<? extends INode> elements, Function<Object, String> graphStyler) {
		new GraphLog(name, graphStyler).create(elements).write();
	}
}
