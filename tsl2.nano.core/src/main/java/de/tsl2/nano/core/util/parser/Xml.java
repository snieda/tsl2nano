/*
 * created by: Tom
 * created on: 06.04.2024
 * 
 * Copyright: (c) Thomas Schneider 2024, all rights reserved
 */
package de.tsl2.nano.core.util.parser;

import java.util.Map;
import java.util.regex.Pattern;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.StringUtil;

/** 
 * simple xml serializer and parser/objectmapper not providing attributes - only tags! useful for data-structures.
 * constraints of this implemenation:
 *   1. serializatiion is possible with tags and properties/attributes, but deserializing only on tags!
 *   2. lists of lists are not supported. (a list cannot directly contain another list)
 *   3. no xml/schema validation
*/
@SuppressWarnings("unchecked")
public class Xml extends StructParser.ASerializer {
    private static final String REGEX_OPENTAG = "<\\w+>";
    private static final String REGEX_CLOSETAG = "</${open}>";
    private static final String XML_EXPR = "\\s*(?:<\\?xml.*\\?>)?(?:<\\w+.*\\/?>(?:.*<\\/\\w+>)*)+\\s*";
    private static final Pattern XML_PATTERN = Pattern.compile(XML_EXPR, Pattern.MULTILINE);
    /** (default: true), if false, simple types like numbers, dates, strings will be presented as attributes/properties */
    private boolean tagsOnly = true;

    private static final Map<String, String> ESCAPINGS;
    static {
        ESCAPINGS = MapUtil.asMap("&", "&amp;", "<", "&lt;",
                ">", "&gt;",
                "\"", "&quot;",
                "'", "&apos;");
    }

    public Xml() {
    }

    public Xml(SerialClass s) {
        super(s);
    }

    /** @param tagsOnly: @see #tagsOnly */
    public Xml(boolean tagsOnly) {
        this.tagsOnly = tagsOnly;

    }

    @Override
    public boolean isParseable(CharSequence s) {
        return XML_PATTERN.matcher(s).find();
    }

    @Override
    public String commentExpression() {
        return "\\s*<(!--|[?]).*(--|[?])>";
    }

    @Override
    public Map<String, String> escapingTable() {
        return ESCAPINGS;
    }
    @Override
    StringBuilder createInitialStringBuilder() {
        return new StringBuilder(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!-- generated by " + this.getClass().getName() + " -->");
    }

    @Override
    public boolean isList(CharSequence s, TreeInfo tree) {
        // we assume lists having only equal types of children.
        // 3 cases:
        //   1. 0 children                      => probability very high to be a list
        //   2. n (n != 1) equal typed children => probability very high to be a list
        //   3. 1 child                         => probability high to be a list

        return (!tree.isRoot() && !tree.current().isArray()) && onlySiblings(s);
    }

    private boolean onlySiblings(CharSequence s) {
        String[] children = getChildren("<root>" + s + "</root>", null);
        String tagName = null;

        for (int i = 0; i < children.length; i++) {
            if (tagName == null)
                tagName = getRootTagnameOf(children[i]);
            else if (!tagName.equals(getRootTagnameOf(children[i])))
                return false;
        }
        return true;
    }

    @Override
    public String tagOpen(TreeInfo tree) {
        return (tree.consumeTagOpenUnfinished() ? ">" : "") + "<"
                + tree.currentName()
                + (tagsOnly || tree.isReference(tree.current().value) ? ">" : "");
    }

    @Override
    public String tagClose(TreeInfo tree) {
        return "</" + tree.currentName() + ">";
    }

    @Override
    public String arrOpen(TreeInfo tree) {
        return (tree.consumeTagOpenUnfinished() ? ">" : "") + "<" + tree.currentName() + ">";
    }

    @Override
    public String arrClose(TreeInfo tree) {
            tree.refPath.removeLast();
            return "</" + tree.currentName() + ">";
    }

    @Override
    public String encloseKey(Object k, TreeInfo tree) {
        return "";//"\"" + k + "\"=";
    }

    @Override
    public Object encloseValue(Object obj, TreeInfo tree) {
        if (!tagsOnly && isSimpleType(obj) && !tree.isReference(obj)) {
            tree.setTagOpenUnfinished(true);
            return " " + tree.currentName() + "=\"" + obj + "\"";
        } else {
            tree.consumeTagOpenUnfinished();
            return tagOpen(tree) + obj + tagClose(tree);
        }
    }

    @Override
    public String div() {
        return " ";
    }

    @Override
    public String quot() {
        return "";
    }

    @Override
    public String[] getKeyValue(CharSequence attr) {
        String k = getRootTagnameOf(attr);
        String v = StringUtil.extractSubstring(attr, REGEX_OPENTAG, REGEX_CLOSETAG, 0, false);
        return new String[] { k, v };
    }

    private String getRootTagnameOf(CharSequence attr) {
        String k = StringUtil.extract(attr, REGEX_OPENTAG);
        k = StringUtil.extract(k, "\\w+");
        return k;
    }

    @Override
    public String[] getChildren(CharSequence s, TreeInfo tree) {
        return splitArray(s, tree, 1, false);
    }

    @Override
    public String[] splitArray(CharSequence s, TreeInfo tree) {
        return splitArray(s, tree, 0, true);
    }

    public String[] splitArray(CharSequence s, TreeInfo tree, int start, boolean includeRoot) {
        String[] kv;
        if (tree != null && tree.isReference((kv = getKeyValue(s))[1]))
            return new String[] {
                    "<" + BeanClass.BeanMap.KEY_REF + ">" + kv[1] + "</" + BeanClass.BeanMap.KEY_REF + ">" };
        else
            return StringUtil.splitStructure(s, REGEX_OPENTAG, REGEX_CLOSETAG, "\\w+", start, includeRoot);
    }
}