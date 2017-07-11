/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 09.06.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.h5;

import java.math.BigDecimal;
import java.text.Format;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Transient;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

import de.tsl2.nano.action.IActivable;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.bean.def.GroupingPresentable;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.bean.def.IValueDefinition;
import de.tsl2.nano.bean.def.MapValue;
import de.tsl2.nano.bean.def.SecureAction;
import de.tsl2.nano.bean.def.Status;
import de.tsl2.nano.collection.TableList;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.NumberUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.incubation.specification.Pool;
import de.tsl2.nano.logictable.ICellVisitor;
import de.tsl2.nano.logictable.LogicForm;
import de.tsl2.nano.logictable.LogicTable;
import de.tsl2.nano.util.PrivateAccessor;

/**
 * Provides calculation sheets as tables with columns and rows. each cell is owned by an {@link MapValue} as BeanValue.
 * 
 * @author Tom
 * @version $Revision$
 */
public class CSheet extends Bean<Object> {

    /** serialVersionUID */
    private static final long serialVersionUID = 750996758733098834L;
    private static final Log LOG = LogFactory.getLog(CSheet.class);

    @Transient //mark this class as extension of BeanDefinition
    protected String logicFormFileName;
    transient protected LogicForm<? extends Format, Object> logicForm;

    static {//registere this class as wrapper for logicform. this will be used by Bean.class
        ENV.setProperty(LogicForm.class.getName() + Bean.BEANWRAPPER, CSheet.class.getName());
    }

    /**
     * constructor
     */
    protected CSheet() {
        super();
    }

    public CSheet(String name, int cols, int rows) {
        logicForm = new LogicForm<>(name, cols, rows);
        init();
    }

    public CSheet(String name, int cols, Object... rowValues) {
        logicForm = new LogicForm<>(name, cols);
        logicForm.addAll(false, rowValues);
        init();
    }

    /**
     * constructor
     * 
     * @param logicForm
     */
    public CSheet(LogicForm<? extends Format, Object> logicForm) {
        this.logicForm = logicForm;
    }

