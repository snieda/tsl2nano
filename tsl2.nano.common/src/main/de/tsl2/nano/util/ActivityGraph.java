package de.tsl2.nano.util;

import java.util.function.Function;

import de.tsl2.nano.core.ENV;

public class ActivityGraph extends GraphLog {

	public ActivityGraph(String name) {
		super(name);
	}

	public ActivityGraph(String name, Function<Object, String> styler) {
		super(name, styler);
	}
	
	@Override
	protected String gravizStart() {
		return "<img src='https://g.gravizo.com/svg?\n@startuml; ";
	}
	@Override
	protected String gravizEnd() {
		return "@enduml\n'>";
	}
	@Override
	public GraphLog add(Object node, Object dest, Object descriptor) {
		graph.append("\"" + node + "\" --> [" + descriptor + "]  \"" + dest + "\"\n");
		return this;
	}
	public String getFileName() {
		return ENV.getConfigPath() + name + "-activity.md.html";
	}
}
