package de.tsl2.nano.core.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import de.tsl2.nano.core.log.LogFactory;

/**
 * simple property loader. provides property file and system properties.<p/>
 * use: <pre>
 * private static final MyProperties MY = new MyProperties(MyClassName.class, "myclassname.");
 * 
 * public static final String MYCONSTANT = MY.get("myconstant", "my-default-value");
 * </pre>
 */
public class MainProperties {
    private String propPrefix;

    public MainProperties(Class<?> mainCls) {
        this(mainCls, mainCls.getSimpleName().toLowerCase() + ".");
    }
    public MainProperties(Class<?> mainCls, String propPrefix) {
        LogFactory.log("creating myproperties for: " + mainCls.getSimpleName() + ", prefix: " + propPrefix);
        this.propPrefix = propPrefix.endsWith(".") ? propPrefix : propPrefix + ".";
        loadProperties(mainCls);
    }

    /** loads properties from file and provides all as system properties */
    private void loadProperties(Class<?> mainCls) {
        try {
            Properties p = new Properties();
            String pfile = mainCls.getSimpleName().toLowerCase() + ".properties";
            System.out.print("trying to load properties from '" + pfile + "'...");
            p.load(new FileReader(new File(pfile)));
            p.forEach((k, v) -> System.setProperty((String)k, (String)v) );
            System.out.println("successfull " + p.size() + " properties imported to system-properties!"); 
        } catch (IOException e) {
            System.out.println("not found!");
        }
    }

    @SuppressWarnings("unchecked")
	public <T> T geT(String name, T defValue) {
        if (defValue instanceof String)
			return (T) System.getProperty(propPrefix + name, (String) defValue);
        else {
        	String p = System.getProperty(propPrefix + name);
        	return p != null ? (T) ObjectUtil.wrap(p, defValue.getClass()) : defValue;
        }
    }
}
