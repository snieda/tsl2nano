package de.tsl2.nano.core.util;

import java.util.regex.Pattern;

public class Yaml extends StructParser.ASerializer {
    private static final String YAML_EXPR = "[\"]?(?:[\\{\\[](?:(?:\\s*+[\"]?\\w++[\"]?\\s*)[:,](?:\\s*+[\"]?[^\"]*+[\"]?\\s*+)[,]?)*+[\\}\\]])++[\"]?";
    private static final Pattern YAML_PATTERN = Pattern.compile(YAML_EXPR, Pattern.MULTILINE);

    JSon json = new JSon();

    @Override
    public boolean isParseable(CharSequence s) {
        return YAML_PATTERN.matcher(s).find();
    }

    @Override
    public boolean isList(CharSequence s) {
        return json.isList(s);
    }

    @Override
    public String tagOpen(TreeInfo tree) {
        return indent(tree) + tree.currentName() + "\n";
    }

    private String indent(TreeInfo tree) {
        return StringUtil.fixString(tree.path_.size(), '\t');
    }

    @Override
    public String tagClose(TreeInfo tree) {
        return "";
    }

    public String arrOpen(TreeInfo tree) {
        tree.array = true;
        return "\n";
    }

    @Override
    public String arrClose(TreeInfo tree) {
        tree.array = false;
        return "";
    }

    @Override
    public String div() {
        return "\n";
    }

    @Override
    public String encloseKey(Object k, TreeInfo tree) {
        return indent(tree) + k + ": ";
    }

    @Override
    public String[] getKeyValue(String attr) {
        return json.getKeyValue(attr);
    }

}
