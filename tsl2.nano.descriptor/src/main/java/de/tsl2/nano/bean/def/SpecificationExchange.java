package de.tsl2.nano.bean.def;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;

/**
 * provides a mechanism to read all specifications (rules, queries, actions etc.) and their use in the 
 * bean-presentations from a single property or csv file.<p/>
 * as convenience, each new bean writes all possible attribute properties to be used for a specification 
 * definition to a file inside the applications configuration directory.
 * 
 * @author ts
 */
public class SpecificationExchange {
	private static final Log LOG = LogFactory.getLog(SpecificationExchange.class);

	public static final String EXT_CSV = ".csv";
	protected static final String SEP = ";";
    public static final String FILENAME_SPEC_PROPERTIES = "specification.properties";

	public static final String PATH_LAYOUTCONSTRAINTS = "presentable.layoutConstraints";
	public static final String PATH_COLDEF_LAYOUTCONSTRAINTS = "columnDefinition." + PATH_LAYOUTCONSTRAINTS;

    public enum Change {valueexpression, attributefilter, icon, addattribute, addaction, enabler, listener, rulecover};
    
    /**
     * generates resource entries for each attribute+tooltip and each action to be edited later.
     */
    public void saveSpecificationEntries(BeanDefinition bean, Properties p) {
    	if (p.contains(bean.getId()))
    		return;
//        p.put(bean.getId(), bean.getName());
        Collection<IAttributeDefinition<?>> attributes = bean.getBeanAttributes();
		String keyPrefix = "#" + bean.getId() + ".";
		p.put(keyPrefix + Change.attributefilter, "<attribute names comma or space separated>");
		p.put(keyPrefix + Change.valueexpression, "<map-bean-to-value e.g. {name}-{surname}>");
		p.put(keyPrefix + Change.icon, "<path-to-icon-file>");
		p.put(keyPrefix + Change.addaction + "XXX", "<rule>");
        p.put(keyPrefix + Change.addattribute + "XXX", "<rule>");
        String id;
        for (IAttributeDefinition<?> a : attributes) {
            id = a.getId();
            if (p.getProperty(id) == null) {
            	//TODO: listener (rulecover -> attributecover) are unknown here (-> specification)
                keyPrefix = "#" + id + ".";
				p.put(keyPrefix + Change.enabler, "<rule>");
                p.put(keyPrefix + Change.listener, "<rule>:<comma-separated-list-of-observable-attribute-names>");
                p.put(keyPrefix + Change.rulecover, "<rule>:<path like 'presentable.layoutconstaints'>");
            }
        }
        FileUtil.saveProperties(ENV.getConfigPath() + FILENAME_SPEC_PROPERTIES, p);
        saveAsCSV(ENV.getConfigPath() + FILENAME_SPEC_PROPERTIES + EXT_CSV, p);
    }

    /** converts the given properties to csv (objectname, rule, optional-parameter) */
    public String saveAsCSV(String filename, Properties p) {
    	StringBuilder buf = new StringBuilder();
    	//TODO: performance: don't write whole properties on each bean...
    	for (Map.Entry<Object, Object> entry : p.entrySet()) {
			String key = entry.getKey().toString();
			String val = entry.getValue().toString();
			String first = StringUtil.substring(val, null, ":");
			String second = val.contains(":") ? StringUtil.substring(val, ":", null) : "";
			buf.append(key + SEP + first + SEP + second + "\n");
		}
    	FileUtil.writeBytes(buf.toString().getBytes(), filename, false);
    	return filename;
	}

    /** does the job - imports the specification into the bean presentation layer - must be implmented. */
	public int enrichFromSpecificationProperties() {
    	throw new UnsupportedOperationException();
    }

}
