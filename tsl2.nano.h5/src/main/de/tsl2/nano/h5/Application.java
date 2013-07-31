package de.tsl2.nano.h5;

import static de.tsl2.nano.h5.HtmlUtil.BTN_ASSIGN;
import static de.tsl2.nano.h5.HtmlUtil.BTN_CANCEL;
import static de.tsl2.nano.h5.HtmlUtil.BTN_SUBMIT;
import static de.tsl2.nano.util.bean.def.BeanPresentationHelper.KEY_FILTER_FROM_LABEL;
import static de.tsl2.nano.util.bean.def.BeanPresentationHelper.KEY_FILTER_TO_LABEL;
import static de.tsl2.nano.util.bean.def.IBeanCollector.MODE_CREATABLE;
import static de.tsl2.nano.util.bean.def.IBeanCollector.MODE_EDITABLE;
import static de.tsl2.nano.util.bean.def.IBeanCollector.MODE_MULTISELECTION;
import static de.tsl2.nano.util.bean.def.IBeanCollector.MODE_SEARCHABLE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Stack;

import org.apache.commons.logging.Log;

import de.tsl2.nano.Environment;
import de.tsl2.nano.Messages;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.collection.ListSet;
import de.tsl2.nano.collection.MapUtil;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.execution.ScriptUtil;
import de.tsl2.nano.format.RegExpFormat;
import de.tsl2.nano.log.LogFactory;
import de.tsl2.nano.persistence.HibernateBeanContainer;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.persistence.PersistenceClassLoader;
import de.tsl2.nano.script.ScriptTool;
import de.tsl2.nano.service.util.BeanContainerUtil;
import de.tsl2.nano.util.DateUtil;
import de.tsl2.nano.util.FileUtil;
import de.tsl2.nano.util.NumberUtil;
import de.tsl2.nano.util.StringUtil;
import de.tsl2.nano.util.bean.BeanContainer;
import de.tsl2.nano.util.bean.IBeanContainer;
import de.tsl2.nano.util.bean.def.Bean;
import de.tsl2.nano.util.bean.def.BeanCollector;
import de.tsl2.nano.util.bean.def.BeanDefinition;
import de.tsl2.nano.util.bean.def.BeanPresentationHelper;
import de.tsl2.nano.util.bean.def.BeanValue;
import de.tsl2.nano.util.bean.def.IBeanCollector;
import de.tsl2.nano.util.bean.def.IPageBuilder;
import de.tsl2.nano.util.bean.def.IPresentable;
import de.tsl2.nano.util.bean.def.SecureAction;

