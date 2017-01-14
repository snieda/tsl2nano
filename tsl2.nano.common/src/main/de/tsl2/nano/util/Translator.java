package de.tsl2.nano.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.logging.Log;

import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;

/**
 * machine translator using a REST service from WorldLingo.
 * 
 * @author Tom
 */
public class Translator {
    private static final Log LOG = LogFactory.getLog(Translator.class);
    /** WorldLingo URL */
    private static final String URL_TANSLATION_TEMPL =
        "http://www.worldlingo.com/S000.1/api?wl_srclang={0}&wl_trglang={1}&wl_password=secret&wl_mimetype=text%2Fplain&wl_data={2}";

    //Der KEY ist mit einem google-account + projekt verbunden. Fuer dieses Projekt muss 'abrechenbar' aktiviert sein, so dass
    //Bank-Konto-Daten verf�gbar sein m�ssen :-(
//        String googleTranslateUrl =
//            "https://www.googleapis.com/language/translate/v2?key=INSERT-YOUR-KEY&q=hello%20world&source=en&target=de";

    public static final String translate(Locale srcLang, Locale destLang, String words) {
        words = StringUtil.spaceCamelCase(words);
        words = URLEncoder.encode(words);
        String request = MessageFormat.format(URL_TANSLATION_TEMPL, srcLang, destLang, words);
        try {
            return URLDecoder.decode(NetUtil.get(request).substring(2).trim(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * translates all property values from srcLang to destLang. splits camel case expressions.
     * 
     * @param name bundle name
     * @param origin properties to translate
     * @param srcLang source language
     * @param destLang destination language
     * @return translated properties
     */
    @SuppressWarnings("rawtypes")
    public static Properties translateProperties(String name,
            Map origin,
            Locale srcLang,
            Locale destLang/*, int requestWordCount*/) {
        LOG.info("starting translation of " + origin.size() + " words from <" + srcLang + "> to <" + destLang + ">");
        Set keySet = origin.keySet();
        Properties target = new Properties();
        int tries = 0;
        String word = null;
        for (Object k : keySet) {
            try {
                word = origin.get(k).toString();
                if (word.matches("[\\w\\s]+")) //translate only 'real' words!
                    target.put(k, translate(srcLang, destLang, word));
            } catch (Exception e) {
                if (tries++ < 20) {
                    LOG.info("...retrying translation on word " + target.size() + " / " + origin.size());
                    ConcurrentUtil.sleep(3000);
                    try {
                        target.put(k, translate(srcLang, destLang, word));
                        continue;
                    } catch (Exception e1) {
                        //Ok, log only the first error and stop
                    }
                }
                LOG.error("stopping translation on word '" + origin.get(k) + "' in cause of error: " + e.toString());
                LOG.info("only " + target.size() + " of " + origin.size() + " were translated!");
                break;
            }
        }
        return target;
    }

    /**
     * tries to translate all properties on one request. if values have more than one word (myEntry=one two), the
     * mapping will fail
     * 
     * @param name bundle name
     * @param origin properties to translate
     * @param srcLang source language
     * @param destLang destination language
     * @return translated properties
     */
    @SuppressWarnings("rawtypes")
    public static Map translatePropertiesFast(String name,
            Map origin,
            Locale srcLang,
            Locale destLang/*, int requestWordCount*/) {
        String words = StringUtil.toString(origin.values(), -1);//.replaceAll("[,\\s]+", " ");
        String trans = translate(srcLang, destLang, words);
        //create the target properties
        String[] t = trans.split("[,\\s]+");
        Properties target = new Properties();
        int i = 0;
        String tos[];
        StringBuilder tt = new StringBuilder();
        String key;
        for (Object k : origin.keySet()) {
            key = (String) origin.get(k);
            tos = /*CollectionUtil.concat(*/StringUtil.splitCamelCase(key)/*, StringUtil.splitWordBinding(key))*/;
            tt.setLength(0);
            //concat camelcase words...works only on same length
            for (int j = 0; j < tos.length; j++) {
                if (i+1 < t.length)
                    tt.append(t[i++] + " ");
            }
//            System.out.println(k + " ("+ origin.get(k) + ") --> " + tt);
            target.put(k, tt.length() > 0 ? tt.toString().trim() : origin.get(k));
        }
        return target;
    }

    /**
     * translates all property values from srcLang to destLang. Uses the {@link ResourceBundle} wrapper {@link Messages}
     * to get the values for the given keys. splits camel case expressions. writes the bundle to a file and reloads the
     * resource bundles.
     * 
     * @param name bundle name
     * @param keySet key-set of a resource bundle.
     * @param srcLang source language
     * @param destLang destination language
     * @return translated properties
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Map translateBundle(String name, Set<String> keySet, Locale srcLang, Locale destLang) {
        final Map map = new TreeMap(); //alphabetic sorted entries
        Map p = new LinkedHashMap();
        Map komplex = new LinkedHashMap();
        String txt;
        for (String k : keySet) {
            txt = Messages.getString(k);
            if (txt.indexOf(' ') == -1 && txt.indexOf('.') == -1) {
                p.put(k, txt);
                if (p.size() >= 100) {//request may exceed max length (-->Http 413)
                    map.putAll(translatePropertiesFast(name, p, srcLang, destLang));
                    p.clear();
                }
            } else
                komplex.put(k, txt);
            //TODO: key/values are shifted in more komplex bundles. the framework bundles have only en/de!
            if (!ENV.get("app.translate.bundle.framework", false))
                break;
        }
        map.putAll(translatePropertiesFast(name, p, srcLang, destLang));
        //TODO: is the service now able to translate sentences?
        map.putAll(translateProperties(name, komplex, srcLang, destLang));

        Properties props = new Properties() {
            @Override
            public synchronized Enumeration<Object> keys() {
                return Collections.enumeration(keySet());
            }
            @Override
            public Set<Object> keySet() {
                return map.keySet();//alphabetic order
            }
        };
        props.putAll(map);
        FileUtil.saveProperties(Messages.getResourceFileName(name), props);
        Messages.reload();
        return p;
    }
}
