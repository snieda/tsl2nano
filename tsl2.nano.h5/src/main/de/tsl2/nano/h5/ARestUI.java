package de.tsl2.nano.h5;

import static de.tsl2.nano.h5.ARESTDynamic.Methods.DELETE;
import static de.tsl2.nano.h5.ARESTDynamic.Methods.GET;
import static de.tsl2.nano.h5.ARESTDynamic.Methods.POST;
import static de.tsl2.nano.h5.ARESTDynamic.Methods.PUT;
import static de.tsl2.nano.h5.NanoH5Util.LOG;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Supplier;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ISession;
import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.core.util.parser.JSon;
import de.tsl2.nano.h5.ARESTDynamic.Status;

/**
 * Provides a ui service to interact with a static ARESTDynamic restful service.
 * enriches responses of that restful service and creates additional dialogs to
 * call the restful serivce with methods like PUT, POST and DELETE. Tries to 
 * implement something like HATEOAS, providing links the possible actions in the
 * current context.
 * Usage: see initial help text of REST service: '/rest' or go directly to 
 * the RestUI page: '/restui'. This will you guide further with possible links
 * to current context.
 * 
 * Example: http://localhost/8067/restui.
 * 
 * @TYPE RESPONSE response implementation of an HttpServer (e.g.: NanoHTTPD.Response)
 */

public abstract class ARestUI<RESPONSE> {
    private static final String MIME_HTML = "text/html";
    public static String BASE_PATH = ENV.get("app.restui.basepath", "/restui");
    private static final String URLKEY_ENTITIES = "entities";
    private static final String URLKEY_ENTITIESJSON = "entitiesjson";
	private static final String FIELD_PATH = "path";

    enum CHANGE_ACTIONS {
        CREATE, CHANGE, DELETE;

        public String value() {
            return this.toString().toLowerCase();
        }
    }
	public static boolean canRestUI(String uri) {
        return (uri.endsWith(BASE_PATH) || uri.contains(BASE_PATH + "/")) && ENV.get("app.restui.active", true);
    }
    public static boolean canRest(String uri) {
		return ARESTDynamic.canRest(uri) || canRestUI(uri) && !isFileRequest(uri);
	}

	private static boolean isFileRequest(String uri) {
        return uri.matches(".*/icons/.*[.](jpg|png|gif|ico)");
    }
    RESPONSE serve(ISession session, String url, String method, Map<String, String> header, Map<String, String> parms, Map<String, String> payload) {
        if (isChangeAction(url) && method.equals(GET.name()) && !isCanceled(parms)) {
            return provideInputDialog(session, url, null);
        } else if (method.equals(POST.name())) {
            if (isCanceled(parms)) {
                url = BASE_PATH + "/" + URLKEY_ENTITIES;
                method = GET.name();
            //prepare REST request from InputDialog
            } else if (is(url, CHANGE_ACTIONS.CREATE)) {
                payload.put(ARESTDynamic.BODY, parms.get("payload"));
            } else if (is(url, CHANGE_ACTIONS.CHANGE)) {
                url = parms.get(FIELD_PATH);
                method = PUT.name();
            } else if (is(url, CHANGE_ACTIONS.DELETE)) {
                url = parms.get(FIELD_PATH);
                method = DELETE.name();
            }
        }
        RESPONSE restResponse = callRestService(url, method, header, parms, payload);

        if (method.equals(POST.name())) {
            return provideInputDialog(session, url, getData(restResponse));
        } else if (!getStatus(restResponse).equals(Status.OK)
            || url.equals(BASE_PATH)
            || url.endsWith(URLKEY_ENTITIES)) {
            return createResponse(session, url, restResponse, method);
        }
        return presentDesiredObject(session, url, method, restResponse);
    }
    private boolean isCanceled(Map<String, String> parms) {
        return parms.containsKey(IAction.CANCELED);
    }
    private RESPONSE presentDesiredObject(ISession session, String url, String method, RESPONSE restResponse) {
        String json = getData(restResponse);
        String entity = getEntity(url);
        Object instance;
        if (entity == null) {
            instance = json;
        } else if (entity.equals(URLKEY_ENTITIES)) {
            instance = json;
        } else if (entity.equals(URLKEY_ENTITIESJSON)) {
            instance = new JSon().toList(LinkedHashMap.class, json);
        } else {
            if (JSon.isJSon(json)) {
                BeanDefinition<?> beanDef = BeanDefinition.getBeanDefinition(entity);
                instance = new JSon().toObject(beanDef.getClazz(), json);
            } else
                instance = json;
        }

        if (instance instanceof String) {
            instance = HtmlUtil.createHtmlFromText((String) instance);
        }
        return createResponse(session, restResponse, entity, instance, method);
    }
    private RESPONSE provideInputDialog(ISession session, String url, String message) {
        String name = StringUtil.substring(url, BASE_PATH + "/", "/");
        Map values = Bean.newBean(name).toValueMap(null);
        String html = createInputMask(session, url, name, new JSon().serialize(values), message);
        return createResponse(Status.OK, MIME_HTML, html);
    }
    private boolean isChangeAction(String url) {
        CHANGE_ACTIONS[] changeActions = CHANGE_ACTIONS.values();
        for (int i = 0; i < changeActions.length; i++) {
            if (url.endsWith(changeActions[i].value()))
                return true;
        }
        return false;
    }
    private RESPONSE createResponse(ISession session, String url, RESPONSE restResponse, String method) {
        String data = getData(restResponse);
        if (!JSon.isJSon(data)) {
            data = enrichtTextToHtml(url, data);
        }
        String entity = getEntity(url);
        entity = Util.isEmpty(entity) ? BASE_PATH : entity;
        return createResponse(session, restResponse, entity, data, method);
    }