/**
 * An Application of subclassing NanoHTTPD to make a custom HTTP server.
 * 
 * <pre>
 * TODO:
 * - Bean-->BeanValue-->getColumnDefinition() --> Table(columns)
 * - PageBuilder --> Bean.Presentable
 * - Navigation
 * - Verbindung/Abgrenzung BeanContainer
 * - evtl mini-browser wie lynx einbinden
 * - BeanContainer mit cache fuer tests
 * </pre>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class Application extends NanoHTTPD {
    IPageBuilder<?, String> builder;
    Stack<BeanDefinition<?>> navigation = new Stack<BeanDefinition<?>>();
    BeanDefinition<?> model;
    int port;
    Response response;
    ClassLoader appstartClassloader;
    private static final Log LOG = LogFactory.getLog(Application.class);

    private static final String DEGBUG_HTML_FILE = "application.html";
    private static final String START_PAGE = "Start";
    private static final int OFFSET_FILTERLINES = 2;

    public Application() throws IOException {
        this(8067, Environment.get(IPageBuilder.class), new Stack<BeanDefinition<?>>());
    }

    public Application(int port, IPageBuilder<?, String> builder, Stack<BeanDefinition<?>> navigation) throws IOException {
        super(port, new File(Environment.getConfigPath()));
        this.port = port;
        this.builder = builder;
        this.navigation = navigation;
        ResourceBundle bundle = ResourceBundle.getBundle(this.getClass().getPackage().getName() + ".messages",
            Locale.getDefault(),
            Thread.currentThread().getContextClassLoader());
        Messages.registerBundle(bundle, false);
        appstartClassloader = Thread.currentThread().getContextClassLoader();
    }

    /**
     * starts application and shows initial html page
     */
    public void start() {
        try {
            LOG.info(System.getProperties());
            createStartPage(DEGBUG_HTML_FILE);
            LOG.info("Listening on port " + port + ". Hit Enter to stop.\n");
            if (System.getProperty("os.name").startsWith("Windows"))
                ScriptUtil.executeRegisteredWindowsPrg("application.html");
            System.in.read();
        } catch (Exception ioe) {
            LOG.error("Couldn't start server:", ioe);
            System.exit(-1);
        }
    }

    /**
     * createStartPage
     * 
     * @param resultHtmlFile
     */
    protected void createStartPage(String resultHtmlFile) {
        InputStream stream = Environment.getResource("start.template");
        String startPage = String.valueOf(FileUtil.getFileData(stream, null));
        startPage = StringUtil.insertProperties(startPage,
            MapUtil.asMap("url", "http://localhost:" + port, "name", Environment.getName()));
        FileUtil.writeBytes(startPage.getBytes(), resultHtmlFile, false);
    }

    /**
     * setNavigationModel
     * 
     * @param navigationModel new navigation model
     * @return application itself
     */
    public Application setNavigationModel(Stack<BeanDefinition<?>> navigationModel) {
        navigation = navigationModel;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response serve(String uri, String method, Properties header, Properties parms, Properties files) {
        LOG.info(String.format("serving request:\n\turi: %s\n\tmethod: %s\n\theader: %s\n\tparms: %s\n\tfiles: %s",
            uri,
            method,
            header,
            parms,
            files));
        String msg = "[undefined]";
        try {
            Environment.assignClassloaderToCurrentThread();
            if (method.equals("GET") && uri.contains("."))
                return super.serve(uri, method, header, parms, files);
            //WORKAROUND for uri-problem
            String referer = header.getProperty("referer");
            if (parms.containsKey(IAction.CANCELED) || (method.equals("POST") && referer != null && uri.length() > 1 && referer.contains(uri)))
                uri = "/";
            BeanDefinition<?> linkToModel = evalLinkToModel(uri);
            Object userResponse = null;
            //direct link to another page/bean
            //selection-link-number in beancollector
            Number uriLinkNumber = linkToModel != null ? null : NumberUtil.extractNumber(uri.substring(1));
            //form-button clicked - or first page
            if (!parms.isEmpty() || linkToModel != null || uriLinkNumber != null || response == null) {
                if (linkToModel != null) {
                    userResponse = linkToModel;
                } else {
                    userResponse = processInput(uri, parms, uriLinkNumber);
                }
                if (userResponse instanceof String && !userResponse.equals(IAction.CANCELED)) {
                    if (!msg.contains("<html>"))
                        msg = "<html><body><pre>" + userResponse + "</pre></body></html>";
                } else {
                    msg = getNextPage(userResponse);
                }
                response = new NanoHTTPD.Response(HTTP_OK, MIME_HTML, msg);
            } else {
                //do nothing...
            }
        } catch (Exception e) {
            RuntimeException ex = ForwardedException.toRuntimeEx(e, true);
            msg = refreshPage(ex.getMessage());
            response = new NanoHTTPD.Response(HTTP_BADREQUEST, MIME_HTML, msg);
        }
        //TODO: eliminate bug in NanoHTTPD not resetting uri...
//        header.clear();
//        response.header.remove(uri);
        return response;
    }

    /**
     * evaluates, if a navigation item (bean) was clicked.
     * 
     * @param uri uri to analyze
     * @return navigation bean or null
     */
    private BeanDefinition<?> evalLinkToModel(String uri) {
        BeanDefinition<?> linkBean = null;
        String link = StringUtil.substring(uri, "/", null, true);
        //reset-link clicked? recreate the navigation
        if (link.equals(START_PAGE)) {
            reset();
            return null;
        }
        for (BeanDefinition<?> bean : navigation) {
            if (bean.getName().equals(link)) {
                linkBean = bean;
                break;
            }
        }
        return linkBean;
    }

    private String refreshPage(String message) {
        return builder.build(model, message, true);
    }

    /**
     * pops the next model from navigation stack (see {@link #getNextModel(Object)} and asks the pagebuilder to create a
     * full html page for the given model.
     * 
     * @param returnCode return code of last request
     * @return html string
     */
    private String getNextPage(Object returnCode) {
        return builder.build(getNextModel(returnCode), "", true, getNavigationQueue());
    }

    /**
     * navigation stack
     * 
     * @return current navigation queue wrapped into an object array
     */
    private BeanDefinition<?>[] getNavigationQueue() {
        BeanDefinition<?>[] beans = new BeanDefinition[navigation.size()];
        for (int i = 0; i < navigation.size(); i++) {
            beans[i] = navigation.get(i);
        }
        return beans;
    }

    /**
     * process user input
     * 
     * @param uri page uri
     * @param parms response parameter
     * @param uriLinkNumber if model is a bean-collector, it is the selected element number
     * @return user response object. may be {@link IAction#CANCELED} any saved or selected object or null.
     */
    private Object processInput(String uri, Properties parms, Number uriLinkNumber) {
//        if (parms.containsKey(PageBuilder.COMMAND_RESTART)) {
//            stop();
//            main(null);
//        }
        Object responseObject = null;
        if (parms.containsKey(IAction.CANCELED))
            return IAction.CANCELED;

        convertDates(parms);

        //refresh bean values
        if (model instanceof Bean) {
            Bean vmodel = (Bean) model;
            for (String p : parms.stringPropertyNames()) {
                if (vmodel.hasAttribute(p)) {
                    vmodel.setParsedValue(p, parms.getProperty(p));
                }
            }
        }
        //follow links or fill selected items
        if (model instanceof BeanCollector) {
            //follow given link
            if (uriLinkNumber != null) {
                BeanCollector collector = (BeanCollector) model;
                Collection data = collector.getBeanFinder().getData();
                ListSet listSet = CollectionUtil.asListSet(data);
                //visible numbers starting with '1', but indexes starting with '0'
                responseObject = new Bean(listSet.get(uriLinkNumber.intValue() - 1));
                return responseObject;
            } else {
                if (!isCanceled(parms) && provideSelection((BeanCollector) model, parms)) {
                    if (isReturn(parms)) {
                        responseObject = null;
                    } else if (isOpenAction(parms, (BeanCollector) model)){
                        //normally, after a selection the navigation object will be hold on stack
                        if (Environment.get("application.edit.multiple", true))
                            responseObject = putSelectionOnStack((BeanCollector) model);
                        else
                            responseObject = model;
                    }
                }
            }
        }
        //start the actions
        Collection<IAction> actions = null;
        if (model != null) {
            actions = new ArrayList<IAction>();
            if (model.getActions() != null)
                actions.addAll(model.getActions());
            actions.addAll(model.getPresentationHelper().getPresentationActions());
            if (model.isMultiValue()) {
                actions.addAll(((BeanCollector) model).getColumnSortingActions());
            }
        }
        if (actions != null) {
            for (Object k : parms.keySet()) {
                String p = (String) k;
                IAction<?> action = getAction(actions, p);
                if (action != null) {
                    if (model.isMultiValue() && isSearchRequest(action.getId(), (BeanCollector<?, ?>) model)) {
                        responseObject = processSearchRequest(parms, (BeanCollector<?, ?>) model);
                    } else {
                        /*
                         * submit/assign and cancel will not push a new element to the navigation stack!
                         * TODO: refactore access to names ('reset' and 'save')
                         */
                        Object result = action.activate();
                        if (result != null && responseObject != IAction.CANCELED && !action.getId().endsWith("save")) {
                            responseObject = result;
                        } else if (action.getId().endsWith("reset")) {
                            responseObject = model;
                        } else {
//                        action.activate();
                            return responseObject;
                        }
                    }
                } else {
                    if (p.endsWith(IPresentable.POSTFIX_SELECTOR)) {
                        String n = StringUtil.substring(p, null, IPresentable.POSTFIX_SELECTOR);
                        final BeanValue assignableAttribute = (BeanValue) model.getAttribute(n);
                        responseObject = assignableAttribute.connectToSelector(model);
                    }
                }
            }
        }
        return responseObject;
    }

    private BeanDefinition<?> putSelectionOnStack(BeanCollector c) {
        Collection selection = (Collection) c.getSelectionProvider().getValue();
        BeanDefinition<?> firstElement = null;
        BeanDefinition<?> bean;
        for (Object object : selection) {
            bean = (BeanDefinition<?>) (object instanceof BeanDefinition ? object : Bean.getBean((Serializable) object));
            //don't add the first element, see behaviour in getNextModel()
            if (firstElement != null) {
                navigation.add(bean);
            } else {
                firstElement = bean;
            }
        }
        return firstElement;
    }

    private IAction<?> getAction(Collection<IAction> actions, String id) {
        if (actions != null) {
            for (IAction a : actions) {
                if (a.getId().equals(id))
                    return a;
            }
        }
        return null;
    }

    protected boolean isCanceled(Properties parms) {
        return parms.containsKey(BTN_CANCEL);
    }

    protected boolean isReturn(Properties parms) {
        return isCanceled(parms) || parms.containsKey(BTN_ASSIGN) || parms.containsKey(BTN_SUBMIT);
    }

    protected <T> boolean isOpenAction(Properties parms, BeanCollector<?, T> model) {
        for (Object k : parms.keySet()) {
            if (isOpenAction((String)k, model))
                return true;
        }
        return false;
    }

    protected <T> boolean isOpenAction(String actionId, BeanCollector<?, T> model) {
        return actionId.equals(BeanContainer.getActionId(model.getClazz(), true, "open"));
    }


    protected <T> boolean isSearchRequest(Properties parms, BeanCollector<?, T> model) {
        for (Object k : parms.keySet()) {
            if (isSearchRequest((String)k, model))
                return true;
        }
        return false;
    }

    protected <T> boolean isSearchRequest(String actionId, BeanCollector<?, T> model) {
        return actionId.equals(BeanContainer.getActionId(model.getClazz(), true, "search"));
    }

    protected <T> BeanCollector<?, T> processSearchRequest(Properties parms, BeanCollector<?, T> model) {
        //fill the search values
        Bean<?> filterBean = model.getBeanFinder().getFilterRange();
        if (filterBean != null) {
            Bean<?> from = (Bean<?>) filterBean.getValueAsBean("from");
            Bean<?> to = (Bean<?>) filterBean.getValueAsBean("to");

            from.getPresentationHelper().change(BeanPresentationHelper.PROP_DOVALIDATION, false);
            from.setAttributeFilter("name");
            from.setName(null);
            to.getPresentationHelper().change(BeanPresentationHelper.PROP_DOVALIDATION, false);
            to.setAttributeFilter("name");
            to.setName(null);

            for (String p : parms.stringPropertyNames()) {
                String rowName = StringUtil.substring(p, null, ".", true);
                String colName = StringUtil.substring(p, ".", null, true);
                if (from.getPresentationHelper().prop(KEY_FILTER_FROM_LABEL).equals(rowName) && from.hasAttribute(colName)) {
                    from.setParsedValue(colName, parms.getProperty(p));
                } else if (to.getPresentationHelper().prop(KEY_FILTER_TO_LABEL).equals(rowName) && to.hasAttribute(colName)) {
                    to.setParsedValue(colName, parms.getProperty(p));
                } else if (from.hasAttribute(colName)) {
                    from.setParsedValue(colName, parms.getProperty(p));
                    to.setParsedValue(colName, parms.getProperty(p));
                }
            }
            model.getSearchAction().activate();
        }
        //a search request will show the same search panel again - but with filtered data.
        return model;
    }

    /**
     * converts the standard date format yyyy-MM-dd to the locale specific date format - to be parseable
     * 
     * @param parms
     */
    private void convertDates(Properties parms) {
        String v;
        for (String p : parms.stringPropertyNames()) {
            v = parms.getProperty(p);
            if (v != null && v.matches(RegExpFormat.FORMAT_DATE_SQL))
                parms.setProperty(p, DateUtil.getFormattedDate(DateUtil.getDateSQL(v)));
        }
    }

    /**
     * evaluates all selected checkboxes of a table (given by a beancollector) and fills a collection with selected
     * table-beans
     * 
     * @param c model, holding table data
     * @param parms user response
     * @return all selected table beans
     */
    Collection<Object> getSelectedElements(BeanCollector c, Properties parms) {
        Collection<Object> selectedElements = new LinkedList<Object>();
        Number selection;
        Collection<?> data = c.getCurrentData();
        for (String p : parms.stringPropertyNames()) {
            selection = NumberUtil.extractNumber(p);
            if (selection != null && "on".equalsIgnoreCase(parms.getProperty(p))) {
                //evaluate selected element to be used by an action
                Object selectedBean = CollectionUtil.getList(data.iterator())
                    .get(selection.intValue() - (c.hasMode(MODE_SEARCHABLE) ? OFFSET_FILTERLINES : 0));
                selectedElements.add(selectedBean);
            }
        }
        return selectedElements;
    }

    /**
     * uses {@link #getSelectedElements(BeanCollector, Properties)} to provide the selection to the selectionprovider of
     * the beancollector.
     * 
     * @param c table model
     * @param parms user response
     */
    boolean provideSelection(BeanCollector c, Properties parms) {
        Collection<Object> elements = getSelectedElements(c, parms);
        c.getSelectionProvider().setValue(elements);
        return !c.getSelectionProvider().isEmpty();
//
//        if (elements.size() > 0)
//            navigation.push(new Bean(elements.iterator().next()));
    }

    /**
     * the next model may be a new bean model, if the response object is not null, not in the current navigation stack
     * and not a cancel action.
     * 
     * @param userResponseObject result of {@link #processInput(String, Properties, Number)}
     * @return next bean model or null
     */
    private BeanDefinition<?> getNextModel(Object userResponseObject) {
        boolean isOnWork = false;
        boolean goBack = userResponseObject == null || userResponseObject == IAction.CANCELED;
        if (!goBack) {
            BeanDefinition<?> userResponseBean = (BeanDefinition<?>) (userResponseObject instanceof BeanDefinition<?> ? userResponseObject
                : Bean.getBean((Serializable) userResponseObject));
            isOnWork = navigation.contains(userResponseBean);
            if (!isOnWork) //--> go forward
                return (model = navigation.push(userResponseBean));
            else {
                if (model != userResponseBean) {
                    while (!userResponseBean.equals(navigation.peek()))
                        navigation.pop();
                    return model = navigation.peek();
                }
            }

        }
        //go back
        if (!isOnWork && response != null)//checking to be not the first page
            navigation.pop();

        model = navigation.size() > 0 ? navigation.peek() : null;
        //workaround for a canceled new action
        if (userResponseObject == IAction.CANCELED && model instanceof IBeanCollector) {
            removeUnpersistedNewEntities((BeanCollector) model);
        }
        return model;
    }

    /**
     * workaround for 'new' action on a beancollector followed by a cancel action - means the new instance is added to
     * the beancollector, but the cancel action has to remove the instance.
     * 
     * @param collector collector holding a canceled/transient instance.
     */
    @SuppressWarnings("rawtypes")
    private void removeUnpersistedNewEntities(BeanCollector collector) {
        if (!BeanContainer.instance().isPersistable(collector.getBeanFinder().getType()))
            return;
        Collection currentData = collector.getCurrentData();
        for (Iterator iterator = currentData.iterator(); iterator.hasNext();) {
            Object item = iterator.next();
            if (BeanContainer.isTransient(item))
                iterator.remove();
        }
    }

    /**
     * main entry
     * 
     * @param args launching args
     */
    public static void main(String[] args) {
        //TODO: create an Argumentator
        try {
            if (args.length == 1) {
                if (args[0].matches(".*(\\?|help|man)")) {
                    System.out.println("Please provide a path for application configurations!");
                    return;
                } else {
                    System.setProperty(Environment.KEY_CONFIG_PATH, args[0]);
                    File file = new File(args[0]);
                    Environment.setProperty(Environment.KEY_CONFIG_PATH, file.getAbsolutePath() + "/");
                }
            }
            initServices();
            new Application().setNavigationModel(createGenericNavigationModel()).start();
        } catch (IOException e) {
            ForwardedException.forward(e);
        }
    }

    private static void initServices() {
        Html5Presentation pageBuilder = new Html5Presentation();
        Environment.addService(BeanPresentationHelper.class, pageBuilder);
        Environment.addService(IPageBuilder.class, new Html5Presentation());
    }

    /**
     * reads all classes of 'beanjar', creates a root beancollector holding a collection of this classes.
     * 
     * @param beanjar jar to resolve the desired entities from
     * @return navigation stack holding a beancollector for all entity classes inside beanjar
     */
    private static Stack<BeanDefinition<?>> createGenericNavigationModel() {

        BeanContainer.initEmtpyServiceActions();
        /*
         * create the presentable navigation stack
         */
        LOG.debug("creating navigation stack");

        Bean<?> login = createLogin();

        Stack<BeanDefinition<?>> navigationModel = new Stack<BeanDefinition<?>>();
        navigationModel.push(Bean.getBean(START_PAGE));
        navigationModel.push(login);
        return navigationModel;
    }

    private static Bean<?> createLogin() {
        final Persistence persistence = Persistence.current();
        Bean<?> login = new Bean(persistence);
        login.getPresentationHelper().change(BeanPresentationHelper.PROP_NULLABLE, false);
        login.addAction(new SecureAction<Object>("tsl2nano.login.ok") {
            @Override
            public Object action() throws Exception {
                persistence.save();
                if (!new File(persistence.getJarFile()).exists())
                    generateJarFile(persistence.getJarFile());

                PersistenceClassLoader runtimeClassloader = new PersistenceClassLoader(new URL[0],
                    Thread.currentThread().getContextClassLoader());
                runtimeClassloader.addLibraryPath(Environment.getConfigPath());
                Thread.currentThread().setContextClassLoader(runtimeClassloader);
                Environment.addService(ClassLoader.class, runtimeClassloader);

                List<Class> beanClasses = runtimeClassloader.loadBeanClasses(persistence.getJarFile(), null);
                Environment.setProperty("loadedBeanTypes", beanClasses);

                if (Environment.get("use.applicationserver", false))
                    BeanContainerUtil.initGenericServices(runtimeClassloader);
                else
                    HibernateBeanContainer.initHibernateContainer(runtimeClassloader);

                Environment.addService(IBeanContainer.class, BeanContainer.instance());

                LOG.debug("creating collector for: ");
                List types = new ArrayList(beanClasses.size());
                for (Class cls : beanClasses) {
                    LOG.debug("creating collector for: " + cls);
                    BeanCollector collector = BeanCollector.getBeanCollector(cls, null, MODE_EDITABLE | MODE_CREATABLE
                        | MODE_MULTISELECTION
                        | MODE_SEARCHABLE, null);
//                    collector.setPresentationHelper(new Html5Presentation(collector));
                    types.add(collector);
                }
                /*
                 * Perhaps show the script tool to do direct sql or ant
                 */
                if (Environment.get("application.show.scripttool", true)) {
                    ScriptTool tool = new ScriptTool();
                    Bean beanTool = new Bean(tool);
                    beanTool.setAttributeFilter("text", "result", "resourceFile", "selectionAction");
                    beanTool.getAttribute("text").getPresentation().setType(IPresentable.TYPE_INPUT_MULTILINE);
                    beanTool.getAttribute("result").getPresentation().setType(IPresentable.TYPE_TABLE);
                    beanTool.getAttribute("sourceFile").getPresentation().setType(IPresentable.TYPE_ATTACHMENT);
                    beanTool.getAttribute("selectedAction").setRange(tool.availableActions());
                    beanTool.addAction(tool.runner());
                    types.add(beanTool);
                }
                BeanCollector root = new BeanCollector(BeanCollector.class, types, MODE_EDITABLE | MODE_SEARCHABLE, null);
                root.setName(StringUtil.toFirstUpper(StringUtil.substring(persistence.getJarFile(), "/", ".jar", true)));
                root.setAttributeFilter("name");
                root.getAttribute("name").setFormat(new Format() {
                    @Override
                    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                        String name = StringUtil.substring((String) obj, null, BeanCollector.POSTFIX_COLLECTOR);
                        toAppendTo.append(Environment.translate(name, true));
                        pos.setEndIndex(1);
                        return toAppendTo;
                    }

                    @Override
                    public Object parseObject(String source, ParsePosition pos) {
                        return null;
                    }
                });
                return root;
            }

            @Override
            public String getImagePath() {
                return "icons/open.png";
            }

            @Override
            public boolean isDefault() {
                return true;
            }
        });
        return login;
    }

    protected static void generateJarFile(String jarFile) {
        /** ant script to start the hibernatetool 'hbm2java' */
        final String HIBTOOLNAME = "hibtool.xml";
        /** hibernate reverse engeneer configuration */
        final String HIBREVNAME = "hibernate.reveng.xml";
        Properties properties = new Properties();
        properties.setProperty(HIBREVNAME, Environment.getConfigPath() + HIBREVNAME);
//    properties.setProperty("hbm.conf.xml", "hibernate.conf.xml");
        properties.setProperty("server.db-config.file", Persistence.getPath(Persistence.FILE_JDBC_PROP_FILE));
        properties.setProperty("dest.file", jarFile);

        String plugin_dir = Environment.getConfigPath();
        properties.setProperty("plugin.dir", new File(plugin_dir).getAbsolutePath());
        if (plugin_dir.endsWith(".jar/")) {
            properties.setProperty("plugin_isjar", Boolean.toString(true));
        }
        ScriptUtil.antbuild(Environment.getConfigPath() + "hibtool.xml", "create.bean.jar", properties, null);
    }

    protected void reset() {
        navigation = null;
        response = null;
        model = null;
        Environment.reset();
        BeanDefinition.clearCache();
        BeanValue.clearCache();
        Thread.currentThread().setContextClassLoader(appstartClassloader);
        initServices();
        builder = Environment.get(IPageBuilder.class);
        setNavigationModel(createGenericNavigationModel());
    }

