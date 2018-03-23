package de.tsl2.nano.h5.plugin;

import java.util.Map;
import java.util.SortedMap;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.h5.NanoHTTPD.Method;
import de.tsl2.nano.h5.navigation.IBeanNavigator;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.plugin.Plugin;
import de.tsl2.nano.serviceaccess.IAuthorization;

/**
 * NanoH5 Plugin interface to define an own NanoH5 Application through callbacks invoked on most important entry
 * points. All classpath-implmenetations of this interface will be found by the framework and will be invoked on the
 * instruction points.
 * <p/>
 * At least one implementation should have a main function calling
 * 
 * <pre>
 * Main.startApplication(NanoH5.class, null, args);
 * </pre>
 * 
 * Each method will be called with parameters, pre-filled and provided by the framework.
 * <p>
 * To enhance the the Html-Output, see {@link IDOMDecorator}.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public interface INanoPlugin extends Plugin {
    /** do some inspections on authentication. The auth object is pre-filled */
    void onAuthentication(IAuthorization auth);

    /**
     * change the default properties of the application. see 'environment.xml' for a list of all keys and their defaults
     */
    void configuration(SortedMap<Object, Object> properties, Map<Class<?>, Object> services);

    /**
     * The Html page-builder defines the creation of each response. you must return an instance of pageBuilder (null
     * will result in errors)!
     */
    public <PAGE, OUTPUT, T extends IPageBuilder<PAGE, OUTPUT>> T definePresentationType(T pageBuilder);

    /**
     * for each bean type (belonging to an entity class) the default handling can be changed. the output can be found at
     * the presentation directory
     */
    void defineBeanDefinition(BeanDefinition<?> beanDef);

    /**
     * before an authentication, the properties for the jpa persistence.xml will be defined. You can change these
     * settings before a new session will be started
     */
    void definePersistence(Persistence persistence);

    /** before running the requested user interaction, you can inspect the action that will be called */
    void actionBeforeHandler(IAction<?> action);

    /** after running the requested user interaction, you can inspect the action that was called */
    void actionAfterHandler(IAction<?> action);

    /**
     * on creating a new session, the workflow / navigation stack is defined by loading from 'workflow.xml' or if not
     * found by providing all entity beans in a list. This workflow can be changed by your inspector.
     */
    void workflowHandler(IBeanNavigator workflow);

    void exceptionHandler(Exception ex);

    /** on each new http request, you can inspect and change the header, parameters and files */
    void requestHandler(String uri,
            Method m,
            Map<String, String> header,
            Map<String, String> parms,
            Map<String, String> files);
}
