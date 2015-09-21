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
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;

/**
 * Loader for {@link NanoH5}.
 * 
 * @author ts, Thomas Schneider
 * @version $Revision$
 */
public class Loader extends AppLoader {
    @Override
    protected NetworkClassLoader provideClassloader(String environment) {
        //read the manifest before creating the new classloader - perhaps we lose that informations
        Attributes attributes = NetworkClassLoader.readManifest(Thread.currentThread().getContextClassLoader());
        
        NetworkClassLoader cl = super.provideClassloader(environment);
        
        //if this app was started through jnlp, we have to download the main jar again
        if (!FileUtil.hasResource("websocket.client.js.template")) {
            //IMPROVE: could be done through system property jnlpx.origFilenameArg
            String appUrl = attributes.getValue("Application-Source");
            System.out.println("downloading webstart main jar file from " + appUrl);
            File rootjar = NetUtil.download(appUrl, environment, true, false);
            cl.addFile(rootjar.getPath());
            //WORKAROUND: extracting all files - for NestingClassLoader not loading any resources
            FileUtil.extract(rootjar.getPath(), environment + "/", null);
            String[] files = rootjar.getParentFile().list();
            for (int i = 0; i < files.length; i++) {
                if (files[i].endsWith(".jar"))
                    cl.addFile(new File(files[i]).getName());
            }
        }
        
        return cl;
    }

    public static void main(String[] args) {
        new Loader().start("de.tsl2.nano.h5.NanoH5", args);
    }
}
