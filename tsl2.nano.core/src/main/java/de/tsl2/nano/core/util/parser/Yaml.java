package de.tsl2.nano.core.util.parser;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.regex.Pattern;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.StringUtil;

/**
 * simple yaml de-/serializer
 * lists of lists are not supported. (a list cannot directly contain another list)
 */
public class Yaml extends StructParser.ASerializer {
    private static final String YAML_EXPR = "\\s*(?:-\\s)?\\w+:\\s?\"?.*\"?";
    private static final Pattern YAML_PATTERN = Pattern.compile(YAML_EXPR, Pattern.MULTILINE);

    @Override
    public boolean isParseable(CharSequence s) {
        return YAML_PATTERN.matcher(s).find();
    }

    @Override
    public String commentExpression() {
        return "\\s*#.*\n";
    }
    @Override
    public boolean isList(CharSequence s, TreeInfo tree) {
        if (tree.isRoot() || tree.current().isArray())
            return false;
        String[] children = getChildren(s, tree); // -> poor performance
        return children.length == 0
                || Arrays.stream(children).allMatch(c -> {
                    try (Scanner sc = new Scanner(c)) {
                        return sc.nextLine().matches("\\s+- \\w+:.*");
                    }
                });
    }

    @Override
    public String tagOpen(TreeInfo tree) {
        return indent(tree) + arrElementIdentifier(tree) + tree.currentName() + ":\n";
    }

    private String indent(TreeInfo tree) {
        return StringUtil.fixString(tree.path.size() - 1, '\t');
    }

    @Override
    public String tagClose(TreeInfo tree) {
        return "";
    }

    public String arrOpen(TreeInfo tree) {
        return !tree.current().isStream() ? "\n" : "";
    }

    @Override
    public String arrClose(TreeInfo tree) {
        return "";
    }

    @Override
    public String div() {
        return "\n";
    }

    @Override
    public String encloseKey(Object k, TreeInfo tree) {
        return indent(tree) + arrElementIdentifier(tree) + k + ": ";
    }

    @Override
    public Object encloseValue(Object obj, TreeInfo tree) {
        String referenceIndent = "";
        if (tree.isReference(obj))
            referenceIndent = encloseKey(tree.currentName(), tree);
        return referenceIndent + super.encloseValue(obj, tree);
    }

    @Override
    public String arrElementIdentifier(TreeInfo tree) {
        return tree.getParent() != null && tree.getParent().isArray() ? "- " : "";
    }

    @Override
    public String[] getKeyValue(CharSequence attr) {
        return getKeyValue(attr, ":", false);
    }

    @Override
    public String[] getChildren(CharSequence s, TreeInfo tree) {
        return getChildren(s, tree, tree != null && tree.current().isArray());
    }

    public String[] getChildren(CharSequence s, TreeInfo tree, boolean excluding) {
        String v = tree != null ? StringUtil.trim((getKeyValue(s))[1], " \t\n\"") : null;
        if (v != null && tree.isReference(v)) {
            return new String[] { indent(tree) + arrElementIdentifier(tree) + BeanClass.BeanMap.KEY_REF + ": " + v };
        } else {
        LinkedList<StringBuffer> children = new LinkedList<>();
        try (Scanner sc = new Scanner(s.toString())) {
            String l;
            int childLIndent = -1;
            StringBuffer item = null;
            while (sc.hasNextLine()) {
                l = sc.nextLine();
                if (l.trim().isEmpty())
                    continue;
                if (childLIndent == -1) {
                    childLIndent = readIndent(l) + (excluding || tree.isRoot() ? 1 : 0);
                }
                int indent = readIndent(l);
                if (indent == childLIndent) {
                    item = new StringBuffer();
                    children.add(item);
                }
                if (item != null && indent >= childLIndent)
                    item.append(l + "\n");
            }
        }
        if (children.size() == 0 && !tree.current().isArray())
            LOG.error("unexptected empty content of \"" + s);
        return children.stream().map(c -> c.toString()).toArray(String[]::new);
    }
}

@Override
public String[] splitArray(CharSequence s, TreeInfo tree) {
    return getChildren(s, tree, false);
}

    private int readIndent(String l) {
        return StringUtil.extract(l, "^\\s*").length();
    }

    @Override
    public String trim(String s) {
        return StringUtil.trim(s, " \t-" + quot() + (s.contains(":") ? "" : "\n"));
    }
}
