package de.tsl2.nano.specification;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.bean.def.IBeanDefinitionSaver;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;

/**
 * provides a mechanism to read all specifications (rules, queries, actions etc.) and their use in the 
 * bean-presentations or workflows from a single property, csv or markdown file.<p/>
 * as convenience, each new bean writes all possible attribute properties to be used for a specification 
 * definition to a file inside the applications configuration directory.
 * 
 * @author ts
 */
public class SpecificationExchange implements IBeanDefinitionSaver {
	private static final String SPECIFICATION_README_TXT = "specification-readme.txt";

	private static final Log LOG = LogFactory.getLog(SpecificationExchange.class);

	public static final String EXT_CSV = ".csv";
	public static final String EXT_MARKDOWN = ".md.html";
	
	protected static final String SEP = "\t";
    public static final String FILENAME_SPEC_PROPERTIES = "specification.properties";

    public static final String PATH_POSTIFX = "*";
	public static final String PATH_LAYOUTCONSTRAINTS = "presentable.layoutConstraints";
	public static final String PATH_COLDEF_LAYOUTCONSTRAINTS = "columnDefinition." + PATH_LAYOUTCONSTRAINTS;

    public enum Change {valueexpression, attributefilter, icon, addattribute, addaction, enabler, listener, rulecover, 
    	createstatistics, createquery, createcompositor, createcontroller, createsheet};
    
    protected boolean exists;

    static final String doc = "##############################################################################\n"
    		+ "# Tsl2Nano H5 Specification Properties (Thomas Schneider / 2022)\n"
    		+ "# \n"
    		+ "# Syntax:\n"
    		+ "# <create-property>|<create-user><create-rule><bean-change>\n"
    		+ "#\n"
    		+ "# with:\n"
    		+ "#   create-property   : <property-name>=<property-value>\n"
    		+ "#   create-user       : createuser=<user-name>:<password>:<db-user-name>:<db-password>\n"
    		+ "#   create-rule       : <<rule-type-character><rule-simple-name>=<rule-expression>\n"
    		+ "#   bean-change       : <bean-name>[.<bean-attribute>.[<prop-change>|<attr-change>]] | [bean-change-ex]\n"
    		+ "#     with:  \n"
    		+ "#       bean-name     : <simple-bean-class-name>\n"
    		+ "#       bean-attribute: <simple-bean-attribute-name>\n"
    		+ "#       prop-change   : <<presentable>|<columnDefinition>|<constraint>|type|id|unique|temporalType|description|doValidation>*=<new-value>\n"
    		+ "#	    attr-change   :\n"
    		+ "#			  enabler=<rule>\n"
    		+ "#			| listener=<rule>:<list-of-observables>\n"
    		+ "#			| rulecover=<rule>:<attribute-property>\n"
    		+ "#       bean-change-ex:\n"
    		+ "#			  <valueexpression=<{attribute-name}[[any-seperator-characters]{attribute-name}...]>\n"
    		+ "#			| addattribute=<rule-name>\n"
    		+ "#			| addaction=<rule-name>\n"
    		+ "#			| attributefilter=<list-of-attribute-names-of-this-bean>\n"
    		+ "#			| icon=<relative-path-to-image-file>\n"
    		+ "#			| createcompositor=<basetype>,<baseattribute>,<attribte-of-this-bean-as-target><icon-attribte>\n"
    		+ "#			| createcontroller=<basetype>,<baseattribute>,<attribte-of-this-bean-as-target><icon-attribte><attribute-to-be-increased-by-clicks>\n"
    		+ "#			| createquery=<sql-query>\n"
    		+ "#			| createstatistics\n"
    		+ "#			| createsheet=<name>,<rows>,<cols>\n"
    		+ "#\n"
    		+ "#      with:\n"
    		+ "#        rule       : <rule-type-character><rule-simple-name>\n"
    		+ "#        constraint : constraint.<type|format|scale|precision|nullable|length|min|max|defaultValue|allowedValues>\n"
    		+ "#        presentable: presentable.<type|style|label|description|layout|layoutConstraints|visible|searchable|icon|nesting>\n"
    		+ "#        columndef  : columndefinition.<name|format|columnIndex|sortIndex|isSortUpDirection|width|<presentable>|minsearch|maxsearch|standardSummary>\n"
    		+ "#\n"
    		+ "# The character ':' can be replaced by one of ';:,\\s'. The character '=' can be\n"
    		+ "# replaced by a tab character.\n"
    		+ "##############################################################################\n"
    		+ "\n";
    
	public String loadDocumentation() {
        // TODO: load from file, when resource provided inside jar file.
//		ENV.extractResource(SPECIFICATION_README_TXT);
//		return FileUtil.getFileString(SPECIFICATION_README_TXT);
		return doc;
	}

    /**
     * generates resource entries for each attribute+tooltip and each action to be edited later.
     */
    public void saveResourceEntries(BeanDefinition bean) {
    	saveSpecificationEntries(bean, ENV.getSortedProperties(FILENAME_SPEC_PROPERTIES));
    }

    public void saveSpecificationEntries(BeanDefinition bean, Properties p) {
    	if (exists || p.contains(bean.getId()))
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
        FileUtil.saveProperties(ENV.getConfigPath() + FILENAME_SPEC_PROPERTIES, p, loadDocumentation());
        saveAsTSV(ENV.getConfigPath() + FILENAME_SPEC_PROPERTIES + EXT_CSV, p);
    }

    /** converts the given properties to csv (objectname, rule, optional-parameter) */
    public String saveAsTSV(String filename, Properties p) {
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

    public void setExists(boolean exists) {
		this.exists = exists;
	}
    
    /** does the job - imports the specification into the bean presentation layer - must be implmented. */
	public int enrichFromSpecificationProperties() {
    	throw new UnsupportedOperationException();
    }

}
