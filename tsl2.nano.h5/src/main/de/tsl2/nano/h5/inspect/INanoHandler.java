package de.tsl2.nano.h5.inspect;

import java.util.Map;
import java.util.SortedMap;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.h5.NanoHTTPD.Method;
import de.tsl2.nano.h5.navigation.IBeanNavigator;
import de.tsl2.nano.inspection.Inspector;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.serviceaccess.IAuthorization;

public interface INanoHandler extends Inspector {
    void onAuthentication(IAuthorization auth);
    void configuration(SortedMap<Object, Object> properties, Map<Class<?>, Object> services);
    public <PAGE, OUTPUT, T extends IPageBuilder<PAGE, OUTPUT>> T definePresentationType(T pageBuilder);
    void defineBeanDefinition(BeanDefinition<?> beanDef);
    void definePersistence(Persistence persistence);
    void actionBeforeHandler(IAction<?> action);
    void actionAfterHandler(IAction<?> action);
    void workflowHandler(IBeanNavigator workflow);
    void exceptionHandler(Exception ex);
    void requestHandler(String uri, Method m, Map<String, String> header, Map<String, String> parms, Map<String, String> files);
}