    RESPONSE createResponse(ISession session, RESPONSE restResponse, String name, Object instance, String method) {
        Bean model = instance instanceof String ? null : Bean.getBean(instance);
        String msg = instance instanceof String ? instance.toString() : null;
        IPageBuilder<?, String> pageBuilder = ENV.get(IPageBuilder.class); //Bean.getBean(instance).getPresentationHelper();
        String html = model == null && msg.contains("</html>") ? msg : pageBuilder.build(session, model, msg, true);
        // html = pageBuilder.buildDialog(name, instance);
        return createResponse(getStatus(restResponse), MIME_HTML, html);
	}
    /** override and implement the real response of your selected HttpServer */
    abstract protected RESPONSE createResponse(Status status, String mimeType, String html);

    /** override and implement the real response status of your selected HttpServer */
    abstract protected Status getStatus(RESPONSE restResponse);
    /** override and implement the real response data of your selected HttpServer */
    abstract protected String getData(RESPONSE restResponse);
    /** override and implement the real call to the desired RESTful service */
    abstract protected RESPONSE callRestService(String url, String method, Map<String, String> header, Map<String, String> parms,
            Map<String, String> payload);

	private String enrichtTextToHtml(String pageUrl, String data) {
        Page page = createPage(data);
        try (Scanner sc = new Scanner(data);) {
            while (sc.hasNextLine()) {
                String l = sc.nextLine();
                if (l.trim().length() == 0)
                    continue;
                if (l.contains("://")) {
                    String url = StringUtil.extract(l, "\\w{3,5}://[\\w/:\\d?&=]+");
                    String urlui = url.replace(ARESTDynamic.BASE_PATH, BASE_PATH);
                    l = l.replace(url, createUrl(url, urlui));
                } else if (BeanDefinition.isDefined(l.trim())) {
                    page.up( () -> page.isCurrent("tr"));
                    page.tag("tr");
                    l = addRESTActions(l);
                } else {
                    page.tag("tr", () -> !page.isCurrent("tr"));
                    l = appendUrl(l, l.contains(BASE_PATH) ? BASE_PATH : l.contains(URLKEY_ENTITIESJSON) ? URLKEY_ENTITIESJSON : URLKEY_ENTITIES);
                    l = appendAttributeUrl(l);
                    // page.up();
                }
                page.tag("tr", () -> !page.isCurrent("tr"));
                if (pageUrl.endsWith("restui")) {
                    page.add(l).add("</br>");
                } else {
                    page.tag("td").add(l).up();
                }
            }
        }
        return HtmlUtil.createMessage(page.toString());
    }
    private Page createPage(String data) {
        Page page = new Page("restui", data.length() * 2);
        page.tag("html", Page.attr("manifest", ENV.get("html5.manifest.file", "")))
            .tag("head")
            .tag("style").add(HtmlUtil.templateStyles())
            .up().up()
            .tag("body", Page.attr("id", "page"))
            .tag("table");
        return page;
    }
    private String addRESTActions(String l) {
        String ll = l.trim();
        return l + " (" 
            + "\t" + createUrl(CHANGE_ACTIONS.CREATE, getServicePath(ll + "/" + CHANGE_ACTIONS.CREATE.value()))
            + "\t" + createUrl(CHANGE_ACTIONS.CHANGE, getServicePath(ll + "/" + CHANGE_ACTIONS.CHANGE.value()))
            + "\t" + createUrl(CHANGE_ACTIONS.DELETE, getServicePath(ll + "/" + CHANGE_ACTIONS.DELETE.value()))
            + ")";
    }
    private String createUrl(Object name, String link) {
        return "<a id=\"" + name + "\" class=\"restui\" href=\"" + link + "\">" + name + "</a>";
    }
    private String appendAttributeUrl(String l) {
        return l.replaceAll("(\\w+)([.])(\\w+)", "<a id=\"attribute\" class=\"restui\" href=\"" + getServicePath("") + "$1/$3/*\">$1$2$3</a>");
    }
    private String appendUrl(String l, String name) {
        return l.replace(name, createUrl(name, getServicePath(name)));
    }
    private String getServicePath(String name) {
        return ENV.get("service.url", "") + BASE_PATH + "/" + name;
    }
    private String createInputMask(ISession session, String uri, String name, String json, String message) {
        // The solution with Html Dialog cannot submit the result!
        // WSDialog wsDlg = new WSDialog(name, "", WSButton.CLOSE, WSButton.SUBMIT);
        // wsDlg.addFields(new WSField(name, json, true));
        // return wsDlg.toHtmlDialog();
        Bean<?> b = Bean.newBean("restui");
        b.setName(name);
        String label, value;
        if (is(uri, CHANGE_ACTIONS.CREATE)) {
            label = "payload";
            value = json;
        } else {
            label = FIELD_PATH;
            value = getServicePath(name) + "/{select-attribute-name}/{select-attribute-value}/{attribute-name-of-new-value}/{new-value}";
        }

        b.addAttribute(label, value, null,  label, null);

        if (is(uri, CHANGE_ACTIONS.CREATE)) {
            b.getAttribute(label).getPresentation().setType(IPresentable.TYPE_INPUT_MULTILINE);
        }
        return (String) ((IPageBuilder)b.getPresentationHelper()).build(session, b, message, true);
    }
    private boolean is(String uri, CHANGE_ACTIONS action) {
        return uri.endsWith(action.value());
    }
    protected String restUrl(String url) {
        return url.replace(BASE_PATH, ARESTDynamic.BASE_PATH);
    }

