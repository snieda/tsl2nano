/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 22.04.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.h5.rest;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * registers all jax-rs services under /web.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@ApplicationPath("/web")
public class RestfulApplication extends Application {
}
