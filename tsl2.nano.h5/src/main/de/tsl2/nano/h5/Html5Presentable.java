package de.tsl2.nano.h5;

import static de.tsl2.nano.h5.HtmlUtil.ATTR_HEIGHT;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_SIZE;

import java.io.Serializable;
import java.util.LinkedHashMap;

import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.GroupingPresentable;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.Util;

/**
 * Hmtl5-specialized {@link de.tsl2.nano.bean.def.Presentable}. Not possible to be handled as inner class, because of
 * xml-serialization.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked" })
public class Html5Presentable extends GroupingPresentable {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    private static final LinkedHashMap<String, String> DEFAULT_HASHMAP = new LinkedHashMap<String, String>();

    protected Html5Presentable() {
    }

    public Html5Presentable(AttributeDefinition<?> attr) {
        super(attr);
    }

    @Override
    public int getWidth() {
        String w = layout(ATTR_SIZE);
        return w != null ? Integer.valueOf(w) : UNDEFINED;
    }

    public void setWidth(int width) {
        if (width != UNDEFINED) {
            getLayout().put(ATTR_SIZE, String.valueOf(width));
        } else {
            getLayout().remove(ATTR_SIZE);
        }
    }

    @Override
    public int getHeight() {
        String h = layout(ATTR_HEIGHT);
        return h != null ? Integer.valueOf(h) : UNDEFINED;
    }

    public void setHeight(int height) {
        if (height != UNDEFINED) {
            getLayout().put(ATTR_HEIGHT, String.valueOf(height));
        } else {
            getLayout().remove(ATTR_HEIGHT);
        }
    }

    /**
     * @return Returns the layout.
     */
    @Override
    public LinkedHashMap<String, String> getLayout() {
        if (layout == null) {
            //LinkedHashmap not supported by simple-xml
            layout = new LinkedHashMap<>(ENV.get("layout.default", DEFAULT_HASHMAP));
        }
        return layout;
    }

    //to have write-access, we need this setter
    @Override
    public <L extends Serializable, T extends IPresentable> T setLayout(L l) {
        this.layout = (LinkedHashMap<String, String>) l;
        return (T) this;
    }

    /**
     * @return Returns the layoutConstraints.
     */
    @Override
    public LinkedHashMap<String, String> getLayoutConstraints() {
        if (layoutConstraints == null) {
            //LinkedHashmap not supported by simple-xml
            layoutConstraints =
                new LinkedHashMap<>(ENV.get("layout.constraints.default", DEFAULT_HASHMAP));
        }
        return layoutConstraints;
    }

    //to have write-access, we need this setter
    @Override
    public <L extends Serializable, T extends IPresentable> T setLayoutConstraints(L lc) {
        this.layoutConstraints = (LinkedHashMap<String, String>) lc;
        return (T) this;
    }

    @Override
    public <T extends IPresentable> T addLayoutConstraints(String name, Object value) {
        getLayoutConstraints().put(name, Util.asString(value));
        return (T) this;
    }
    
    @Override
    public int getType() {
        int t = super.getType();
        if (t == TYPE_DEPEND) {
            //TODO: implement value/data depend type
            t = TYPE_DATA;
        }
        return t;
    }
    @Override
    public int getStyle() {
        int s = super.getStyle();
        if (s == UNDEFINED) {
            //TODO: implement value/data depend type
            s = STYLE_DATA_FRAME;
        }
        return s;
    }
}