    protected void init() {
        //internal create attributes 
        allDefinitionsCached = false;
        attributeDefinitions = null;
        autoInit("test");
        getActions().clear();
        addAction(new SaveAction(this, "csheet.save"));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public List<IAttribute> getAttributes(boolean readAndWriteAccess) {
        if (!allDefinitionsCached) {
            if (attributeDefinitions == null) {
                attributeDefinitions = new LinkedHashMap<String, IAttributeDefinition<?>>();
            }
            logicForm.doOnValues(new ICellVisitor() {
                @Override
                public void visit(int row, int col, Object cell) {
                    if (hasAttribute(row, col)) {
                        String name = createAttributeName(row, col);
                        attributeDefinitions.put(name, new BeanValue<>(null, new MapValue(name, null, logicForm.getValueMap())));
                    }
                }
            });
            allDefinitionsCached = true;
        }
        return new ArrayList<IAttribute>(attributeDefinitions.values());
    }

    protected String createAttributeName(int row, int col) {
        return logicForm.createKey(row, col);
    }

    public Object getRowID(int row) {
        return logicForm.getRowID(row);
    }
    
    public Object get(int row, int col) {
        IValueDefinition attr = getAttribute(createAttributeName(row, col));
        Object e = attr.getValue();
        if (e != null && e.toString().startsWith(LogicTable.ERROR))
            if (attr instanceof AttributeDefinition)
                ((AttributeDefinition)attr).setStatus(new Status(new ManagedException(e.toString())));
        return e;
    }
    
    public void set(int row, int col, Object value) {
        getAttribute(createAttributeName(row, col)).setValue(value);
    }
    
    public void set(int row, Object... values) {
        for (int i = 0; i < values.length; i++) {
            getAttribute(createAttributeName(row, i)).setValue(values[i]);
        }
    }
    
    /**
     * @return Returns the logicForm.
     */
    public LogicForm<? extends Format, Object> getLogicForm() {
        return logicForm;
    }

    /**
     * @param logicForm The logicForm to set.
     */
    public void setLogicForm(LogicForm<? extends Format, Object> logicForm) {
        this.logicForm = logicForm;
    }

    @Override
    public Object save() {
        return save(null);
    }
    @Override
    protected Object save(Object bean) {
        toLogicForm();
        saveDefinition();
        return null;
    }

    private void toLogicForm() {
        int rc = logicForm.getRowCount();
        int cc = logicForm.getColumnCount();
        String s;
        Object v, oldValue;
        for (int i = 0; i < rc; i++) {
            for (int j = 0; j < cc; j++) {
                v = get(i, j);
                if (v != null) {
                    if (!(v instanceof BigDecimal))
                        if (NumberUtil.isNumber(v))
                            v = new BigDecimal(v.toString());
                }
                // get the pure cell value (not calculated)
                oldValue = logicForm.get(logicForm.getRowID(i))[j];
                if (v != null && v.equals(oldValue))
                    continue;
                //formulas have high priority
                if (Util.isEmpty(v) || !logicForm.isFormula(oldValue) || logicForm.isFormula(v))
                    logicForm.set(i, j, v);
            }
        }
    }

    @SuppressWarnings({ "rawtypes" })
    protected void fromLogicForm() {
        logicFormFileName = logicForm.getName();
        IPresentable p = getPresentable();
        p.setIcon("icons/table.png");
        if (p instanceof GroupingPresentable) {
            GroupingPresentable pres = (GroupingPresentable) p;
            pres.setGridWidth(logicForm.getColumnCount() * 3); // 3 gui-fields per attribute
            pres.setGridHeight(logicForm.getRowCount());
            logicForm.doOnValues(new ICellVisitor() {
                @Override
                public void visit(final int row, final int col, Object cell) {
                    if (hasAttribute(row, col)) {
                        IValueDefinition attr = getAttribute(createAttributeName(row, col));
                        Html5Presentable pp = (Html5Presentable) attr.getPresentation();
                        pp.setWidth(IPresentable.UNUSABLE);
                        pp.setEnabler(new DisableHeader(row, col));
                        if (row >= 0) {
                            pp.setLabel(null);
                        } else {
                            pp.setLabel(row >= 0 ? String.valueOf(logicForm.getRowID(row)) : null);
                            pp.setType(pp.TYPE_INPUT);
                        }
                    }
                }
            });
        } else {
            LOG.warn("bean of type CSheet should have a presentable of type GroupingPresentable: " + p);
        }

        // define presentable behaviour on label and enabling (headers with label but disabled)
    }

    protected boolean hasAttribute(int row, int col) {
        return col >= 0;
    }

    @Override
    @Persist
    protected void initSerialization() {
        fromLogicForm();
        logicForm.save(getDirectory());
        getActions().clear();
        super.initSerialization();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Commit
    protected void initDeserialization() {
        logicForm = TableList.load(getDirectory() + logicFormFileName + TableList.FILE_EXT, LogicForm.class);
        List<IAttribute> attrs = getAttributes();
        for (IAttribute a : attrs) {
            //TODO: didn't open access to internal attribute - why not?
            ((MapValue)new PrivateAccessor(a).member("attribute")).setMap(logicForm.getValueMap());
        }
        addAction(new SaveAction(this, "csheet.save"));
    }

    protected String getDirectory() {
        return ENV.get(Pool.class).getDirectory();
    }
    
    @Override
    public String toString() {
        return getName();
    }
}

/**
 * To be serializable, we had to extract a full class instead of using the shorter inline class. Only used at
 * {@link Bean#createSaveAction(Object, String)}.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked" })
class SaveAction extends SecureAction<CSheet> {
    /** serialVersionUID */
    private static final long serialVersionUID = -5782021148268245939L;
    private CSheet sheet;
    
    protected SaveAction() {}
    
    public SaveAction(CSheet sheet, String id) {
        super(id, id, id, 0);
        this.sheet = sheet;
    }

    public SaveAction(CSheet sheet,
            String id,
            String shortDescription,
            String longDescription,
            int actionMode) {
        super(id, shortDescription, longDescription, actionMode);
        this.sheet = sheet;
    }

    @Override
    public CSheet action() throws Exception {
        return (CSheet) sheet.save();
    }

    @Override
    public String getImagePath() {
        return "icons/save.png";
    }
}

/**
 * used by presenter (not as inner class in cause of problems of serialization) for enabling
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
class DisableHeader implements IActivable {
    /** serialVersionUID */
    private static final long serialVersionUID = 1080676348806158498L;

    @Attribute
    private int col;
    @Attribute
    private int row;

    /**
     * constructor
     */
    protected DisableHeader() {
    }

    public DisableHeader(int row, int col) {
        this.row = row;
        this.col = col;
    }

    @Override
    public boolean isActive() {
        return row >= 0 && col >= 0;
    }
}
