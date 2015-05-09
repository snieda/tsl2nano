/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 24.04.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.h5.rest;

import java.io.File;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.h5.Loader;

/**
 * If nano.h5 is deployed into a web-container, this RESTful service starts nano.h5 through the request
 * 'host:8080/tsl2.nano.h5/start/config/8686' or 'host:8080/tsl2.nano.h5/start/user.home/free.port'. see
 * {@link #start(String, int)}
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@Path("start")
public class NanoJAXRS {
    /**
     * starts the nano.h5 application through jax-rs.
     * 
     * @param config nano.h5 environment directory. if config is found inside the system properties, that value will be
     *            used
     * @param port nano.h5 httpd server port. if port is found inside the system properties, that value will be used. if
     *            port equals 'free.port', the next free port will be used.
     */
    @GET
    @Path("{config}/{port}")
    @Produces(MediaType.TEXT_HTML)
    public String start(@PathParam("config") String config, @PathParam("port") String port) {
        try {
            if (config.equals("?") || config.equals("help")) {
                String help =
                    "arg1: nano.h5 environment directory. may be a system property like user.home.<br/>arg2: nano.h5 server port. may be a system property or 'free.port'.";
                return help;
            }
            if (System.getProperty(config) != null)
                config = System.getProperty(config) + "/.nano.h5";
            if (System.getProperty(port) != null)
                port = System.getProperty(port);
            else if (port.equals("free.port"))
                port = String.valueOf(NetUtil.getFreePort());

            Loader.main(new String[] { config, port });
            String nanoUrl = "http://" + NetUtil.getMyIP() + ":" + port; //ENV.get("service.url", "...not started yet...");
            return "<h1 align=\"center\"><a href=\"" + nanoUrl
                + "\"><b>nano.h5</b> started!<br/><font size=\"-1\">directory: <i>" + new File(config).getPath()
                + "</i><br/>port: <i>" + port + "</i></font></a></h1>";
        } catch (Exception e) {
            ManagedException.forward(e);
            return "couldn't start nano.h5: \n" + e;
        }
    }
}
