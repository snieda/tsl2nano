package de.tsl2.nano.h5;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.h5.NanoHTTPD.Method;
import de.tsl2.nano.h5.navigation.IBeanNavigator;
import de.tsl2.nano.h5.plugin.INanoPlugin;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.serviceaccess.IAuthorization;

/**
 * Tries to prepare some unit-test specific environments on NanoH5
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class NanoH5UnitPlugin implements INanoPlugin, ENVTestPreparation {

    private static boolean enabled;

    @Override
    public void configuration(SortedMap<Object, Object> properties, Map<Class<?>, Object> services) {
    }

    @Override
    public void onAuthentication(IAuthorization auth) {
    }

    @Override
    public void definePersistence(Persistence persistence) {
    }


    @Override
    public void databaseGenerated(Persistence persistence) {
        //copy the jarfile to the workspace classpath
//        FileUtil.copy(persistence.jarFileInEnvironment(), TARGET_DIR + "classes/" + persistence.jarFileInEnvironment());
    }

    @Override
    public void beansGenerated(Persistence persistence) {
        //copy META-INF/persistence.xml to the workspace classpath
        //and set jarfile from origin user.dir
        String persistenceXML = null;
        try {
            persistence.setJarFile(Persistence.FIX_PATH + new File(persistence.jarFileInEnvironment()).getAbsolutePath());
            persistenceXML = persistence.save();
        } catch (IOException e) {
            ManagedException.forward(e);
        }
        FileUtil.writeBytes(persistenceXML.getBytes(), "classes/" + Persistence.FILE_PERSISTENCE_XML, false);
    }

    @Override
    public <PAGE, OUTPUT, T extends IPageBuilder<PAGE, OUTPUT>> T definePresentationType(T pageBuilder) {
        return pageBuilder;
    }

    @Override
    public void defineBeanDefinition(BeanDefinition<?> beanDef) {
    }

    @Override
    public void actionBeforeHandler(IAction<?> action) {
    }

    @Override
    public void actionAfterHandler(IAction<?> action) {
    }

    @Override
    public void workflowHandler(IBeanNavigator workflow) {
    }

    @Override
    public void exceptionHandler(Exception ex) {
    }

    @Override
    public void requestHandler(String uri,
            Method m,
            Map<String, String> header,
            Map<String, String> parms,
            Map<String, String> files) {
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    public static void setEnabled(boolean e) {
        enabled = e;
    }
}
