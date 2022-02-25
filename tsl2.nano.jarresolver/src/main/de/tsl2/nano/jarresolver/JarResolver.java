/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 28.05.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.jarresolver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.AppLoader;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.execution.SystemUtil;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * Resolves all given dependencies defined in jarresolver.properties or through main args. Uses maven - downloading
 * maven, creating a dynamic pom.xml and downloading the dependencies through maven.
 * 
 * <pre>
 * Features:
 * - installs maven by itself (through an internet connection)
 * - transforms known class names (with package) to known jar-dependencies
 * - creates dynamically a pom.xml holding all dependencies
 * - loads all given dependencies through maven to the current path
 * 
 * Example:
 *  JarResolver.main("ant-1.7.0", "org.hsqldb.jdbcDriver");
 *  
 * In the example you see, that you are able to mix jar-names and class-names. If you use a class-name, JarResolver will 
 * resolve the default jar-version for you.
 * 
 * For more informations, read comments on 'jarresolver.properties'.
 * 
 * ALGORITHM:
 * 1. class name
 *   a. try to find the package (starting with PACKAGE)
 *   b. if not found, try to find it through a part of a package, cutting the rest (at least two parts must retain e.g. org.company)
 *   c. if not found, use the first two parts as groupId and the third part as artifactId (e.g.: org.company/product)
 * 2. artifact name
 *   a. find the package path through all PACKAGE values
 *   b. if not found, cut last part concatenated through '-' and try to find PACKAGE value again. if found, use that as groupId.
 *   c. if not found, use artifact name as group name
 * 
 * </pre>
 * 
 * @author Tom
 * @version $Revision$
 */
public class JarResolver {
    private static final Log LOG = LogFactory.getLog(JarResolver.class);

    Properties props;
    String mvnRoot;
    String basedir;

    static Properties pckMvnDeps = null;
    /*
     * for a description of these constants, see jarresolver.properties
     */
    static final String URL_UPDATE_PROPERTIES = "default.update.url";

    static final String URL_MVN_DOWNLOAD = "mvn.download.url";
    static final String URL_MVN_REPOSITORY = "mvn.repository.url";
    static final String DIR_LOCALREPOSITORY = "dir.local.repository";
    static final String TMP_POM = "pom.template";

    static final String JAR_DEPENDENCIES = "jar.dependencies";
    static final String TMP_DEPENDENCY = "dependency.template";
    static final String KEY_GROUPID = "groupId";
    static final String KEY_ARTIFACTID = "artifactId";
    static final String KEY_VERSION = "version";
    /** version numbers from '-0.0' to '-999.999.999.999.999.Description' */
    static final String REGEX_VERSION = "-\\d{1,3}([.]\\d{1,3}){1,3}[.]\\d{0,3}[.-]?[a-zA-Z]*";

    static final String PRE_PACKAGE = "PACKAGE.";
    static final String PACKAGE_EXCEPTION = "package.exception.regex";
    static final String ONLY_LOAD_DEFINED = "only.load.defined.packages";

    private static final String KEY_FINDJAR_INDEX = "findjar.index";
    private static final String KEY_FINDJAR_CLASS = "findjar.class";
    private static final String KEY_FINDJAR_TIMEOUT = "findjar.timeout";

    private static final String KEY_JARDOWNLOAD_CLASS = "jardownload.class";
    private static final String KEY_JARDOWNLOAD_TIMEOUT = "jardownload.timeout";

    private static final String KEY_MVNSEARCH_CLASS = "mvnsearch.class";
    private static final String KEY_MVNSEARCH_TIMEOUT = "mvnsearch.timeout";

    public JarResolver() {
        this(null);
    }
    
    /**
     * constructor
     */
    public JarResolver(String baseDir) {
        props = new Properties();
        try {
            props.load(ENV.getResource("jarresolver.properties"));
            String updateUrl = props.getProperty(URL_UPDATE_PROPERTIES);
            setBaseDir(baseDir);
            if (updateUrl != null) {
                try {
                    LOG.info("updating jarresolver.properties through " + updateUrl);
                    download(updateUrl, true, true);
                } catch (Exception ex) {
                    //no problem - perhaps no network connection
                    LOG.warn("couldn't update jarresolver.properties from " + updateUrl);
                }
            }

        } catch (IOException e) {
            ManagedException.forward(e);
        }
    }

