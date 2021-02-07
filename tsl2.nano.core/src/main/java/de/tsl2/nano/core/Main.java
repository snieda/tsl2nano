/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 27.09.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.core;

import java.util.Map;

import de.tsl2.nano.core.messaging.EventController;
import de.tsl2.nano.core.util.StringUtil;


/**
 * base class for application main. provides a generic main-method to instantiate an extension of {@link Main}. please
 * overwrite the main method.<br/>
 * Example:
 * 
 * <pre>
 * public static void main(String[] args) {
 *     startApplication(NanoH5.class, MapUtil.asMap(0, &quot;http.port&quot;), args);
 * }
 * </pre>
 * 
 * TODO: use {@link Argumentator} instead of {@link #setEnvironmentArguments(String[], Map)}.
 * 
 * @author Tom
 * @version $Revision$
 */
public class Main {

	public static final int DEFAULT_PORT = 8067;
	public static final String DEFAULT_URL = "http://localhost:" + DEFAULT_PORT;
	
    /**
     * constructor
     */
    protected Main() {
        initENVService();
    }

	protected void initENVService() {
    	if (ENV.isAvailable())
    		ENV.addService(Main.class, this);
	}
    
    /**
     * main entry
     * 
     * @param args launching args
     */
    public static void main(String[] args) {
        startApplication(Main.class, null, args);
    }

    /**
     * startApplication
     * 
     * @param nanoH5
     * @param args
     */
    public static void startApplication(Class<?> application, Map<Integer, String> argMapping, String... args) {
        try {
            setEnvironmentArguments(argMapping, args);
            //use reflection on construction to handle exception inside this method
            Main main = (Main) application.getConstructor(new Class[0]).newInstance(new Object[0]);
            main.start();
        } catch (Exception e) {
            ManagedException.forward(e);
        }
    }

    /**
     * provides all args mapped by argMapping to the system environment properties.
     * 
     * @param args main arguments, given by {@link #main(String[])}.
     * @param argMapping application specific mapping of args. each name is an argument name for an argument at
     *            key-position.
     */
    protected static void setEnvironmentArguments(Map<Integer, String> argMapping, String... args) {
    	String url = null, port = null;
        if (argMapping != null && argMapping.size() > 0) {
            for (int i = 0; i < args.length; i++) {
                String argName = argMapping.get(i);
                if (argName != null) {
                    ENV.setProperty(argName, args[i]);
                }
            }
        } else {
        	for (int i = 0; i < args.length; i++) {
				if (args[i].matches("https?[:]//.*[:]\\d{3,7}")) {
					url = args[i];
					break;
				}
				else if (args[i].matches("\\d{3,7}")) {
					port = args[i];
					break;
				}
			}
        }
        if (url != null) {
            ENV.setProperty("service.url", url);
        } else {
            if (port == null)
                port = ENV.get("service.port", null);
            if (port != null) {
                url = ENV.get("service.url", DEFAULT_URL);
                ENV.setProperty("service.url", StringUtil.substring(url, null, ":", true) + ":" + port);
            }
        }
    }

    public void start() {
        throw new UnsupportedOperationException();
    }

    public void stop () {
        throw new UnsupportedOperationException();
    }
    
    public void reset() {
    }
    
    public void persist() {
    }
    
    public EventController getEventController() {
        throw new UnsupportedOperationException();
    }
}
