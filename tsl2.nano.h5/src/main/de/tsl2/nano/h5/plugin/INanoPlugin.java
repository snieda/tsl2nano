package de.tsl2.nano.h5.plugin;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.java_websocket.WebSocket;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.h5.NanoH5Session;
import de.tsl2.nano.h5.NanoHTTPD.Method;
import de.tsl2.nano.h5.NanoHTTPD.Response;
import de.tsl2.nano.h5.navigation.IBeanNavigator;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.persistence.PersistenceClassLoader;
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
    /**
     * change the default properties of the application. see 'environment.xml' for a list of all keys and their defaults
     */
    void configuration(SortedMap<Object, Object> properties, Map<Class<?>, Object> services);

    /** do some inspections on authentication. The auth object is pre-filled */
    void onAuthentication(IAuthorization auth);

    /**
     * before an authentication, the properties for the jpa persistence.xml will be defined. You can change these
     * settings before a new session will be started
     */
    void definePersistence(Persistence persistence);

    /** do some initializings on the database. the connection-info is inside the persistence object */
    void databaseGenerated(Persistence persistence);
    
    /** do some initializings on the jar-file. the jar-file-name is inside the persistence object */
    void beansGenerated(Persistence persistence);
    
    /**
     * The Html page-builder defines the creation of each response. you must return an instance of pageBuilder (null
     * will result in errors)!
     */
    <PAGE, OUTPUT, T extends IPageBuilder<PAGE, OUTPUT>> T definePresentationType(T pageBuilder);

    /**
     * for each bean type (belonging to an entity class) the default handling can be changed. the output can be found at
     * the presentation directory
     */
    void defineBeanDefinition(BeanDefinition<?> beanDef);

    /** before running the requested user interaction, you can inspect the action that will be called */
    void actionBeforeHandler(IAction<?> action);

    /** after running the requested user interaction, you can inspect the action that was called */
    default void actionAfterHandler(IAction<?> action) {}

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

    /** on each html response you are able to manipulate the dom transformed html response string */
	default String manipulateHtmlResponse(String html) {
		return html;
	}

	/** lets you change the response object - perhaps adding header values etc. */
	default Response handleResponse(Response response) {
		return response;
	}

	/** do anything on receiving a new message from a websocket client */
	default void handleWebSocketMessage(WebSocket conn, String msg) {}

	/** do anything on receiving a new message from a websocket client */
	default void handleWebSocketMessage(WebSocket conn, ByteBuffer message) {}

	/** the request has entered the session to work on. all request values were transferred to the workflow bean */
	default void handleSessionRequest(NanoH5Session nanoH5Session, Map<String, String> parms) {}

    /**
     * @see BeanContainer.createBeanContainer()
     * should create and register the BeanContainer by calling BeanContainer.initServiceActions(...).
     * the BeanContainer provides all data. implement this method only, if you don't want to get the data
     * through a relational database.
     * e.g. getting the bean classes from an openapi implementation and the data through a rest service or a nosql database.
     * if this method returns null, the origin method will be invoked with its  standard implementation. 
     * returning a list of bean/model clesses, the origin method wont be called - you are responsable to create the BeanContainer.
     * @param persistence persistence properties - only usable on relational databases
     * @param runtimeClassloader nanoh5 classloader
     * @return list of bean/model classes or null
     */
    @SuppressWarnings("rawtypes")
    default List<Class> createBeanContainer(final Persistence persistence, PersistenceClassLoader runtimeClassloader) {return null;}
}
