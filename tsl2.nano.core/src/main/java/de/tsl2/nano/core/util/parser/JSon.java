/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 31.03.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.core.util.parser;

import java.util.regex.Pattern;

public class JSon extends StructParser.ASerializer {

	/** this expression is only a basic pattern - if matching, it may be - nevertheless - invalid! */
	private static final String JSON_EXPR0 = "((\\[[^\\}]{3,})?\\{s*[^\\}\\{]{3,}?:.*\\}([^\\{]+\\])?)";
	private static final String JSON_EXPR1 = "[\"]?(?:[\\{\\[](?:(?:\\s*+[\"]?\\w++[\"]?\\s*)[:,](?:\\s*+[\"]?[^\"]*+[\"]?\\s*+)[,]?)*+[\\}\\]])++[\"]?";
	private static final String JSON_EXPR_STREAM = "\\[([-+.\\d]+([,]\\s*)?)*+\\]";
	private static final String NO_JSON = "[^,:\\[]+\\s*\\{.*\\}";
	private static final Pattern JSON_PATTERN0 = Pattern.compile(JSON_EXPR0, Pattern.MULTILINE);
	private static final Pattern JSON_PATTERN1 = Pattern.compile(JSON_EXPR1, Pattern.MULTILINE);
	private static final Pattern JSON_PATTERN_STREAM = Pattern.compile(JSON_EXPR_STREAM, Pattern.MULTILINE);
	static final Pattern NO_JSON_PATTERN = Pattern.compile(NO_JSON, Pattern.MULTILINE);

	public JSon() {
	}

	public JSon(SerialClass s) {
		super(s);
	}

	public boolean isParseable(CharSequence txt) {
		return isJSon(txt);
	}

	public static final boolean isJSon(CharSequence txt) {
		return !NO_JSON_PATTERN.matcher(txt).find() && JSON_PATTERN0.matcher(txt).find() || JSON_PATTERN1.matcher(txt).find()
				|| JSON_PATTERN_STREAM.matcher(txt).find();
	}

	public String tagClose(TreeInfo tree) {
		return "}";
	}

	public String arrOpen(TreeInfo tree) {
		return !tree.current().isStream() ? "[" : "";
	}

	public String arrClose(TreeInfo tree) {
		if (tree != null && !tree.refPath.isEmpty())
			tree.refPath.removeLast();
		return !tree.current().isStream() ? "]" : "";
	}

	public String tagOpen(TreeInfo tree) {
		return "{";
	}

	public String div() {
		return ",";
	}

	public String encloseKey(Object k, TreeInfo tree) {
		return "\"" + k + "\": ";
	}

	public boolean isList(CharSequence s, TreeInfo tree) {
		return s.length() > 1 && s.charAt(0) == '[' && s.charAt(s.length() - 1) == ']';
	}

}
