package de.tsl2.nano.h5.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import de.tsl2.nano.math.vector.Point;

@Path("event")
public class RestfulService
{
    @GET
    @Path("x/{x}/y/{y}")
    @Produces(MediaType.TEXT_HTML)
    public String testElement(@PathParam("x") String x, @PathParam("y") String y) {
        return x + ", " + y;
    }
}
