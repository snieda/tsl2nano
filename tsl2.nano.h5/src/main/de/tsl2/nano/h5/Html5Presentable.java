package de.tsl2.nano.h5;

import static de.tsl2.nano.h5.HtmlUtil.ATTR_HEIGHT;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_WIDTH;

import java.util.LinkedHashMap;

import de.tsl2.nano.Environment;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.bean.def.Presentable;
/**
 * Hmtl5-specialized {@link de.tsl2.nano.bean.def.Presentable}. Not possible to be handled as inner class, because
 * of xml-serialization.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Html5Presentable extends Presentable {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    protected Html5Presentable() {
    }

    public Html5Presentable(AttributeDefinition<?> attr) {
        super(attr);
    }

    @Override
    public int getWidth() {
        String w = layout(ATTR_WIDTH);
        return w != null ? Integer.valueOf(w) : UNDEFINED;
    }

    @Override
    public int getHeight() {
        String h = layout(ATTR_HEIGHT);
        return h != null ? Integer.valueOf(h) : UNDEFINED;
    }

    /**
     * @return Returns the layout.
     */
    public LinkedHashMap<String, String> getLayout() {
        if (layout == null) {
            //LinkedHashmap not supported by simple-xml
            layout = Environment.get("default.layout", new LinkedHashMap<String, String>());
        }
        return (LinkedHashMap<String, String>) layout;
    }

    //to have write-access, we need this setter
    public <T extends IPresentable> T setLayout(LinkedHashMap<String, String> l) {
        this.layout = l;
        return (T) this;
    }

    /**
     * @return Returns the layoutConstraints.
     */
    public LinkedHashMap<String, String> getLayoutConstraints() {
        if (layoutConstraints == null) {
            //LinkedHashmap not supported by simple-xml
            layoutConstraints = Environment.get("default.layoutconstaints", new LinkedHashMap<String, String>());
        }
        return (LinkedHashMap<String, String>) layoutConstraints;
    }

    //to have write-access, we need this setter
    public <T extends IPresentable> T setLayoutConstraints(LinkedHashMap<String, String> lc) {
        this.layoutConstraints = lc;
        return (T) this;
    }
}
