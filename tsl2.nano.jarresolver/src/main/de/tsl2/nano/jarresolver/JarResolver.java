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
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;

import de.tsl2.nano.collection.MapUtil;
import de.tsl2.nano.core.Environment;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.execution.SystemUtil;

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
    /** version numbers from '-0.0' to '-999.999.999.Description' */
    static final String REGEX_VERSION = "-\\d{1,3}[.]\\d{1,3}[.]\\d{0,3}[.-]?[a-zA-Z]*";

    static final String PRE_PACKAGE = "PACKAGE.";
    static final String PACKAGE_EXCEPTION = "package.exception.regex";
    static final String ONLY_LOAD_DEFINED = "only.load.defined.packages";
    
    /**
     * constructor
     */
    public JarResolver() {
        props = new Properties();
        try {
            props.load(Environment.getResource("jarresolver.properties"));
            String updateUrl = props.getProperty(URL_UPDATE_PROPERTIES);
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

    /**
     * does the whole work
     * 
     * @param deps dependency or package names
     * @return information string about resolved dependencies
     */
    public String start(String... deps) {
        if (deps != null && deps.length > 0) {
            prepareDependencies(deps);
        }
        loadMvn();
        createMvnScript();
        loadDependencies();

        return props.getProperty(JAR_DEPENDENCIES);
    }

    private void loadDependencies() {
        System.setProperty("M2_HOME", mvnRoot);
        SystemUtil.execute(new File(basedir), mvnRoot + "/bin/mvn.bat", "install");
    }

    private void createMvnScript() {
        LOG.info("creating mavens pom.xml");
        InputStream stream = Environment.getResource(TMP_POM);
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
        InputStream stream = Environment.getResource(TMP_DEPENDENCY);
        String dependency = String.valueOf(FileUtil.getFileData(stream, "UTF-8"));

        StringBuilder buf = new StringBuilder(deps.length * (dependency.length() + 20));
        Map p;
        String groupId, artifactId, version;
        for (int i = 0; i < deps.length; i++) {
            LOG.info("creating dependency '" + deps[i] + "'");
            version = StringUtil.extract(deps[i], REGEX_VERSION);
            //version should not be an empty string
            if (Util.isEmpty(version))
                version = null;
            if (deps[i].contains("/")) {
                groupId = StringUtil.substring(deps[i], null, "/");
                artifactId = StringUtil.substring(deps[i], "/", version);
            } else {
                artifactId = StringUtil.substring(deps[i], null, version);
                groupId = findPackage(deps[i], true);
                //if no definition was found, try it with the artifactID itself
                if (groupId == null)
                    groupId = artifactId;
            }
            version = !Util.isEmpty(version) ? version.substring(1) : "RELEASE";
            p = MapUtil.asMap(KEY_GROUPID, groupId, KEY_ARTIFACTID, artifactId, KEY_VERSION, version);
            buf.append(StringUtil.insertProperties(dependency, p));
        }
        return buf.toString();
    }

    private String[] getDependencies() {
        String depStr = (String) props.get(JAR_DEPENDENCIES);
        if (Util.isEmpty(depStr))
            throw new IllegalArgumentException("no dependencies defined --> nothing to do!");
        return depStr.split(",\\s*");
    }

    private void loadMvn() {
        File mvnFile = download((String) props.get(URL_MVN_DOWNLOAD), false, false);
        mvnRoot = mvnFile.getParent();
        String extractedName = mvnRoot + "/" + StringUtil.substring(mvnFile.getName(), null, "-bin.zip");
        if (!new File(extractedName + "/bin").exists())
            FileUtil.extract(mvnFile.getPath(), mvnRoot + "/", ".*");
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
        basedir = props.getProperty(DIR_LOCALREPOSITORY);
        basedir = !Util.isEmpty(basedir) ? basedir : Environment.getConfigPath();
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
                deps[i] = pck;
                addIt = true;
            } else  {
                addIt = !onlyLoadDefined;
            }
            if (addIt && !deps[i].matches(PACKAGE_EXCEPTION))
                buf.append("," + deps[i]);
        }
        
        if (buf.length() == 0)
            throw new IllegalArgumentException("no dependencies defined --> nothing to do!");

        jars = buf.toString();
        if (jars.startsWith(","))
            jars = jars.substring(1);
        props.setProperty(JAR_DEPENDENCIES, jars);
    }

    private String findPackageByArtifactId(String artifactId) {
        Set<Object> keySet = props.keySet();
        String key;
        for (Object k : keySet) {
            key = (String) k;
            if (key.startsWith(PRE_PACKAGE) && props.getProperty(key).equals(artifactId))
                return StringUtil.substring(key, PRE_PACKAGE, null);
        }
        return null;
    }

    /**
     * recursive finder of package definitions. searches for the given dependency name in the properties starting with
     * {@link #PRE_PACKAGE}. If packageKey is true, the property key will be returned, otherwise the property value (the
     * dependency/artifact name).
     * 
     * @param dependency package path
     * @param packageKey whether to return the key or value
     * @return found dependency entry (key or value)
     */
    private String findPackage(String dependency, boolean packageKey) {
        String pck = props.getProperty(PRE_PACKAGE + dependency);
        if (pck != null)
            return packageKey ? dependency : pck;
        else if (dependency.contains("."))
            //search for parts of given package
            return findPackage(StringUtil.substring(dependency, null, ".", true), packageKey);
        else if (props.values().contains(dependency))
            return findPackageByArtifactId(dependency);
        else
            return null;
    }

    /**
     * main
     * 
     * @param args dependency names
     */
    public static void main(String[] args) {
        new JarResolver().start(args);
    }

}
