package de.tsl2.nano.h5;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.Main;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.exception.ExceptionHandler;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.h5.navigation.EntityBrowser;
import de.tsl2.nano.persistence.DatabaseTool;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.serviceaccess.Authorization;

/**
 * ONLY TO BE EXTENDED BY JUNIT TESTS! USES JUNIT+HTMLUNIT
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public abstract class NanoH5Unit implements ENVTestPreparation {

    protected static final int DEFAULT_H2_PORT = 9092;
    protected static final int DEFAULT_HSQLDB_PORT = 9003;
	protected static final String BTN_LOGIN_OK = PersistenceUI.ACTION_LOGIN_OK;
    protected static final String BTN_RESET = ".reset";
    protected static final String BTN_DELETE = ".delete";
    protected static final String BTN_CANCEL = IAction.CANCELED;
    protected static final String BTN_NEW = ".new";
    protected static final String BTN_EXPORT = ".export";
    protected static final String BTN_PRINT = ".print";
    protected static final String BTN_BACK = ".back";
    protected static final String BTN_FORWARD = ".forward";
    protected static final String BTN_SEARCH = ".search";
    protected static final String BTN_SHUTDOWN = ".shutdown";
    protected static final String BTN_OPEN = ".open";
    protected static final String BTN_ADMINISTRATION = ".administration";
    protected static final String BTN_SELECTALL = ".selectall";
    protected static final String BEANCOLLECTORLIST = (BeanCollector.class.getSimpleName()
            + Messages.getString("tsl2nano.list")).toLowerCase();

    protected boolean nanoAlreadyRunning;
    protected int port = -1;

    protected String getServiceURL(boolean nextFreePort) {
        return "http://localhost:" + (port == -1 && nextFreePort ? port = NetUtil.getNextFreePort(Main.DEFAULT_PORT) : port == -1 ? port = Main.DEFAULT_PORT : port);
    }

    protected int dbPort() {
    	return DEFAULT_HSQLDB_PORT;
    }
    
    public void setUp() {
    	setUpUnit("h5");
    }
    
    public void setUpUnit(String moduleShort) {
//    	tearDownAfter(250000);
        System.setProperty("tsl2nano.offline", "true");
        System.setProperty(ENV.KEY_TESTMODE, "true");
        System.setProperty("app.stop.allow.system.exit", "false");
        nanoAlreadyRunning = Boolean.getBoolean("app.server.running");
        NanoH5UnitPlugin.setEnabled(!nanoAlreadyRunning);
        ENVTestPreparation.super.setUp(moduleShort);
        if (!nanoAlreadyRunning) {
//    		GenericLocalBeanContainer.initLocalContainer();
        	setPersistenceConnectionPort();
        	DatabaseTool.runDBServerDefault(); //in the test it seems not enough (forks?) to let nanoh5 start h2 internally....
//        	ENV.setProperty("app.database.internal.run", true);
        	ENV.setProperty("service.url", getServiceURL(!nanoAlreadyRunning));
            startApplication();
            ConcurrentUtil.waitFor(()->NetUtil.isOpen(port));
        } else {
        	setPersistenceConnectionPort();
//        	DatabaseTool.runDBServerDefault(); //in the test it seems not enough (forks?) to let nanoh5 start h2 internally....
            System.out.println("NanoH5TestBase: nanoAlreadyRunning=true ==> trying to connect to external NanoH5");
        }
		// ConcurrentUtil.sleep(20000); //otherwise the nano-server is not started completely on parallel testing
    }

	protected void setPersistenceConnectionPort() {
		try {
			Persistence persistence = Persistence.current();
			String url = persistence.getConnectionUrl().replace(String.valueOf(DEFAULT_H2_PORT), String.valueOf(dbPort()));
			persistence.setConnectionUrl(url);
			persistence.setDatabase(Persistence.DEFAULT_DATABASE);
			persistence.save();
		} catch (IOException e) {
			ManagedException.forward(e);
		}
	}
    
	/** WORKAROUND FOR SUREFIRE/FAILSAFE failing on not stopping fork */
	private void tearDownAfter(long millis) {
		ConcurrentUtil.doAfterWait(millis, "tearDown after " + millis + " ms", () -> {this.tearDown(); return null;});
	}

    public void tearDown() {
    	if (webClient != null)
    		webClient.close();
    	if (NetUtil.isOpen(port))
    		shutdownNanoHttpServer();
    	if (NetUtil.isOpen(dbPort()))
    		DatabaseTool.shutdownDBServerDefault();
//    	ENVTestPreparation.removeCaches();
    	Bean.clearCache();
        BeanContainer.reset();
    }
    
    protected void startApplication() {
		startApplication(getTestEnv(), String.valueOf(port));
    }
    protected static void startApplication(String...args) {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.jnu.encoding", "UTF-8");
        System.setProperty("JAVA_OPTS", "-Xmx512m -Djava.awt.headless -agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n");
        System.setProperty("tsl2nano.offline", "true");
        System.setProperty("websocket.use", "false");
        System.setProperty("app.show.startpage", "false");
        ConcurrentUtil.startDaemon(new Runnable() {
            @Override
            public void run() {
                new Loader().start(NanoH5.class.getName(), args);
//                Main.startApplication(NanoH5.class, null, args);
            }
        });
    }
	private WebClient webClient;

    protected HtmlPage runWebClient() {
        return runWebClient(getServiceURL(!nanoAlreadyRunning));
    }
    
    protected HtmlPage runWebClient(String serviceURL) {
        HtmlPage page = null;
        webClient = new WebClient();
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setTimeout(1200000); //20min
        webClient.getOptions().setPrintContentOnFailingStatusCode(true);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
//        webClient.getOptions().setRedirectEnabled(true);
        try {
            page = webClient.getPage(serviceURL);
        } catch (FailingHttpStatusCodeException | IOException e) {
            ManagedException.forward(e);
        }
        if (page == null)
            throw new AssertionError("web client can't get first page on " + serviceURL
                + "Please stop any running NanoH5 application on this port!");
        return page;
    }

    protected HtmlPage submit(HtmlPage page, String buttonName) throws Exception {
        System.out.println("htmlUnit testing button: " + buttonName);
        try {
            HtmlButton htmlButton = (HtmlButton) page.getElementById(buttonName);
            if (htmlButton == null) {
                throw new IllegalArgumentException("button " + buttonName + " not found! page:\n\t" + page.asXml());
            }
            page = htmlButton.click();
            page.getWebClient().waitForBackgroundJavaScript(1000);
            return page;
        } catch (Exception e) {
            List<Throwable> exceptions = ((ExceptionHandler)ENV.get(UncaughtExceptionHandler.class)).getExceptions();
            String pageName = page.getBody().getId();
            String buttonHandleError = "error on clicking button: '" + buttonName + "' on page '" + pageName + "'\n\n"
                    + StringUtil.toFormattedString(exceptions, -1);
            String asXml = "<!--\n" + buttonHandleError +
                ManagedException.toString(e) + "\n-->\n" + page.asXml();
            FileUtil.writeBytes(asXml.getBytes(), ENV.getTempPath() + "page-failed.html", false);
            System.out.println(buttonHandleError);
            ManagedException.forward(e);
            return page;
        }
        //        return form.getInputByName(buttonName).click();
    }

    protected HtmlPage back(HtmlPage page) throws Exception {
        page.getWebClient().getWebWindows().get(0).getHistory().back();
        return (HtmlPage) page;//.refresh();
    }

    protected HtmlPage crudBean(HtmlPage page) throws Exception {
        String pageId = page.getBody().getId(); //thats not the best solution - perhaps a bean has a translated name
        if (Util.isEmpty(pageId))
            throw new IllegalStateException("pageId is empty!");
        String beanName = StringUtil.toFirstLower(pageId);
        if (BeanDefinition.getBeanDefinition(beanName).isVirtual()) {
        	System.out.println("beanname was translated....ignoring");
        	return submit(page, BTN_CANCEL);
        }
        String beanList = beanName + Messages.getString("tsl2nano.list").toLowerCase();

        //TODO: check pages with with saved last current state
        page = submit(page, beanList + BTN_SEARCH);
        page = submit(page, beanList + BTN_FORWARD);
        page = submit(page, beanList + BTN_BACK);
                submit(page, beanList + BTN_PRINT);
                page = back(page);
                submit(page, beanList + BTN_EXPORT);
                page = back(page);

        page = submit(page, beanList + BTN_NEW);
        if (page.getElementById(BTN_CANCEL) == null)
        	return back(page); //workaround
        page = submit(page, BTN_CANCEL);
        //        page = submit(page, beanName + ".save");
//                page = submit(page, beanList + BTN_DELETE);
//                page = submit(page, beanName + BTN_RESET);
        return page;
    }

    /*
     * conveniences to create a testable nanoh5 application + session
     */
    public static NanoH5Session createApplicationAndSession(String name, Serializable... instances) throws IOException, UnknownHostException {
		return createApplicationAndSession(name, Arrays.stream(instances).map(i -> Bean.getBean(i)).toArray(Bean[]::new));
	}
	// public static NanoH5Session createApplicationAndSession(String name, Class...classes) throws IOException, UnknownHostException {
	// 	return createApplicationAndSession(name, Arrays.stream(classes).map(c -> BeanDefinition.getBeanDefinition(c)).toArray(BeanDefinition[]::new));
	// }
	public static NanoH5Session createApplicationAndSession(String name, BeanDefinition...beandefs) throws IOException, UnknownHostException {
		NanoH5 server = new NanoH5();
//		NanoH5Session session = (NanoH5Session) new PrivateAccessor<NanoH5>(server).call("createSession", NanoH5Session.class
//				, new Class[] {InetAddress.class}, InetAddress.getLocalHost());
        Stack<BeanDefinition<?>> nav = new Stack<BeanDefinition<?>>();
        nav.addAll((List) Arrays.asList(beandefs));
		EntityBrowser entities = new EntityBrowser("test", nav);
		entities.next(null);
		NanoH5Session session = NanoH5Session.createSession(server, InetAddress.getLocalHost()
				, entities, Thread.currentThread().getContextClassLoader()
				, Authorization.create(name, false), new HashMap());
		return session;
	}
	
	protected void shutdownNanoHttpServer() {
		shutdownNanoHttpServer(FileUtil.userDirFile(ENV.getTempPath() + "instance-id.txt"));
	}
	protected void shutdownNanoHttpServer(File idFile) {
		if (!idFile.exists())
			return;
		String id = FileUtil.getFileString(idFile.getAbsolutePath());
		String cmdShutdown = "/" + id + "-shutdown";
		String url = getServiceURL(false) + cmdShutdown;
		try {
			NetUtil.browse(url, System.out);
		} catch (Exception e) {
			System.out.println("couldn't shutdown nanohttp server on: " + url + " exception: " + e.toString());
		}
	}
}
