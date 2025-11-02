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

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.def.BeanValueMap;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.Util;

/**
 * provides nano.h5 application as backend through jax-rs (example implementation, base to generate an open-api description).<p/>
 * It is only an example implementation, should be an extra module!
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@Path("/")
public class NanoBackendJaxrs {
    @GET
    @Path("backend")
    @Produces(MediaType.APPLICATION_JSON)
    public BeanValueMap backend(@QueryParam("user") String user, @QueryParam("password") String password, Map<String, Object> options) {
        // TODO: create new NanoH5 session , check authrization, set all thread values like BeanContainer
        // Bean<?> bean = new BeanValueMap("user", user);
        String query = ENV.get("app.external.backend.nanoh5.query",
            "select * from party where party.shortname='MM'"
        );
        Collection<Object> result = query(query, user);
        if (Util.isEmpty(result))
            throw new IllegalArgumentException("no data found for: " + user);
        // should we return a collection? No, the single bean may have attributes containing collections!
        Tuple first = (Tuple) result.iterator().next();
        return BeanValueMap.from(toMap(first));
    }

    @SuppressWarnings("rawtypes")
    protected Map<String, String> toMap(Tuple first) {
        return first.getElements().stream().collect(Collectors.toMap( 
            e -> ((TupleElement)e).getAlias(),
            e -> ((TupleElement)e).toString())
            );
    }

    protected Collection<Object> query(String query, Object...args) {
        return BeanContainer.instance().getBeansByQuery(query, true, args);
    }
}
