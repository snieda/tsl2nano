/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts, Thomas Schneider
 * created on: 11.09.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.h5;

import java.io.File;
import java.util.jar.Attributes;

import de.tsl2.nano.core.AppLoader;
import de.tsl2.nano.core.classloader.NetworkClassLoader;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.NetUtil;

/**
 * Loader for {@link NanoH5}.
 * 
 * @author ts, Thomas Schneider
 * @version $Revision$
 */
public class Loader extends AppLoader {
    private static final String PREFIX_APP = "Application-";
    private static final String APP_CLASS =  PREFIX_APP + "Class";
    private static final String APP_SOURCE =  PREFIX_APP + "Source";
    static private Attributes attributes;

    @Override
    protected NetworkClassLoader provideClassloader(String environment) {
        //read the manifest before creating the new classloader - perhaps we lose that informations
        
        NetworkClassLoader cl = super.provideClassloader(environment);
        
        //if this app was started through jnlp, we have to download the main jar again
        if (!FileUtil.hasResource("websocket.client.js.template")) {
            //IMPROVE: could be done through system property jnlpx.origFilenameArg
            String appUrl = getAttributes().getValue(APP_SOURCE);
            System.out.println("downloading webstart main jar file from " + appUrl);
            File rootjar = NetUtil.download(appUrl, environment, true, false);
            cl.addFile(rootjar.getPath());
            //check the download against it's stored checksum SHA-1 (SHA-1 because sourceforge uses this algorithm)
            File sha1checkFile = NetUtil.download(appUrl + ".SHA-1", environment, true, false);
            char[] sha1check = FileUtil.getFileData(sha1checkFile.getPath(), "UTF-8");//it's an hex string, the encoding can be ignored
//            try {
                FileUtil.checksum(rootjar.getPath(), "SHA-1", String.valueOf(sha1check));
//            } catch (Exception e) {
//                //Workaround: try next steps, even if the chechsum failed
//                System.out.println(e.toString());
//            }
            //WORKAROUND: extracting all files - for NestingClassLoader not loading any resources
            FileUtil.extract(rootjar.getPath(), environment + "/", null);
            String[] files = rootjar.getParentFile().list();
            for (int i = 0; i < files.length; i++) {
                if (files[i].endsWith(".jar"))
                    System.out.println("adding " + files[i] + " to classpath");
                    cl.addFile(new File(files[i]).getPath());
            }
        }
        
        return cl;
    }

    static protected Attributes getAttributes() {
        if (attributes == null)
            attributes = AppLoader.getManifestAttributes();
        return attributes;
    }
    
    public static void main(String[] args) {
        new Loader().start(getAttributes().getValue(APP_CLASS), args);
    }
}