    private void setBaseDir(String baseDir) {
        this.basedir = baseDir != null ? baseDir : props.getProperty(DIR_LOCALREPOSITORY, ENV.getConfigPath());
        LOG.info("basedir is: " + this.basedir);
    }

    /**
     * installs given package. see {@link #start(String...)}
     * 
     * @param packages
     * @return see {@link #start(String...)}
     */
    public static String install(String... packages) {
        return new JarResolver().start(packages);
    }

    /**
     * does the whole work
     * 
     * @param deps dependency or package names
     * @return information string about resolved dependencies. on error, string starts with FAILED
     */
    public String start(String... deps) {
        if (deps != null && deps.length > 0) {
            prepareDependencies(deps);
        }
        loadMvn();
        createMvnScript();
        int result = loadDependencies();

        return (result > 0 ? "FAILED: " : "") + props.getProperty(JAR_DEPENDENCIES);
    }

    /**
     * loadDependencies
     * 
     * @return mvn process exit value (0: Ok, >0 otherwise)
     */
    private int loadDependencies() {
        try {
            System.setProperty("M2_HOME", new File(mvnRoot).getCanonicalPath());
        } catch (IOException e) {
            ManagedException.forward(e);
        }
        LOG.info("setting M2_HOME=" + System.getProperty("M2_HOME"));
        String mvnRootRel = FileUtil.replaceToSystemSeparator(FileUtil.getRelativePath(mvnRoot, basedir));
        File baseDirectory = new File(FileUtil.replaceToSystemSeparator(basedir));
        Process process = null;
        if (AppLoader.isWindows()) {
            String script = "\\bin\\mvn.cmd";
            new File(mvnRoot + script).setExecutable(true);
            process = SystemUtil.execute(baseDirectory, "cmd", "/C", mvnRootRel + script, "install");
        } else {
            String script = "/bin/mvn";
            new File(mvnRoot + script).setExecutable(true);
            process = SystemUtil.execute(baseDirectory, mvnRootRel + script, "install");
        }
        if (process.exitValue() != 0) {
            LOG.error("Process returned with: " + process.exitValue());
        }
        return process.exitValue();
    }

