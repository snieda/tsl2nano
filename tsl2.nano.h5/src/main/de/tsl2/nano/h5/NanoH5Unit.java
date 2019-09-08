package de.tsl2.nano.h5;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.List;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.exception.ExceptionHandler;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * ONLY TO BE EXTENDED BY JUNIT TESTS! USES JUNIT+HTMLUNIT
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public abstract class NanoH5Unit implements ENVTestPreparation {

    protected static final String BTN_LOGIN_OK = "tsl2nano.login.ok";
    protected static final String BTN_RESET = ".reset";
    protected static final String BTN_DELETE = ".delete";
    protected static final String BTN_CANCEL = "de.tsl2.nano.action.action_cancelled";
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

    protected static String TEST_DIR;
    protected static boolean nanoAlreadyRunning;
    protected static int port = -1;
    protected static final String BEANCOLLECTORLIST = (BeanCollector.class.getSimpleName()
        + Messages.getString("tsl2nano.list")).toLowerCase();

    protected static String getServiceURL(boolean nextFreePort) {
        return "http://localhost:" + (port == -1 && nextFreePort ? port = NetUtil.getNextFreePort(8067) : port == -1 ? port = 8067 : port);
    }

    public static void setUp() {
        nanoAlreadyRunning = Boolean.getBoolean("app.server.running");
        NanoH5UnitPlugin.setEnabled(!nanoAlreadyRunning);
        TEST_DIR = ENVTestPreparation.setUp(false) + TARGET_TEST;
        if (!nanoAlreadyRunning) {
            ENV.setProperty("service.url", getServiceURL(!nanoAlreadyRunning));
            startApplication();
            ConcurrentUtil.waitFor(()->NetUtil.isOpen(port));
        } else {
            System.out.println("NanoH5TestBase: nanoAlreadyRunning=true ==> trying to connect to external NanoH5");
        }
    }
    
    public static void tearDown() {
        // String target = StringUtil.subEnclosing(new File(TEST_DIR).getAbsolutePath(), null, "target", false);
        // String instanceId = FileUtil.getFileString(target + "/pre-integration-test/.nanoh5.environment/temp/instance-id.txt");
        // NetUtil.get(getServiceURL(false) + "/" + instanceId);
        if (BeanContainer.isInitialized())
            BeanContainer.instance().executeStmt(ENV.get("app.shutdown.statement", "SHUTDOWN"), true, null);
    }
    
    protected static void startApplication() {
        startApplication(new String[0]);
    }

    protected static void startApplication(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.jnu.encoding", "UTF-8");
        System.setProperty("JAVA_OPTS", "-Xmx512m -Djava.awt.headless -agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n");
        System.setProperty("tsl2nano.offline", "true");
        System.setProperty("websocket.use", "false");
        System.setProperty("app.show.startpage", "false");
        ConcurrentUtil.startDaemon(new Runnable() {
            @Override
            public void run() {
                new Loader().start("de.tsl2.nano.h5.NanoH5", args);
//                Main.startApplication(NanoH5.class, null, args);
            }
        });
    }

    protected HtmlPage runWebClient() {
        return runWebClient(getServiceURL(!nanoAlreadyRunning));
    }
    
    protected HtmlPage runWebClient(String serviceURL) {
        HtmlPage page = null;
        WebClient webClient = new WebClient();
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setTimeout(1200000); //20min
        webClient.getOptions().setPrintContentOnFailingStatusCode(true);
        webClient.getOptions().setCssEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
//        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
//        webClient.getOptions().setRedirectEnabled(true);
        try {
            page = webClient.getPage(serviceURL);
        } catch (FailingHttpStatusCodeException | IOException e) {
            ManagedException.forward(e);
        }
        if (page == null)
            fail("web client can't get first page on " + serviceURL
                + "Please stop any running NanoH5 application on this port!");
        return page;
    }

    protected HtmlPage submit(HtmlPage page, String buttonName) throws Exception {
        System.out.println("htmlUnit testing button: " + buttonName);
        try {
            HtmlButton htmlButton = (HtmlButton) page.getElementById(buttonName);
            if (htmlButton == null) {
                throw new IllegalArgumentException("button " + buttonName + " not found!");
            }
            page = htmlButton.click();
            page.getWebClient().waitForBackgroundJavaScript(500);
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
        String pageId = page.getBody().getId();
        if (Util.isEmpty(pageId))
            throw new IllegalStateException("pageId is empty!");
        String beanName = StringUtil.toFirstLower(pageId);
        String beanList = beanName + Messages.getString("tsl2nano.list").toLowerCase();

        //TODO: check pages with with saved last current state
        page = submit(page, beanList + BTN_SEARCH);
        page = submit(page, beanList + BTN_FORWARD);
        page = submit(page, beanList + BTN_BACK);
//                submit(page, beanList + BTN_PRINT);
//                page = back(page);
//                submit(page, beanList + BTN_EXPORT);
//                page = back(page);

        page = submit(page, beanList + BTN_NEW);
        page = submit(page, BTN_CANCEL);
        //        page = submit(page, beanName + ".save");
//                page = submit(page, beanList + BTN_DELETE);
//                page = submit(page, beanName + BTN_RESET);
        return page;
    }

    protected static void shutdown() {
        System.exit(0);
    }
}