    private String getEntity(String url) {
        return StringUtil.substring(url, BASE_PATH + "/", "/", true, true);
    }
}
/** 
 * simple convenience to create structured content like xml/html without having to think about closing tags and positioning
 * constraint: provides only straight forward positioning 
 */
class Page {
    String name;
    StringBuilder content;
    int cur;
    String tagOpen, tagClose, tagEnd;

    LinkedList<Tag> stack = new LinkedList<>();

    public Page(String name, int length) {
        this(name, "<", ">", "</", length);
    }

    public Page(String name, String tagOpen, String tagClose, String tagEnd, int length) {
        this.name = name;
        content = new StringBuilder(length * 2);
        this.tagOpen = tagOpen;
        this.tagClose = tagClose;
        this.tagEnd = tagEnd;
    }
    Page add(String part) {
        return add(cur, part);
    }
    Page add(int pos, String part) {
        cur = pos == -1 ? content.length() : pos;
        content.insert(cur, part);
        cur += part.length();
        return this;
    }

    Page tag(String tag, Supplier<Boolean> if_, String...attributes) {
        return if_.get() ? tag(tag, attributes) : this;
    }

    Page tag(String tag, String...attributes) {
        return tag(cur, tag, attributes);
    }

    Page tag(int pos, String tag, String...attributes) {
        StringBuilder attrs = new StringBuilder(pageAttributes());
        Arrays.stream(attributes).forEach(a -> attrs.append(a));
        add(pos, tagOpen + tag + attrs + tagClose);
        int inside = cur;
        add(tagEnd + tag + tagClose);
        cur = inside;
        stack.add(new Tag(tag, inside));
        return this;
    }

    String pageAttributes() {
        return attr("class", name);
    }

    static String attr(String name, String value) {
        return " " + name + "=\"" + value + "\"";
    }

    Page up(String to) {
        // did not find any performant solution on streams
        int indexOfToTag = stack.lastIndexOf(new Tag(to, -1));
        if (indexOfToTag < 0)
            throw new IllegalArgumentException("cannot go up in path until: '" + to + "' current path is:" + stack);
        stack = (LinkedList)stack.subList(0, indexOfToTag);
        LOG.debug("stack changed to sublist: " + stack);
        return this;
    }
    Page up(Supplier<Boolean> if_) {
        return if_.get() ? up(): this;
    }

    Page up() {
        Tag tag = stack.removeLast();
        String strTag = tagEnd + tag.name + tagClose;
        int i = content.indexOf(strTag, cur);
        if (i < 0)
            throw new IllegalStateException("cannot find close tag for: " + tag.name);
        cur = i + strTag.length();
        return this;
    }

    public List<Tag> path() {
        return Collections.unmodifiableList(stack);
    }

    boolean isCurrent(String tagName) {
        return currentTag().name.equals(tagName);
    }

    public Tag currentTag() {
        return stack.getLast();
    }

    public String info() {
        return ObjectUtil.toString(this, "name", "cur", "stack", "content");
    }

    @Override
    public String toString() {
        return content.toString();
    }

    class Tag {
        String name;
        int pos;
        public Tag(String name, int pos) {
            this.name = name;
            this.pos = pos;
        }
        @Override
        public boolean equals(Object obj) {
            return obj != null && ((Tag)obj).name.equals(name);
        }
        @Override
        public String toString() {
            return ObjectUtil.toString(this, "name", "pos");
        }
    }
}