    private void createMvnScript() {
        LOG.info("creating mavens pom.xml");
        InputStream stream = ENV.getResource(TMP_POM);
        String pom = String.valueOf(FileUtil.getFileData(stream, "UTF-8"));

        Properties p = new Properties();
        p.putAll(props);
        p.put("dependencies", createDependencyInformation());
        pom = StringUtil.insertProperties(pom, p);
        FileUtil.writeBytes(pom.getBytes(), basedir + "pom.xml", false);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private String createDependencyInformation() {
        String[] deps = getDependencies();
        InputStream stream = ENV.getResource(TMP_DEPENDENCY);
        String dependency = String.valueOf(FileUtil.getFileData(stream, "UTF-8"));

        StringBuilder buf = new StringBuilder(deps.length * (dependency.length() + 20));
        Map p;
        String groupId, artifactId, version;
        for (int i = 0; i < deps.length; i++) {
            LOG.info("creating dependency '" + deps[i] + "'");
            version = StringUtil.extract(deps[i], REGEX_VERSION);
            //version should not be an empty string
            if (Util.isEmpty(version)) {
                version = null;
            }
            if (deps[i].contains("/")) {
                groupId = StringUtil.substring(deps[i], null, "/");
                artifactId = StringUtil.substring(deps[i], "/", version);
            } else {
                artifactId = StringUtil.substring(deps[i], null, version);
                groupId = findPackage(deps[i], true);
                //if no definition was found, try it with the artifactID itself
                if (groupId == null) {
                    //if artifactId is a class-name itself, try to extract the third part of the package (org.company.product....)
                    if (BeanClass.isPublicClassName(artifactId)) {
                        if (groupId == null && Boolean.valueOf(props.getProperty("use.mvn-search.on.unknown", "true"))) {
                            String mvnpath = findMvnSearch(artifactId);
                            if (mvnpath != null) {
                                groupId = StringUtil.substring(mvnpath, null, ":");
                                artifactId = StringUtil.substring(mvnpath, ":", ":");
                            }
                        }
                        if (groupId == null && Boolean.valueOf(props.getProperty("use.jar-download.on.unknown", "true"))) {
                            String mvnpath = findJarDownload(artifactId);
                            if (mvnpath != null) {
                                groupId = StringUtil.substring(mvnpath, null, ":");
                                artifactId = StringUtil.substring(mvnpath, ":", ":");
                            }
                        }
                        if (groupId == null && Boolean.valueOf(props.getProperty("use.findjar.on.unknown", "true"))) {
                            String jarName = findJarOnline(artifactId);
                            if (jarName != null) {
                                artifactId = StringUtil.extract(jarName, "\\w+");
                                if (!Util.isEmpty(artifactId)) {
                                    groupId = artifactId;
                                }
                            }
                        }
                        if (groupId == null) {//try it yourself
                            String cls = artifactId;
                            artifactId = StringUtil.substring(StringUtil.substring(cls, ".", null), ".", ".");
                            groupId = StringUtil.substring(cls, null, "." + artifactId);
                        }
                    } else {
                        groupId = artifactId;
                    }
                }
            }
            version = !Util.isEmpty(version) ? version.substring(1) : "RELEASE";
            p = MapUtil.asMap(KEY_GROUPID, groupId, KEY_ARTIFACTID, artifactId, KEY_VERSION, version);
            buf.append(StringUtil.insertProperties(dependency, p));
        }
        return buf.toString();
    }

    private String[] getDependencies() {
        String depStr = (String) props.get(JAR_DEPENDENCIES);
        if (Util.isEmpty(depStr)) {
            throw new IllegalArgumentException("no dependencies defined --> nothing to do!");
        }
        return depStr.split(",\\s*");
    }

    private void loadMvn() {
        File mvnFile = download((String) props.get(URL_MVN_DOWNLOAD), false, false);
        if (mvnFile.length() == 0)
            Message.send("jarresolver ist not able to work - mvn binary download failed!");
        mvnRoot = mvnFile.getParent();
        String extractedName = mvnRoot + "/" + StringUtil.substring(mvnFile.getName(), null, "-bin.zip");
        if (!new File(extractedName + "/bin").exists()) {
            FileUtil.extract(mvnFile.getPath(), mvnRoot + "/", ".*");
        }
        mvnRoot = extractedName;
    }

    /**
     * downloads the given strUrl if a network connection is available
     * 
     * @param strUrl network url to load
     * @param flat if true, the file of that url will be put directly to the environment directory. otherwise the full
     *            path will be stored to the environment.
     * @param overwrite if true, existing files will be overwritten
     * @return downloaded local file
     */
    protected File download(String strUrl, boolean flat, boolean overwrite) {
        setBaseDir(this.basedir);
        return NetUtil.download(strUrl, basedir, flat, overwrite);
    }

    /**
     * combines dependencies from property file with start arguments and stores it back to the property
     * {@link #JAR_DEPENDENCIES}.
     * <p/>
     * removes all packages matching the package-exception expression (package that should not be loaded through maven).
     * 
     * @param deps start arguments
     */
    private void prepareDependencies(String... deps) {
        String jars = props.getProperty(JAR_DEPENDENCIES);
        Boolean onlyLoadDefined = Boolean.valueOf(props.getProperty(ONLY_LOAD_DEFINED));
        StringBuilder buf = new StringBuilder(jars != null ? jars : "");
        String pck;
        boolean addIt;
        for (int i = 0; i < deps.length; i++) {
            //if the parameter is a known package name, fill all dependent jar-files
            pck = findPackage(deps[i], false);
            if (pck != null) {
                //if groupId only found through another artifact-definition, add this dep-name
                deps[i] = pck.endsWith("/") ? pck + deps[i] : pck;
                addIt = true;
            } else {
                addIt = !onlyLoadDefined;
            }
            if (addIt && !deps[i].matches(PACKAGE_EXCEPTION)) {
                buf.append("," + deps[i]);
            }
        }

        if (buf.length() == 0) {
            throw new IllegalArgumentException("no dependencies defined --> nothing to do!");
        }

        jars = buf.toString();
        if (jars.startsWith(",")) {
            jars = jars.substring(1);
        }
        props.setProperty(JAR_DEPENDENCIES, jars);
    }

    /**
     * convenience for extern calls, creating a new {@link JarResolver} instance and returning result of {@link #findPackage(String)}
     * @param part
     * @return
     */
    public static String getPackage(String part) {
        return new JarResolver().findPackage(part);
    }
    
    /**
     * searches for the given expression part inside all package names and their values. if an entry looks like
     * 'PACKAGE.org.pyhton=jython' and you search for 'python' or 'jython' you will get 'org.pyhton'
     * 
     * @param part package or value part.
     * @return package (group-id) name
     */
    public String findPackage(String part) {
        Set<String> keys = props.stringPropertyNames();
        //first, search in the keys
        for (String k : keys) {
            if (k.startsWith(PRE_PACKAGE) && k.contains(part))
                return StringUtil.substring(k, PRE_PACKAGE, null);
        }
        //second: search in the values
        Collection<Object> values = props.values();
        for (Object v : values) {
            if (v.toString().contains(part)) {
                for (String k : keys) {
                    if (v.equals(props.get(k)))
                        return StringUtil.substring(k, PRE_PACKAGE, null);
                }
            }
        }
        return null;
    }

    private String findPackageByArtifactId(String artifactId) {
        Set<Object> keySet = props.keySet();
        String key;
        for (Object k : keySet) {
            key = (String) k;
            if (key.startsWith(PRE_PACKAGE) && props.getProperty(key).equals(artifactId)) {
                return StringUtil.substring(key, PRE_PACKAGE, null);
            }
        }
        return null;
    }

    /**
     * recursive finder of package definitions. searches for the given dependency name in the properties starting with
     * {@link #PRE_PACKAGE}. If packageKey is true, the property key will be returned, otherwise the property value (the
     * dependency/artifact name).
     * 
     * @param dependency package path
     * @param packageKey whether to return the key or value. on true, return value!
     * @return found dependency entry (key or value)
     */
    private String findPackage(String dependency, boolean packageKey) {
        String pck = props.getProperty(PRE_PACKAGE + dependency);
        if (pck != null) {
            return packageKey ? dependency : pck;
        } else if (dependency.indexOf(".") != dependency.lastIndexOf(".")) {
            String part = StringUtil.substring(dependency, null, ".", true);
            pck = findPackage(part, packageKey);
            /*
             * this is a generic search. perhaps we search for 'org.apache' and get 'org.apache.ant'.
             * but our dependency is org.apache.log4j. so we have to check that again - only if we 
             * are searching on the values (packageKey = true)!
             */
            return pck != null && (!packageKey || dependency.contains(StringUtil.substring(pck, null, "/", true))) ? pck
                : null;
        } else if (dependency.contains("-")) {
            //search for parts of given package
            return findPackage(StringUtil.substring(dependency, null, "-", true), packageKey);
        } else if (props.values().contains(dependency)) {
            return findPackageByArtifactId(dependency);
        } else {
            return (pck = findGroupId(dependency)) != null ? pck + (!packageKey ? "/" : "") : null;
        }
    }

    /**
     * find the group id through another artifact id definition.
     * 
     * @param artifactIdPart part of an artifactid
     * @return group id name or null
     */
    private String findGroupId(String artifactIdPart) {
        Collection<Object> packs = props.values();
        String a, pkg;
        for (Object p : packs) {
            a = (String) p;
            if (a.contains(artifactIdPart)) {
                pkg = findPackageByArtifactId(a);
                if (pkg != null) {
                    if (a.contains("/")) {
                        return StringUtil.substring(a, null, "/");
                    } else {
                        return StringUtil.substring(pkg, PRE_PACKAGE, null);
                    }
                }
            }
        }
        return null;
    }

    /**
     * tries to find the given package name through www.findjars.com.
     * 
     * @param pck package name
     * @return jar-file name or null
     */
    public String findJarOnline(String pck) {
        String urlFindjarClass = props.getProperty(KEY_FINDJAR_CLASS, "https://findjar.com/class/");
        int timeout = Util.trY(() -> Integer.valueOf(props.getProperty(KEY_FINDJAR_TIMEOUT, "20000")));
        String content = NetUtil.get(urlFindjarClass + pck.replace(".", "/"), timeout);
        //try to use that jar file where the a part of the package name could be found
        String jarName = null;
        final String ID_NAMEPART = "$MYNAMEPARTEXPRESSION$";
        final String JAR_REGEX = "[a-zA-Z0-9_.-]*" + ID_NAMEPART + "[a-zA-Z0-9_.-]*" + "\\.jar";
        String[] pckParts = pck.split("\\.");
        int i = pckParts.length;
        while (i > 0 && Util.isEmpty(jarName)) {
            jarName = StringUtil.extract(content, JAR_REGEX.replace(ID_NAMEPART, pckParts[--i]));
        }
        //OK, then take anyone
        if (Util.isEmpty(jarName)) {
            final String JAR_REGEX0 = "[a-zA-Z0-9_.-]+" + "\\.jar";
            jarName = StringUtil.extract(content, JAR_REGEX0);
        }
        LOG.info("findjar.com found '" + jarName + "' for class " + pck);
        return Util.isEmpty(jarName) ? null : jarName;
    }

    /**
     * tries to find the given package name through search.maven.org
     */
    public String findMvnSearch(String pck) {
        String urlFindjarClass = props.getProperty(KEY_MVNSEARCH_CLASS, "https://search.maven.org/solrsearch/select?rows=100&wt=xml&q=fc:");
        int timeout = Util.trY(() -> Integer.valueOf(props.getProperty(KEY_MVNSEARCH_TIMEOUT, "10000")));
        String content = NetUtil.get(urlFindjarClass + pck, timeout);
        //try to use that jar file where the a part of the package name could be found
        int i = 0;
        Map<String, String> mvndeps = new HashMap<>();
        while ((i = putNextMvnSearchMvnPath(content, i, mvndeps)) != -1)
        	;
        String mvnpath = getBestMatch(pck, mvndeps);
        LOG.info("search.mvn.org found '" + mvnpath + "' for class " + pck);
        return Util.isEmpty(mvnpath) ? null : mvnpath;
    }
    /**
     * tries to find the given package name through jar-downloads.com
     */
    public String findJarDownload(String pck) {
        String urlFindjarClass = props.getProperty(KEY_JARDOWNLOAD_CLASS, "https://jar-download.com/maven-repository-class-search.php?search_box=");
        int timeout = Util.trY(() -> Integer.valueOf(props.getProperty(KEY_JARDOWNLOAD_TIMEOUT, "10000")));
        String content = NetUtil.get(urlFindjarClass + pck, timeout);
        //try to use that jar file where the a part of the package name could be found
        int i = 0;
        Map<String, String> mvndeps = new HashMap<>();
        while ((i = putNextJarDownloadMvnPath(content, i, mvndeps)) != -1)
        	;
        String mvnpath = getBestMatch(pck, mvndeps);
        LOG.info("jar-download.com found '" + mvnpath + "' for class " + pck);
        return Util.isEmpty(mvnpath) ? null : mvnpath;
    }
    
	private String getBestMatch(String pck, Map<String, String> mvndeps) {
		Properties p = getMvnPaths();
		String match = null;
		int i = 0;
		String pckKey = "PACKAGE." + StringUtil.substring(pck, null, ".", true);
		for (Map.Entry<String, String> e : mvndeps.entrySet()) {
			String key = e.getKey();
			String val = e.getValue();
			if (pck.replace('_', '-').contains(key)) {
				if (match == null || key.length() > match.length())
					match = key;
			}
			p.put(pckKey + "." + ++i, val);
		}
		FileUtil.saveProperties(getMvnPathsFile(), p);
		return match != null ? mvndeps.get(match) : mvndeps.size() > 0 ? mvndeps.values().iterator().next() : null;
	}

	private int putNextMvnSearchMvnPath(String content, int i, Map<String, String> mvndeps) {
		String mvnpath = StringUtil.substring(content, "<str name=\"id\">", "<", i, true);
		if (mvnpath == null)
			return -1;
        mvndeps.put(StringUtil.substring(mvnpath, null, ":"), mvnpath);
		return content.indexOf(mvnpath, i) + mvnpath.length();
	}

	private int putNextJarDownloadMvnPath(String content, int i, Map<String, String> mvndeps) {
		String groupId = getByTag(content, "groupId", i);
		if (groupId == null)
			return -1;
        String artifactId = getByTag(content, "artifactId", i);
        String version = getByTag(content, "version", i);
        String mvnpath = groupId + ":" + artifactId + ":" + version;
        mvndeps.put(groupId, mvnpath);
		return content.indexOf(version, i) + version.length();
	}

	private String getByTag(String content, String tagName, int i) {
		return StringUtil.substring(content, "&lt;" + tagName + "&gt;", "&lt;", i, true);
	}

	private Properties getMvnPaths() {
		if (pckMvnDeps == null) {
			pckMvnDeps = FileUtil.loadPropertiesFromFile(getMvnPathsFile());
			if (pckMvnDeps == null)
				pckMvnDeps = new Properties();
		}
		return pckMvnDeps;
	}

	private String getMvnPathsFile() {
		return ENV.getConfigPath() + "mvn-search-findings-info.properties";
	}
    /**
     * UNUSED YET! ...is there any usecase?...
     * <p/>
     * tries to find a class for the given package name through www.findjars.com.
     * 
     * @param pck package name
     * @return full class name or null
     */
    public String findClassOnline(String pck) {
        String urlFindjar = props.getProperty(KEY_FINDJAR_INDEX, "https://findjar.com/index.x?query=");
        String content = NetUtil.get(urlFindjar + toURIParameter(pck));
        String jarName = StringUtil.extract(content, "[a-zA-Z0-9_.-]+\\.jar");
        return Util.isEmpty(jarName) ? null : jarName;
    }

    private String toURIParameter(String pck) {
        String[] pckParts = pck.split("\\.");
        String PAR_ALIAS = "%2F";
        StringBuilder buf = new StringBuilder(pck.length() + pckParts.length * PAR_ALIAS.length());
        for (String s : pckParts) {
            buf.append(s + PAR_ALIAS);
        }
        return buf.substring(0, buf.length() - PAR_ALIAS.length());
    }

    /**
     * main
     * 
     * @param args dependency names
     */
    public static void main(String[] args) {
        if (Util.isEmpty(args)) {
            System.out.println(
                "+-----------------------------------------------------------------+\n"
                    + "| jar resolver copyright Thomas Schneider / 2012-2016             |\n"
                    + "| finds and downloads jar libraries for given java class packages.|\n"
                    + "| jpr: java -jar de.tsl2.nano.jarresolver.JarResolver args[]      |\n"
                    + "+-----------------------------------------------------------------+\n"
                    + "| syntax    : jpr class-package [class-package [...]]             |\n"
                    + "| example-1 : jpr org.hsqldb                                      |\n"
                    + "| example-2 : jpr ant-1.7.0 org.hsqldb                            |\n"
                    + "| example-2 : jpr org.hsqldb.jdbc.JDBCDriver                      |\n"
                    + "+-----------------------------------------------------------------+\n");
            return;
        }
        try {
            new JarResolver().start(args);
        } catch (Exception e) {
            LOG.error(e);
        }
    }

}