//    /**
//     * createTestNavigationModel
//     * 
//     * @return navigation model for testing purpose
//     */
//    static Stack<BeanDefinition<?>> createTestNavigationModel() {
//
//        BeanContainer.initEmtpyServiceActions();
//
//        TypeBean b = new TypeBean();
//        b.setDate(new Date());
//        b.setString("test");
//        b.setBigDecimal(new BigDecimal(10));
//        Bean<TypeBean> bean = new Bean<TypeBean>();
//        final Collection<TypeBean> rootBeanList = new ArrayList(Arrays.asList(b));
//        BeanCollector<Collection<TypeBean>, TypeBean> root = new BeanCollector<Collection<TypeBean>, TypeBean>(TypeBean.class,
//            true,
//            true,
//            false);
//        BeanFinder<TypeBean, Object> beanFinder = new BeanFinder<TypeBean, Object>(TypeBean.class) {
//            @Override
//            public Collection<TypeBean> getData(Object fromFilter, Object toFilter) {
//                return rootBeanList;
//            }
//        };
//        beanFinder.setDetailBean(bean);
//        root.setBeanFinder(beanFinder);
//        final Bean<Object> model = new Bean<Object>();
//        model.addAttribute("Name", "Stefan", RegularExpressionFormat.createAlphaNumRegExp(15, true), null, null);
//        model.addAttribute("Kategorie", 1, null, null, null)
//            .setRange(Arrays.asList(1, 2, 3))
//            .setBasicDef(3, false, null, null, "Kategorie");
//        model.addAction(new CommonAction<Object>("testid", "+xxx", null) {
//            @Override
//            public Object action() throws Exception {
//                String newValue = model.getValue("Name") + "xxx";
//                model.setValue("Name", newValue);
//                return model;
//            }
//        });
////        model.addDefaultSaveAction();
//
//        Bean appStart = new Bean();
//        appStart.setName(START_PAGE);
//
//        Stack<BeanDefinition<?>> navigationModel = new Stack<BeanDefinition<?>>();
//        navigationModel.push(appStart);
//        navigationModel.push(root);
//        navigationModel.push(model);
//        return navigationModel;
//    }
}
