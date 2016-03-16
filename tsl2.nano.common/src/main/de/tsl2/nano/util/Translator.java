package de.tsl2.nano.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.logging.Log;

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
    //Bank-Konto-Daten verfügbar sein müssen :-(
//        String googleTranslateUrl =
//            "https://www.googleapis.com/language/translate/v2?key=INSERT-YOUR-KEY&q=hello%20world&source=en&target=de";

    public static final String translate(Locale srcLang, Locale destLang, String words) {
        words = StringUtil.spaceCamelCase(words);
        words = words.replaceAll("[^\\w]+", "\\+");
        String request = MessageFormat.format(URL_TANSLATION_TEMPL, srcLang, destLang, words);
        return NetUtil.get(request).substring(2).trim();
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
    public static Properties translateProperties(String name, Properties origin, Locale srcLang, Locale destLang/*, int requestWordCount*/) {
        LOG.info("starting translation of " + origin.size() + " words from <" + srcLang + "> to <" + destLang + ">");
        Set<Object> keySet = origin.keySet();
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
    public static Properties translateProperties0(String name, Properties origin, Locale srcLang, Locale destLang/*, int requestWordCount*/) {
        String words = StringUtil.toString(origin.values(), -1);
        String trans = translate(srcLang, destLang, words);
        //create the target properties
        String[] t = trans.split("\\s+");
        Properties target = new Properties();
        int i = 0;
        for (Object k : origin.keySet()) {
            target.put(k, t[i++]);
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
    public static Properties translateBundle(String name, Set<String> keySet, Locale srcLang, Locale destLang) {
        Properties p = new Properties();
        for (String k : keySet) {
            p.put(k, Messages.getString(k));
        }
        p = translateProperties(name, p, srcLang, destLang);
        FileUtil.saveProperties(Messages.getResourceFileName(name), p);
        Messages.reload();
        return p;
    }
}
