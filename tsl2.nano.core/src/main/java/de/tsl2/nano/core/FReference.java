package de.tsl2.nano.core;

import java.text.Format;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import de.tsl2.nano.core.cls.AReference;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.util.NumberUtil;
import de.tsl2.nano.core.util.StringUtil;

/**
 * FReference
 */
public class FReference extends AReference {

    @Override
    protected Object getId(Object instance) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Object type(String strType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Object id(Object type, String strId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Object materialize(String description) {
        String def = StringUtil.substring(description, "[", "]");
        List<?> range = getRange(def);
        if (range.size() == 1) {
            return range.get(0);
        } else {
            String defaultValue = StringUtil.substring(def, "|", "|");
            if (range.size() > 1 && defaultValue.startsWith("@")) {
                defaultValue = defaultValue.substring(1);
                if (NumberUtil.isNumber(defaultValue)) {
                    return range.get(PrimitiveUtil.create(int.class, defaultValue));
                }
            } else {
                Format format = getFormat(def);
                if (format != null) {
                    try {
                        format.parseObject(defaultValue);
                    } catch (ParseException e) {
                        ManagedException.forward(e);
                    }
                }
            }
        }
        return null;
    }

    private Format getFormat(String def) {
        return null;
    }

    private List<?> getRange(String def) {
        String rangeDef = StringUtil.substring(def, "{", "}");
        List<?> range = new LinkedList<>();
        if (rangeDef.contains("...")) { //min / max

        } else { //also liste über kommata

        }
        return range;
    }

}