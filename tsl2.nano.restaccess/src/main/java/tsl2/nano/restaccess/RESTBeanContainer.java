package tsl2.nano.restaccess;

import java.util.Arrays;
import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.persistence.GenericLocalBeanContainer;

/**
 * http://localhost:8080/beancontainer/findById/Person/3 --> {"id":3,"name":"test","description":"test..."}
 * 
 * @author schneith
 */
@Path("")
@SuppressWarnings({"unchecked", "rawtypes"})
public class RESTBeanContainer {

    /**
     * constructor
     */
    public RESTBeanContainer() {
        // initialize EntityManager and BeanContainer
        GenericLocalBeanContainer.initLocalContainer(Thread.currentThread().getContextClassLoader(), false);
    }
    
    @GET
    @Path("/findById/{type}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public JSONMapper getById(@PathParam("type") String type, @PathParam("id") String id) {
        Class<?> eType = BeanClass.createInstance(type);
        return new JSONMapper(BeanContainer.instance().getByID(eType, id));
    }

    @GET
    @Path("/findAll/{type}/{start}/{count}")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Person> getAll(@PathParam("type") String type, @PathParam("start") int start, @PathParam("count") int count) {
        
        return Arrays.asList(new Person(0l));
    }

    @GET
    @Path("/findByExample")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<JSONMapper> getByExample(@QueryParam("from") JSONMapper from, @QueryParam("to") JSONMapper to, @QueryParam("start") int start, @QueryParam("count") int count) {
        
        return Arrays.asList(from, to);
    }
    
    @POST
    @Path("/save")
    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
    public Response save(@QueryParam("bean") Person p) {

        return Response.status(200).entity("Ok").build();
    }
    
    @PUT
    @Path("/delete")
    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@QueryParam("bean") Person p) {
        
        return Response.status(200).entity("Ok").build();
    }
    
    @OPTIONS
    @Path("")
    public Response help() {
        return Response.status(200).entity(RestDescriptor.describe(this)).build();
    }
}
