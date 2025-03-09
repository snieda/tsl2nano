package de.tsl2.nano.h5;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.simpleframework.xml.Transient;

import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.util.MethodUtil;

@SuppressWarnings("rawtypes")
public class StatelessActionBean<T> extends Bean<T> {
    @Transient // NOTE: the transient annotation tells the BeanDefinition that it has to provide Extension informations!
    Boolean actionsOnly = true;

    transient List<IAttribute> emptyAttributes = new LinkedList<>();

    public StatelessActionBean() {
    }
    public StatelessActionBean(Class<T> beanClass) {
        clazz = beanClass;
        name = getName(beanClass);
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
    protected boolean isActionMethod(Method m) {
        // TODO: move to SpecifiedAction
        return MethodUtil.isPublic(m) && !m.getDeclaringClass().getPackageName().startsWith("java");
    }
}
