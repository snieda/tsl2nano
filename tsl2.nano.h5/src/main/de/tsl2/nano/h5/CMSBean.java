package de.tsl2.nano.h5;

import static de.tsl2.nano.h5.CMSBean.DefaultAttributes.DESCRIPTION;
import static de.tsl2.nano.h5.CMSBean.DefaultAttributes.IMAGE;
import static de.tsl2.nano.h5.CMSBean.DefaultAttributes.NAME;
import static de.tsl2.nano.h5.CMSBean.DefaultAttributes.VALUE;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.logging.Log;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.action.IActivable;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IIPresentable;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.bean.def.Value;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.execution.SystemUtil;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FilePath;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.core.util.parser.JSon;
import de.tsl2.nano.execution.IPRunnable;
import de.tsl2.nano.h5.expression.WebClient;
import de.tsl2.nano.specification.Pool;
import de.tsl2.nano.specification.actions.Action;

/**
 * Creates Beans from a given content url. 
 * This url must point to a README.MD.
 * The README.MD must contain a tag "{{NAMES}}" followed by a comma separated list of bean names.
 * This bean names have to be found as child folders of the given baseurl (where the README.MD was found)
 * So each bean has to have its own folder. If the name (=folder) starts with "-" it is marked as beeing a collection of sub beans.
 * Each bean (=folder) has to have its own README.MD containing a tag "{{NAMES}}", specifying the attribute names.
 * If a bean has actions, the names of these actions have to be specified after the tag {{ACTIONS}}.
 * 
 * Each attribute is defined by its 
 *  - name (read from README.MD -> {{NAMES}})
 *  - description (optional) read from {NAME}-description.txt
 *  - presentable (optional) read from {NAME}-presentable.json
 *  - image (optional)       read from {NAME}-image.jpg
 *  - value                  read from {NAME}-value.obj
 *
 * The value will be interpreted by the content:
 *  - url : the url will be called
 *  - json: the json content will be wrapped into an object given by {NAME}-valuetype.txt
 *  - endswith a standard extension like zip/jpg/tar: it is a download link
 *  - otherwise: it is a simple string
 * 
 * All Actions (names are defined in README.MD through {{ACTION}} list) have to have the extension ".action"
 * 
 * Each Action will be interpreted by the content:
 *  - class+method name (parameter: de.tsl2.nano.bean.def.Bean.class (to get all informations through current selected bean/attributes))
 *  - url (starting with uri scheme expression (see https://en.wikipedia.org/wiki/List_of_URI_schemes))
 *  - otherwise: shell action
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class CMSBean {
    private static final Log LOG = LogFactory.getLog(CMSBean.class);

    private static final String OPEN_TAG = "\\{\\{";
    private static final String CLOSE_TAG = "\\}\\}\\: ";

    private static final String TAG_NAMES = "NAMES";
    private static final String TAG_ACTIONS = "ACTIONS";

    private static final String INFO = "/README.MD";
    protected static final String PREFIX_COLLECTOR = "-";

    enum DefaultAttributes {NAME, DESCRIPTION, IMAGE, VALUE; public String toString() {return name().toLowerCase();} };

    public static BeanCollector<List<BeanDefinition>, BeanDefinition> provideCMSBeans(String definitionUrl) {
        return provideCMSBeans(definitionUrl, false);
    }
    /** if strict=true, all attribute files must be present */
    public static BeanCollector<List<BeanDefinition>, BeanDefinition> provideCMSBeans(String definitionUrl, boolean strict) {
        ENV.assignClassloaderToCurrentThread();
        definitionUrl = definitionUrl.endsWith(INFO) ? definitionUrl : definitionUrl + (definitionUrl.endsWith("/") ? INFO.substring(1) : INFO);
        logFrame("starting from " + definitionUrl + "\n\tall content will be downloaded to: " + getDefinitionDirectory());

        List<String> names = readNamesFromUrl(definitionUrl);
        String baseUrl = baseUrl(definitionUrl);
        List<BeanDefinition> beans = new ArrayList<>(names.size());
        
        names.parallelStream().forEach(n -> beans.add(n.startsWith(PREFIX_COLLECTOR) ? provideCMSBeans(baseUrl + n + INFO, strict) : createBean(baseUrl, n, strict)));
        
        logFrame(beans.size() + " beans created:\n" + beans.stream().map(b -> ((BeanDefinition)b).toValueMap(null).toString() + "\n").toList());
        return new BeanCollector<List<BeanDefinition>,BeanDefinition>(beans, 1);
    }

    static void logMsg(String txt) {
        Message.send(txt);
    }

    static void logFrame(String txt) {
        Message.send(txt);
        String frame = "\n" + StringUtil.fixString(80, '=') + "\n";
        LOG.info(frame + txt + frame);
    }

    private static String baseUrl(String definitionUrl) {
        return StringUtil.substring(definitionUrl, null, "/", true) + "/";
    }


    private static Bean<?> createBean(String baseUrl, String name, boolean strict) {
        ENV.assignClassloaderToCurrentThread();
        Bean<Object> bean = new Bean<>();
        bean.setName(name);
        // bean.setAttributeFilter("name");
        ((IIPresentable)bean.getPresentable()).setDescription(readFromDownload(baseUrl + name + INFO, false));
        
        List<String> names = readNamesFromUrl(baseUrl + name + INFO, false);
        String baseUrlName = baseUrl + name + "/";
        if (names == null) {
            // names = Arrays.stream(DefaultAttributes.values()).map(a -> a.toString().toLowerCase()).toList();
            addDefaultAttributes(bean, baseUrlName, name, strict);
        } else {
            logMsg("creating bean '" + name + "' with attributes: " + names);
            names.forEach(n -> addAttribute(bean, baseUrlName,  n, strict));
        }
        List<String> actions = readNamesFromUrl(baseUrl + name + INFO, TAG_ACTIONS, false);
        if (actions == null) {
            actions = Arrays.asList(name);
        }
        logMsg("adding actions to  bean '" + name + "': " + actions);
        Map<String, Object> props = bean.toValueMap(null);
        props.put("bean", bean.getName());
        actions.forEach(n -> addAction(bean, baseUrlName, n, strict, props));
        return bean;
    }

    private static void addDefaultAttributes(Bean<Object> bean, String baseUrlName, String name, boolean strict) {
        logMsg("adding default attributes on " + name);
        Map<String, Object> props = bean.toValueMap(null);
        props.put("bean", bean.getName());
        AttributeDefinition<String> attr = bean.addAttribute(NAME.toString(), new Value<String>(NAME.toString(), name), null, null);
        extendPresentable(attr.getPresentation());
        
        attr = bean.addAttribute(DESCRIPTION.toString(),
            new Value<String>(DESCRIPTION.toString(), 
                    readFromDownload(attrFile(baseUrlName, name, DESCRIPTION.toString(), ".txt"), strict, props)), 
                null, 
                null);
        extendPresentable(attr.getPresentation(), IPresentable.TYPE_INPUT_MULTILINE);
        
        String imageUrl = getImageUrl(baseUrlName, name, strict, props);
        attr = bean.addAttribute(IMAGE.toString(),
            new Value<String>(IMAGE.toString(), imageUrl), 
                null, 
                null);
        extendPresentable(attr.getPresentation(), IPresentable.TYPE_ATTACHMENT);

        attr = bean.addAttribute(VALUE.toString(),
            new Value(VALUE.toString(), 
                    getValue(baseUrlName, name, strict, props)), 
                null, 
                null);
        extendPresentable(attr.getPresentation());
    }

    private static IPresentable extendPresentable(IPresentable p, int type) {
        IPresentable pres = extendPresentable(p);
        pres.setType(type);
        return pres;
    }
    private static IPresentable extendPresentable(IPresentable p) {
        p.setEnabler(IActivable.INACTIVE);
        return p;
    }
    private static void addAttribute(Bean<?> bean, String baseUrl, String name, boolean strict) {
        Map<String, Object> props = bean.toValueMap(null);
        props.put("bean", bean.getName());
        String description = readFromDownload(attrFile(baseUrl, name, "description", ".txt"), false, props);
        Object valueObject = getValue(baseUrl, name, strict, props);
        String imageUrl = getImageUrl(baseUrl, name, strict, props);
        Html5Presentable html5Presentable = getPresentable(baseUrl, name, props);
        if (imageUrl != null) {
            html5Presentable = html5Presentable == null ? new Html5Presentable() : html5Presentable;
            html5Presentable.setIcon(imageUrl);
        }
        bean.addAttribute(name, new Value<>(name, valueObject) , description, html5Presentable);
    }
    private static Html5Presentable getPresentable(String baseUrl, String name, Map<String, Object> props) {
        String presJson = readFromDownload(attrFile(baseUrl, name, "presentable", ".json"), false, props);
        presJson = StringUtil.insertProperties(presJson, props);
        return BeanUtil.fromJSON(Html5Presentable.class, presJson);
    }
    private static String getImageUrl(String baseUrlName, String name, boolean strict, Map<String, Object> props) {
        String imageDownload = readFromDownload(attrFile(baseUrlName, name, IMAGE.toString(), ".jpg"), strict, props);
        if (imageDownload == null) {
            imageDownload = readFromDownload(attrFile(baseUrlName, name, IMAGE.toString(), ".url"), strict, props);
        }
        return imageDownload;
    }

    private static Object getValue(String baseUrl, String name, boolean strict, Map<String, Object> props) {
        String value = readFromDownload(attrFile(baseUrl, name, "value", ".obj"), strict, props);
        String valueTypeName = readFromDownload(attrFile(baseUrl, name,"valueType", ".txt"), false, props);
        Object valueObject = null;
        Class<?> valueType = valueTypeName != null ? BeanClass.load(valueTypeName) : null;
        valueObject = value != null && valueType != null && JSon.isJSon(value) ? new JSon().toObject(null, value) : value;
        return valueObject;
    }
    private static String attrFile(String baseUrl, String name, String add, String ext) {
        return baseUrl + (name.equals(ext) ? "" : name + "-") + add + ext;
    }

    private static IAction<?> addAction(Bean<?> bean, String baseUrl, String name, boolean strict, Map<String, Object> props) {
        String actionSpec = readFromDownload(baseUrl + name + ".action", strict, props);
        if (actionSpec == null) {
            return null;
        }

        IPRunnable runnable = null;
        IAction<?> action = null;
        if (actionSpec.matches(ENV.get("uri.scheme.expression", "(file|https?|s?ftp|mailto|tel|imap|irc|nntp|acap|icap|mtqp|wss)[:]//.*"))) {
            runnable = ENV.get(Pool.class).add(WebClient.create(actionSpec, null));
        } else {
            runnable = Util.trY( () -> new Action<>(actionSpec), false);
            if (runnable != null) {
                ENV.get(Pool.class).add(runnable);
            }
        }

        if (runnable == null) {
            action = new CommonAction<>(name, name, name) {
                public Object action() throws Exception {
                    return SystemUtil.execute(actionSpec);
                }
            };
        } else {
            action = new SpecifiedAction<>(runnable.getName(), null);
        }
        bean.addAction(action);
        return action;
    }

    static List<String> readNamesFromUrl(String readmeUrl) {
        return readNamesFromUrl(readmeUrl, TAG_NAMES, true);
    }

    static List<String> readNamesFromUrl(String readmeUrl, boolean mandatory) {
        return readNamesFromUrl(readmeUrl, TAG_NAMES, mandatory);
    }

    static String encloseTag(String name) {
        return OPEN_TAG + name + CLOSE_TAG;
    }
    static List<String> readNamesFromUrl(String readmeUrl, String tag, boolean mandatory) {
        String readmeText = readFromDownload(readmeUrl, mandatory);
        String apps = readmeText != null ? StringUtil.extract(readmeText, encloseTag(tag) + "(.*)") : null;
        if (apps == null && !mandatory)
            return null;
        Objects.requireNonNull(apps, readmeUrl + " has to define names by tag \"" + tag + "\" followed by a comma separated list.");
        return Arrays.asList(trim(apps.split("[,;|]")));
    }

   private static String[] trim(String[] names) {
        for(int i=0; i<names.length; i++) {
            names[i] = names[i].trim();
        }
        return names;
    }
    
   private static String readFromDownload(String url) {
    return readFromDownload(url, true);
   }

   private static String readFromDownload(String url, boolean mandatory, Map<String, Object> properties) {
    String txt = readFromDownload(url, mandatory);
    return txt != null && properties != null ? StringUtil.insertProperties(txt, properties) : txt;
   }

    private static String readFromDownload(String url, boolean mandatory) {
        File file = download(url, mandatory);
        return file != null ? FilePath.read(file) : null;
    }
    private static File download(String url, boolean mandatory) {
        return Util.trY( () -> NetUtil.download(url, getDefinitionDirectory(), false, false), mandatory);
    }

    private static String getDefinitionDirectory() {
        return Pool.getSpecificationRootDir() + "cms/";
    }

}
