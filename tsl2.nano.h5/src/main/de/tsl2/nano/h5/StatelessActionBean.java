package de.tsl2.nano.h5;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.simpleframework.xml.Transient;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.util.MethodUtil;
import de.tsl2.nano.specification.actions.Action;

/**
 * Wrapper Bean to define virtual beans without any attributes - but actions - mostly for openapi generated web client
 * classes to call the rest services.
 */
@SuppressWarnings({ "rawtypes" })
public class StatelessActionBean<T> extends Bean<T> {
    private static final String STATELESSACTIONBEAN_ISACTIONMETHOD = "!statelessactionbeanisactionmethod";

    @Transient // NOTE: the transient annotation tells the BeanDefinition that it has to provide Extension informations!
    Boolean actionsOnly = true;

    transient List<IAttribute> emptyAttributes = new LinkedList<>();

    public StatelessActionBean() {
    }

    public StatelessActionBean(Class<T> beanClass) {
        clazz = beanClass;
        name = getName(beanClass);
        getActions();
    }

    @Override
    public List<IAttribute> getAttributes(boolean readAndWriteAccess) {
        return emptyAttributes;
    }

    @Override
    public boolean isVirtual() {
        return true;
    }

    @Override
    public Bean<T> setInstance(T instance) {
        //Workaround to set only own instance
        this.instance = createInstance();
        return this;
    }

    @Override
    public Collection<IAction> getActions() {
        // copy of getActions() in BeanDefinition in cause of not able to call super.super
        if (actions == null) {
            actions = getActionsByClass();
        }
        return actions;
    }

    @Override
    protected boolean isActionMethod(Method m) {
    /** defines a configurable method to be changed in ENV properties and/or specifiedactions */
    return (boolean) Action.defineAndRun(STATELESSACTIONBEAN_ISACTIONMETHOD, this, "isPublicAndNotJdkMethod",
                Boolean.class, m);
    }

    /** default method used by specifiedaction to be called by {@link #isActionMethod(Method)} */
    public boolean isPublicAndNotJdkMethod(Method m) {
        return MethodUtil.isPublic(m) && !m.getDeclaringClass().getPackageName().startsWith("java");
    }

    @Override
    public IPresentable getPresentable() {
        IPresentable p = super.getPresentable();
        if (p.getIcon() == null) {
            p.setIcon("icons/go.png");
        }
        return p;
    }
}
