package de.tsl2.nano.core.util;

import java.util.regex.Pattern;

public class Xml extends StructParser.ASerializer {
    private static final String XML_EXPR = "\\s*(?:<\\?xml.*\\?>)?(?:<\\w+.*\\/?>(?:.*<\\/\\w+>)*)+\\s*";
    private static final Pattern XML_PATTERN = Pattern.compile(XML_EXPR, Pattern.MULTILINE);

    @Override
    public boolean isParseable(CharSequence s) {
        return XML_PATTERN.matcher(s).find();
    }

    @Override
    public boolean isList(CharSequence s) {
        return false; // xml has no lists
    }

    @Override
    public String tagOpen(TreeInfo tree) {
        return "<" + tree.currentName() + ">";
    }

    @Override
    public String tagClose(TreeInfo tree) {
        return "</" + tree.currentName() + ">";
    }

    @Override
    public String encloseKey(Object k, TreeInfo tree) {
        return "";//"\"" + k + "\"=";
    }

    @Override
    public Object encloseValue(Object obj, TreeInfo tree) {
        return tagOpen(tree) + obj + tagClose(tree);
    }

    @Override
    public String div() {
        return " ";
    }

    @Override
    public String[] getKeyValue(String attr) {
        return attr.split("<.*\\/?>");
    }

    @Override
    public String[] getChildren(CharSequence s) {
        return StringUtil.splitUnnested_(s.subSequence(1, s.length() - 1), ">");
    }
}
