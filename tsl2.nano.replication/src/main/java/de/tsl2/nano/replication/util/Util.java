package de.tsl2.nano.replication.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class Util {
	static Map<String, Object> consumedProps = new HashMap<>(); //for system.out prints...as help
	
	public static String getProperty(String name, String defaultOrMandatory, String info) {
		return getProperty(name, String.class, defaultOrMandatory, info);
	}
	public static <T> T getProperty(String name, Class<T> type) {
		return getProperty(name, type, null, null);
	}
	@SuppressWarnings("unchecked")
	public static <T> T getProperty(String name, Class<T> type, T defaultOrMandatory, String info) {
		assert defaultOrMandatory == null || type.isAssignableFrom(defaultOrMandatory.getClass());
		T value = null;
		if (type.equals(String.class))
			value = (T) System.getProperty(name, (String)defaultOrMandatory);
		else if (type.equals(Boolean.class))
			value = type.cast(Boolean.getBoolean(name)); //TODO: involve defaultOrMandatory
		else if (type.equals(Integer.class))
			value = type.cast(Integer.getInteger(name)); //TODO: involve defaultOrMandatory
		assert info == null || value != null : "Please define -D" + name + " " + info;
		if (value != null)
			ULog.log(" " + name + "=" + value, !String.class.equals(type));
		consumedProps.put(name, value);
		return value;
	}

	public static ClassLoader linkResourcePath(String origin, String link) {
		ClassLoader originCL = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(new URLClassLoader(((URLClassLoader)originCL).getURLs()) {
		    @Override
		    public Enumeration<URL> getResources(String name) throws IOException {
		        if (name.equals(origin)) {
		        	ULog.log("..loading " + link + "...", false);
		        	return super.getResources(link);
		        }
		        return super.getResources(name);
		    }
		});
		return originCL;
	}
	
	public static Map<String, Object> getConsumedProperties() {
		return consumedProps;
	}
	
	public static Properties loadPropertiesToSystem(String file) {
		Properties props = new Properties();
		try {
			ULog.log("loading optional properties from " + file + "...", false, new Object[0]);
			if (Files.exists(Paths.get(file))) {
				props.load(Files.newInputStream(Paths.get(file)));
				System.getProperties().putAll(props);
				ULog.log("done");
			} else {
				ULog.log("file does not exist!");
			}
			return props;
		} catch (IOException e) {
			ULog.log(e.toString()); //optional
			return null;
		}
	}
	public static String[] mergeArgsAndProps(String[] args, int minargs, Properties props) {
		if (props != null) {
			ArrayList<Object> al = new ArrayList<>(minargs);
			for (int i = 0; i < minargs; i++) {
				if (args != null && args.length > i)
					al.add(args[i]);
				else
					if (props.contains("args" + i))
						al.add(props.get("args" + i));
			}
			return al.toArray(new String[0]);
		}
		return args == null ? new String[0] : args;
	}
	public static String toString(Throwable e) {
		while (e.getCause() != null)
			e = e.getCause();
		return e.toString() + ": " + (e.getStackTrace().length > 0 ? e.getStackTrace()[0] : "[no stacktrace available]");
	}

	public static Object handleException(Exception e) {
		if (Util.getProperty("on.error.stop", Boolean.class))
			throw new RuntimeException(e);
		else {
			ULog.log(" " + Util.toString(e) + "...");
			return null;
		}
	}
	
	public static void printLogo(String file) {
		String logo;
		try {
			logo = read(Thread.currentThread().getContextClassLoader().getResourceAsStream(file));
		} catch (IOException e) {
			handleException(e);
			return;
		}
		ULog.log(logo);
	}
	
	public static String read(InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
    }

	public static void invoke(String call) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
        int div = call.lastIndexOf('.');
        String clsName = call.substring(0, div);
        String mthName = call.substring(div + 1);
        Thread.currentThread().getContextClassLoader().loadClass(clsName).getMethod(mthName, new Class[0]).invoke(null);
	}

	public static final void assert_(boolean assertion, String message, Object... args) {
		if (!assertion && !Boolean.getBoolean("tsl2.nano.disable.assertion"))
			throw new IllegalArgumentException(String.format(message, args));
	}
	/**
	 * @return 
	 */
	@SuppressWarnings("unchecked")
	public static <P> P decode(Class<P> cls, Object obj) {
		try {
			cls = (Class<P>) MethodType.methodType(cls).wrap().returnType(); // get the wrapper
			return (P) cls.getMethod("decode", new Class[] {String.class}).invoke(null, obj.toString());
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			throw new RuntimeException(e);
		}
	}
}

