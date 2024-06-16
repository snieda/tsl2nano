/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 31.03.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.core.util;

import java.util.regex.Pattern;

public class JSon extends StructParser.ASerializer {

	/** this expression is only a basic pattern - if matching, it may be - nevertheless - invalid! */
	private static final String JSON_EXPR0 = "((\\[[^\\}]{3,})?\\{s*[^\\}\\{]{3,}?:.*\\}([^\\{]+\\])?)";
	private static final String JSON_EXPR1 = "[\"]?(?:[\\{\\[](?:(?:\\s*+[\"]?\\w++[\"]?\\s*)[:,](?:\\s*+[\"]?[^\"]*+[\"]?\\s*+)[,]?)*+[\\}\\]])++[\"]?";
	private static final String JSON_EXPR_STREAM = "\\[([-+.\\d]+([,]\\s*)?)*+\\]";

	private static final Pattern JSON_PATTERN0 = Pattern.compile(JSON_EXPR0, Pattern.MULTILINE);
	private static final Pattern JSON_PATTERN1 = Pattern.compile(JSON_EXPR1, Pattern.MULTILINE);
	private static final Pattern JSON_PATTERN_STREAM = Pattern.compile(JSON_EXPR_STREAM, Pattern.MULTILINE);

	public boolean isParseable(CharSequence txt) {
		return isJSon(txt);
	}

	public static final boolean isJSon(CharSequence txt) {
		return JSON_PATTERN0.matcher(txt).find() || JSON_PATTERN1.matcher(txt).find()
				|| JSON_PATTERN_STREAM.matcher(txt).find();
	}

	public String tagClose(TreeInfo tree) {
		return "}";
	}

	public String arrOpen(TreeInfo tree) {
		return "[";
	}

	public String arrClose(TreeInfo tree) {
		tree.path.removeLast();
		return "]";
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

	public boolean isList(CharSequence s) {
		return s.length() > 1 && s.charAt(0) == '[' && s.charAt(s.length() - 1) == ']';
	}

	public String[] getKeyValue(String attr) {
		String[] kv = new String[2];
		kv[0] = trim(StringUtil.substring(attr, null, ":"));
		kv[1] = StringUtil.substring(attr, ":", null).trim();
		return kv;
	}
}
