package de.tsl2.nano.h5;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.JSon;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.h5.NanoHTTPD.Method;
import de.tsl2.nano.h5.NanoHTTPD.Response;

public class RestUI {
	public static String BASE_PATH = ENV.get("app.restui.basepath", "/restui");

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
		return ARESTDynamic.canRest(uri) || canRestUI(uri);
	}

	Response serve(NanoH5Session session, String url, String method, Map<String, String> header, Map<String, String> parms, Map<String, String> payload) {
        if (isChangeAction(url) && method.equals("GET")) {
            String name = StringUtil.substring(url, BASE_PATH + "/", "/");
            Map values = Bean.newBean(name).toValueMap(null);
            String html = createInputMask(session, url, name, JSon.toJSon(values));
            return NanoH5.createResponse(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_HTML, html);

            // new HttpClient(url).send("POST", "application/json", JSon.toJSon(values).getBytes());
        } else if (method.equals("POST")) {
            if (is(url, CHANGE_ACTIONS.CREATE)) {
                payload.put("postData", parms.get("payload"));
            } else if (is(url, CHANGE_ACTIONS.CHANGE)) {
                url = parms.get("path");
                method = Method.PUT.name();
            } else if (is(url, CHANGE_ACTIONS.DELETE)) {
                url = parms.get("path");
                method = Method.DELETE.name();
            }
        }
        Response restResponse = new RESTDynamic().serve(restUrl(url), method, header, parms, payload);
        if (!restResponse.getStatus().equals(NanoHTTPD.Response.Status.OK)) {
            return restResponse;
        } else if (url.equals(BASE_PATH)) {
            return createResponse(session, url, restResponse, method);
        } else if (url.endsWith("entities")) {
            return createResponse(session, url, restResponse, method);
        }
        String json = StringUtil.fromInputStream(restResponse.getData());
        String entity = getEntity(url);
        Object instance;
        if (entity == null) {
            instance = json;
        } else if (entity.equals("entities")) {
            instance = json;
        } else if (entity.equals("entitiesjson")) {
            instance = JSon.toList(LinkedHashMap.class, json);
        } else {
            BeanDefinition<?> beanDef = BeanDefinition.getBeanDefinition(entity);
            if (JSon.isJSon(json))
                instance = JSon.toObject(beanDef.getClazz(), json);
            else
                instance = json;
        }

        if (instance instanceof String) {
            instance = HtmlUtil.createNodesFromText((String) instance);
        }
        return createResponse(session, restResponse, entity, instance, method);
    }

    private boolean isChangeAction(String url) {
        CHANGE_ACTIONS[] changeActions = CHANGE_ACTIONS.values();
        for (int i = 0; i < changeActions.length; i++) {
            if (url.endsWith(changeActions[i].value()))
                return true;
        }
        return false;
    }
    private Response createResponse(NanoH5Session session, String url, Response restResponse, String method) {
        String data = StringUtil.fromInputStream(restResponse.getData(), "\n");
        if (!JSon.isJSon(data)) {
            data = enrichtTextToHtml(data);
        }
        String entity = getEntity(url);
        entity = Util.isEmpty(entity) ? BASE_PATH : entity;
        return createResponse(session, restResponse, entity, data, method);
    }

    Response createResponse(NanoH5Session session, Response restResponse, String name, Object instance, String method) {
		de.tsl2.nano.h5.NanoHTTPD.Response.Status status = Response.Status.lookup(restResponse.getStatus().getRequestStatus());
        IPageBuilder<?, String> pageBuilder = ENV.get(IPageBuilder.class); //Bean.getBean(instance).getPresentationHelper();
        String html = pageBuilder.build(session, null, instance, true);
        html = pageBuilder.buildDialog(name, instance);
        return NanoH5.createResponse(status, NanoHTTPD.MIME_HTML, html);
	}

	private String enrichtTextToHtml(String data) {
        StringBuilder enriched = new StringBuilder(data.length() * 2);
        try (Scanner sc = new Scanner(data);) {
            while (sc.hasNextLine()) {
                String l = sc.nextLine();
                if (l.trim().length() == 0)
                    continue;
                else if (l.contains("://")) {
                    String url = StringUtil.extract(l, "\\w{3,5}://[\\w/:\\d?&=]+");
                    l = l.replace(url, createUrl(url, url));
                } else if (BeanDefinition.isDefined(l.trim())) {
                    l = addRESTActions(l);
                } else {
                    l = appendUrl(l, l.contains(BASE_PATH) ? BASE_PATH : l.contains("entitiesjson") ? "entitiesjson" : "entities");
                    l = appendAttributeUrl(l);
                }
                enriched.append("<br/>" + l);
            }
        }
        return HtmlUtil.createMessage(enriched.toString());
    }
    private String addRESTActions(String l) {
        String ll = l.trim();
        return l + " (" 
            + "\t" + createUrl("create", getServicePath(ll + "/create"))
            + "\t" + createUrl("change", getServicePath(ll + "/change"))
            + "\t" + createUrl("delete", getServicePath(ll + "/delete"))
            + ")";
    }
    private String createUrl(String name, String link) {
        return "<a href=\"" + link + "\">" + name + "</a>";
    }
    private String appendAttributeUrl(String l) {
        return l.replaceAll("(\\w+)([.])(\\w+)", "<a href=\"" + getServicePath("") + "$1/$3/*\">$1$2$3</a>");
    }
    private String appendUrl(String l, String name) {
        return l.replace(name, createUrl(name, getServicePath(name)));
    }
    private String getServicePath(String name) {
        return ENV.get("service.url", "") + BASE_PATH + "/" + name;
    }
    private String createInputMask(NanoH5Session session, String uri, String name, String json) {
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
            b.getAttribute(label).getPresentation().setType(IPresentable.TYPE_INPUT_MULTILINE);
        } else {
            label = "path";
            value = getServicePath(name) + "/{select-attribute-name}/{select-attribute-value}/{attribute-name-of-new-value}/{new-value}";
        }
        b.addAttribute(label, value, null,  label, null);
        return (String) ((IPageBuilder)b.getPresentationHelper()).build(session, b, null, true);
    }
    private boolean is(String uri, CHANGE_ACTIONS action) {
        return uri.endsWith(action.value());
    }
    private String restUrl(String url) {
        return url.replace(BASE_PATH, ARESTDynamic.BASE_PATH);
    }

    private String getEntity(String url) {
        return StringUtil.substring(url, BASE_PATH + "/", "/", true, true);
    }
}